package ug.firstbank.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.logging.Logger;

public final class SequenceGenerator {

    private static final Logger LOG =
            Logger.getLogger(SequenceGenerator.class.getName());

    // ── Branch code registry ─────────────────────────────────────────────────

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

    public static String branchCode(String branchDisplayName) {
        String code = BRANCH_CODES.get(branchDisplayName);
        if (code == null) {
            throw new IllegalArgumentException(
                    "Unknown branch: '" + branchDisplayName + "'. "
                    + "Known branches: " + BRANCH_CODES.keySet());
        }
        return code;
    }

    public static String nextAccountNumber(Connection conn,
                                           String branchDisplayName)
            throws SQLException {

        String code = branchCode(branchDisplayName);
        int    year = LocalDate.now().getYear();

        long next = fetchAndIncrement(conn, code, year);

        return String.format("%s-%d-%0" + SEQ_WIDTH + "d", code, year, next);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

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

    private static void guardOverflow(long next, String branchCode, int year) {
        if (next > SEQ_MAX) {
            throw new IllegalStateException(String.format(
                    "Account number sequence for %s-%d has reached its maximum "
                    + "(%d). Contact the system administrator.",
                    branchCode, year, SEQ_MAX));
        }
    }
}