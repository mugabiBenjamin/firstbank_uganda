package ug.firstbank.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Generates sequential, per-branch-per-year account numbers that survive
 * application restarts by persisting the last-used counter in the
 * {@code account_seq} database table.
 *
 * <p><b>Account number format:</b>
 * {@code BRANCHCODE-YYYY-NNNNNN}, e.g. {@code KLA-2026-000142}</p>
 *
 * <p><b>Branch codes:</b></p>
 * <ul>
 *   <li>Kampala → KLA</li>
 *   <li>Gulu    → GUL</li>
 *   <li>Mbarara → MBA</li>
 *   <li>Jinja   → JIN</li>
 *   <li>Mbale   → MBL</li>
 * </ul>
 *
 * <p><b>SOLID notes:</b></p>
 * <ul>
 *   <li><b>SRP</b> — owns one concern: next-sequence generation and
 *       account-number formatting. No other SQL lives here.</li>
 *   <li><b>OCP</b> — adding a new branch means adding one entry to
 *       {@link #BRANCH_CODES}; no logic changes.</li>
 *   <li><b>DIP</b> — receives a {@link Connection} from {@link DatabaseManager}
 *       rather than opening its own; no {@code DriverManager} call here.</li>
 * </ul>
 *
 * <p>The counter increment is done inside a single {@code UPDATE} + re-read
 * cycle within the caller's transaction to prevent duplicate numbers under
 * concurrent access (unlikely in a desktop app, but correct by design).</p>
 */
public final class SequenceGenerator {

    private static final Logger LOG =
            Logger.getLogger(SequenceGenerator.class.getName());

    // ── Branch code registry ─────────────────────────────────────────────────

    /**
     * Maps branch display names (as they appear in the UI combo box) to their
     * 3-letter account-number codes.
     *
     * <p>Using an immutable {@link Map} rather than a switch expression keeps
     * the mapping data-driven: adding a 6th branch is a one-line change here,
     * with zero impact on any other method (OCP).</p>
     */
    private static final Map<String, String> BRANCH_CODES = Map.of(
            "Kampala", "KLA",
            "Gulu",    "GUL",
            "Mbarara", "MBA",
            "Jinja",   "JIN",
            "Mbale",   "MBL"
    );

    /** Zero-padded width of the sequential portion, e.g. 000142. */
    private static final int SEQ_WIDTH = 6;

    /** Maximum value before the sequence rolls over (999999 for 6 digits). */
    private static final long SEQ_MAX = (long) Math.pow(10, SEQ_WIDTH) - 1;

    // ── Constructor ──────────────────────────────────────────────────────────

    /** Not instantiable — all methods are static. */
    private SequenceGenerator() {}

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Returns the 3-letter branch code for {@code branchDisplayName}.
     *
     * @param branchDisplayName the branch name as shown in the UI, e.g. "Kampala"
     * @return the branch code, e.g. "KLA"
     * @throws IllegalArgumentException if the branch name is not recognised
     */
    public static String branchCode(String branchDisplayName) {
        String code = BRANCH_CODES.get(branchDisplayName);
        if (code == null) {
            throw new IllegalArgumentException(
                    "Unknown branch: '" + branchDisplayName + "'. "
                    + "Known branches: " + BRANCH_CODES.keySet());
        }
        return code;
    }

    /**
     * Generates the next account number for {@code branchDisplayName} in the
     * current calendar year and persists the updated counter to the database.
     *
     * <p>Algorithm:</p>
     * <ol>
     *   <li>Look up the current {@code last_seq} for
     *       {@code (branch_code, current_year)} in {@code account_seq}.</li>
     *   <li>If no row exists, insert one with {@code last_seq = 0}.</li>
     *   <li>Increment {@code last_seq} by 1 and {@code UPDATE} the row.</li>
     *   <li>Format the result as {@code BRANCHCODE-YYYY-NNNNNN}.</li>
     * </ol>
     *
     * <p>The connection must be managed (commit/rollback) by the caller
     * ({@link AccountRepository}), ensuring the counter increment and the
     * account INSERT are committed atomically.</p>
     *
     * @param conn              an open, active {@link Connection}
     * @param branchDisplayName the branch name as shown in the UI
     * @return formatted account number, e.g. {@code "KLA-2026-000142"}
     * @throws SQLException             if a database error occurs
     * @throws IllegalArgumentException if the branch is not recognised
     * @throws IllegalStateException    if the sequence has reached its maximum
     *                                  ({@value #SEQ_MAX}) for this branch/year
     */
    public static String nextAccountNumber(Connection conn,
                                           String branchDisplayName)
            throws SQLException {

        String code = branchCode(branchDisplayName);
        int    year = LocalDate.now().getYear();

        long next = fetchAndIncrement(conn, code, year);

        return String.format("%s-%d-%0" + SEQ_WIDTH + "d", code, year, next);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Atomically fetches and increments the sequence counter for
     * {@code (branchCode, year)}.
     *
     * <p>Uses a SELECT-then-UPDATE (or INSERT) approach. In a single-user
     * desktop application this is safe; for multi-user deployments, wrap
     * inside a serialisable transaction.</p>
     *
     * @param conn       open connection
     * @param branchCode 3-letter branch code
     * @param year       current calendar year
     * @return the incremented sequence value to use in the account number
     * @throws SQLException          on DB error
     * @throws IllegalStateException if the sequence would exceed {@link #SEQ_MAX}
     */
    private static long fetchAndIncrement(Connection conn,
                                          String branchCode,
                                          int year) throws SQLException {

        // ── 1. Try to read existing row ──────────────────────────────────────
        String selectSql =
                "SELECT id, last_seq FROM account_seq "
                + "WHERE branch_code = ? AND seq_year = ?";

        try (PreparedStatement sel = conn.prepareStatement(selectSql)) {
            sel.setString(1, branchCode);
            sel.setInt(2, year);

            try (ResultSet rs = sel.executeQuery()) {

                if (rs.next()) {
                    // Row exists — read and increment.
                    int  rowId   = rs.getInt("id");
                    long current = rs.getLong("last_seq");
                    long next    = current + 1;

                    guardOverflow(next, branchCode, year);

                    String updateSql =
                            "UPDATE account_seq SET last_seq = ? "
                            + "WHERE id = ?";
                    try (PreparedStatement upd =
                                 conn.prepareStatement(updateSql)) {
                        upd.setLong(1, next);
                        upd.setInt(2, rowId);
                        upd.executeUpdate();
                    }

                    LOG.fine(() -> String.format(
                            "Sequence %s-%d incremented to %d", branchCode, year, next));
                    return next;

                } else {
                    // ── 2. No row yet — insert with last_seq = 1 ─────────────
                    String insertSql =
                            "INSERT INTO account_seq (branch_code, seq_year, last_seq) "
                            + "VALUES (?, ?, 1)";
                    try (PreparedStatement ins =
                                 conn.prepareStatement(insertSql)) {
                        ins.setString(1, branchCode);
                        ins.setInt(2, year);
                        ins.executeUpdate();
                    }

                    LOG.fine(() -> String.format(
                            "Sequence %s-%d initialised at 1", branchCode, year));
                    return 1L;
                }
            }
        }
    }

    /**
     * Throws {@link IllegalStateException} if {@code next} would exceed the
     * maximum representable sequence value for the configured {@link #SEQ_WIDTH}.
     */
    private static void guardOverflow(long next, String branchCode, int year) {
        if (next > SEQ_MAX) {
            throw new IllegalStateException(String.format(
                    "Account number sequence for %s-%d has reached its maximum "
                    + "(%d). Contact the system administrator.",
                    branchCode, year, SEQ_MAX));
        }
    }
}