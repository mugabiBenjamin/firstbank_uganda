package ug.firstbank.persistence;

import ug.firstbank.model.AccountRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data-access object (DAO) for the {@code accounts} table.
 *
 * <p>Encapsulates all DML (INSERT, SELECT) against the {@code accounts}
 * table. Sequence generation is delegated to {@link SequenceGenerator};
 * connection management is delegated to {@link DatabaseManager}.</p>
 *
 * <p><b>SOLID notes:</b></p>
 * <ul>
 *   <li><b>SRP</b> — owns only account-row persistence. Schema DDL lives in
 *       {@link DatabaseManager}; sequence arithmetic lives in
 *       {@link SequenceGenerator}.</li>
 *   <li><b>OCP</b> — adding a new query (e.g. list all accounts for a branch)
 *       means adding one method; existing methods are untouched.</li>
 *   <li><b>DIP</b> — depends on {@link DatabaseManager} (for {@link Connection})
 *       and {@link SequenceGenerator} (for account numbers), both injected
 *       via constructor rather than instantiated here.</li>
 * </ul>
 *
 * <p>All write operations use manual transaction control
 * ({@code autoCommit=false}) so that the sequence counter increment and the
 * account row INSERT are committed or rolled back atomically.</p>
 */
public final class AccountRepository {

    private static final Logger LOG =
            Logger.getLogger(AccountRepository.class.getName());

    // ── Dependency ───────────────────────────────────────────────────────────

    private final DatabaseManager dbManager;

    // ── Constructor ──────────────────────────────────────────────────────────

    /**
     * Constructs an {@code AccountRepository} backed by the given
     * {@link DatabaseManager}.
     *
     * @param dbManager the shared database manager; must not be {@code null}
     */
    public AccountRepository(DatabaseManager dbManager) {
        if (dbManager == null) throw new IllegalArgumentException(
                "DatabaseManager must not be null.");
        this.dbManager = dbManager;
    }

    // ── Write operations ─────────────────────────────────────────────────────

    /**
     * Persists a new account record and returns the generated account number.
     *
     * <p>Steps performed inside a single transaction:</p>
     * <ol>
     *   <li>Generate the next account number via {@link SequenceGenerator}.</li>
     *   <li>Insert the full record into {@code accounts}.</li>
     *   <li>Commit both operations atomically.</li>
     * </ol>
     *
     * <p>The {@code record} object does not carry an account number on entry —
     * the number is generated here and returned to the caller
     * ({@code AccountService}), which then builds the final
     * {@link AccountRecord} with it.</p>
     *
     * @param record a fully validated {@link AccountRecord} whose
     *               {@code accountNumber} field should be the placeholder
     *               returned by this method
     * @return the generated account number, e.g. {@code "KLA-2026-000142"}
     * @throws SQLException if the insert fails or the connection is unavailable
     */
    public String save(AccountRecord record) throws SQLException {
        Connection conn = dbManager.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);

            // 1. Generate account number (increments account_seq row)
            String accountNumber = SequenceGenerator.nextAccountNumber(
                    conn, record.getBranch());

            // 2. Insert account row
            insertAccount(conn, record, accountNumber);

            // 3. Commit both operations together
            conn.commit();

