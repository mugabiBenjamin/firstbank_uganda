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

public final class AccountRepository {

    private static final Logger LOG =
            Logger.getLogger(AccountRepository.class.getName());

    // ── Dependency ───────────────────────────────────────────────────────────

    private final DatabaseManager dbManager;

    // ── Constructor ──────────────────────────────────────────────────────────

    public AccountRepository(DatabaseManager dbManager) {
        if (dbManager == null) throw new IllegalArgumentException(
                "DatabaseManager must not be null.");
        this.dbManager = dbManager;
    }

    // ── Write operations ─────────────────────────────────────────────────────

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

    private void safeRollback(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, "Rollback failed.", ex);
        }
    }
}