            LOG.info("Account saved: " + accountNumber);
            return accountNumber;

        } catch (SQLException e) {
            safeRollback(conn);
            LOG.log(Level.SEVERE, "Failed to save account — transaction rolled back.", e);
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    // ── Read operations ──────────────────────────────────────────────────────

    /**
     * Checks whether an account already exists for the given NIN,
     * account type, and branch combination.
     *
     * <p>Used before INSERT to warn the user about likely duplicates —
     * a realistic guard for a bank's new-account form.</p>
     *
     * @param nin         primary NIN (14-char uppercase)
     * @param accountType account type display name, e.g. {@code "Savings"}
     * @param branch      branch display name, e.g. {@code "Kampala"}
     * @return {@code true} if a matching record already exists
     * @throws SQLException if the query fails
     */
    public boolean existsDuplicate(String nin, String accountType, String branch)
            throws SQLException {

        String sql =
                "SELECT COUNT(*) FROM accounts "
                + "WHERE nin = ? AND account_type = ? AND branch = ?";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, nin);
            ps.setString(2, accountType);
            ps.setString(3, branch);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Looks up an account by its generated account number.
     *
     * <p>Returns an {@link Optional} so the UI can handle the not-found
     * case without catching an exception.</p>
     *
     * @param accountNumber the account number to search for,
     *                      e.g. {@code "KLA-2026-000142"}
     * @return an {@link Optional} containing the matching record, or empty
     * @throws SQLException if the query fails
     */
    public Optional<AccountRecord> findByAccountNumber(String accountNumber)
            throws SQLException {

        String sql = "SELECT * FROM accounts WHERE account_number = ?";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, accountNumber.trim().toUpperCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Looks up all accounts associated with a given primary NIN.
     *
     * <p>A single NIN may have opened multiple accounts of different types or
     * at different branches, so this returns a {@link List}.</p>
     *
     * @param nin the primary NIN (14-char uppercase) to search for
     * @return unmodifiable list of matching records; empty if none found
     * @throws SQLException if the query fails
     */
    public List<AccountRecord> findByNin(String nin) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE nin = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, nin.trim().toUpperCase());

            try (ResultSet rs = ps.executeQuery()) {
                List<AccountRecord> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
                return Collections.unmodifiableList(results);
            }
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Executes the parameterised INSERT for one account row.
     *
     * <p>Column order mirrors the {@code CREATE TABLE} DDL in
     * {@link DatabaseManager#bootstrapSchema} exactly — any schema change
     * must be reflected here too.</p>
     */
    private void insertAccount(Connection conn,
                                AccountRecord record,
                                String accountNumber) throws SQLException {

        String sql =
                "INSERT INTO accounts "
                + "(account_number, first_name, last_name, nin, second_nin, "
                + " email, phone, dob, account_type, branch, "
                + " opening_deposit, pin_hash, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,  accountNumber);
            ps.setString(2,  record.getFirstName());
            ps.setString(3,  record.getLastName());
            ps.setString(4,  record.getNin());
            ps.setString(5,  record.getSecondNin());          // NULL for non-Joint
            ps.setString(6,  record.getEmail());
            ps.setString(7,  record.getPhone());
            ps.setString(8,  record.getDateOfBirth().toString()); // YYYY-MM-DD
            ps.setString(9,  record.getAccountType());
            ps.setString(10, record.getBranch());
            ps.setLong(11,   record.getOpeningDeposit());
            ps.setString(12, record.getPinHash());
            ps.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));

            ps.executeUpdate();
        }
    }

    /**
     * Maps a {@link ResultSet} row to an {@link AccountRecord}.
     *
     * <p>The {@code pin_hash} is carried through as-is — it is never
     * decrypted or logged.</p>
     *
     * <p>The {@code account_number} in the DB is the authoritative number;
     * it is used as-is without regeneration.</p>
     */
    private AccountRecord mapRow(ResultSet rs) throws SQLException {
        LocalDate dob = LocalDate.parse(rs.getString("dob")); // YYYY-MM-DD

        return new AccountRecord(
                rs.getString("account_number"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("nin"),
                rs.getString("second_nin"),          // may be NULL
                rs.getString("email"),
                rs.getString("phone"),
                dob,
                rs.getString("account_type"),
                rs.getString("branch"),
                rs.getLong("opening_deposit"),
                rs.getString("pin_hash")
        );
    }

    /**
     * Attempts a rollback without throwing — used in {@code catch} blocks
     * where the original exception must not be masked.
     */
    private void safeRollback(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, "Rollback failed.", ex);
        }
    }
}