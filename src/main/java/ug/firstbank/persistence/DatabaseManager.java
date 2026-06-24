package ug.firstbank.persistence;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the JDBC connection to the MS Access (.accdb) database via the
 * UCanAccess bridge, and bootstraps the schema on first run.
 *
 * <p><b>Responsibilities (SRP):</b></p>
 * <ol>
 *   <li>Resolve the database file path from the {@code app.db.path} system
 *       property (set via Maven's {@code javafx-maven-plugin} options in
 *       {@code pom.xml}), falling back to a path relative to the JAR
 *       location — ensuring portability across Ubuntu (dev) and Windows
 *       (runtime) without any hardcoded paths.</li>
 *   <li>Open and lazily cache a single shared {@link Connection}.</li>
 *   <li>Execute {@code CREATE TABLE IF NOT EXISTS} DDL on first connection
 *       so the schema self-bootstraps — no manual SQL scripts required.</li>
 *   <li>Provide a {@link #close()} method so callers (e.g. the JavaFX
 *       {@code Application.stop()} hook) can release the connection cleanly.</li>
 * </ol>
 *
 * <p><b>SOLID notes:</b></p>
 * <ul>
 *   <li><b>SRP</b> — only connection management and DDL live here.
 *       Query logic belongs in {@link AccountRepository}.</li>
 *   <li><b>OCP</b> — the DDL block is a private method; adding a new table
 *       means adding one {@code CREATE TABLE IF NOT EXISTS} statement inside
 *       {@link #bootstrapSchema(Connection)} with no other changes.</li>
 *   <li><b>DIP</b> — {@link AccountRepository} and {@link SequenceGenerator}
 *       receive a {@link Connection} injected by this class; they never call
 *       {@code DriverManager} directly.</li>
 * </ul>
 *
 * <p>UCanAccess creates the {@code .accdb} file automatically when the JDBC
 * URL points to a non-existent path — no binary template is needed in the
 * repository.</p>
 */
public final class DatabaseManager {

    private static final Logger LOG =
            Logger.getLogger(DatabaseManager.class.getName());

    /**
     * System property key set by the JavaFX Maven plugin via
     * {@code <option>-Dapp.db.path=data/firstbank.accdb</option>}
     * in {@code pom.xml}. When running from a packaged JAR without Maven,
     * the fallback path resolution kicks in.
     */
    private static final String DB_PATH_PROPERTY = "app.db.path";

    /** Default relative path used when the system property is absent. */
    private static final String DB_PATH_DEFAULT = "data/firstbank.accdb";

    /** Cached connection — one per application lifetime. */
    private Connection connection;

    // ── Connection lifecycle ─────────────────────────────────────────────────

    /**
     * Returns the shared {@link Connection}, opening it on first call.
     *
     * <p>The database file is resolved in this order:</p>
     * <ol>
     *   <li>{@code -Dapp.db.path} system property (set by Maven plugin)</li>
     *   <li>Sibling {@code data/firstbank.accdb} relative to the directory
     *       containing the running JAR</li>
     *   <li>Current working directory fallback</li>
     * </ol>
     *
     * @return an open {@link Connection}
     * @throws SQLException if the connection cannot be established
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = openConnection();
            bootstrapSchema(connection);
        }
        return connection;
    }

    /**
     * Closes the shared connection if it is open.
     * Should be called from {@code Application.stop()}.
     */
    public synchronized void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    LOG.info("Database connection closed.");
                }
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "Error closing database connection.", e);
            } finally {
                connection = null;
            }
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Opens a UCanAccess JDBC connection to the resolved {@code .accdb} file.
     *
     * <p>UCanAccess connection options used:</p>
     * <ul>
     *   <li>{@code memory=false} — keeps the Jackcess working copy on disk
     *       rather than fully in heap; safer for larger databases.</li>
     *   <li>{@code openLinksAsWriteable=true} — required for INSERT/UPDATE.</li>
     *   <li>{@code ignoreCase=true} — makes string comparisons case-insensitive,
     *       matching MS Access default behaviour.</li>
     * </ul>
     */
    private Connection openConnection() throws SQLException {
        File dbFile = resolveDbFile();
        LOG.info("Connecting to database: " + dbFile.getAbsolutePath());

        // Ensure parent directory exists so UCanAccess can create the file.
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        String url = "jdbc:ucanaccess://" + dbFile.getAbsolutePath()
                + ";memory=false"
                + ";openLinksAsWriteable=true"
                + ";ignoreCase=true";

        return DriverManager.getConnection(url);
    }

    /**
     * Resolves the database file path without hardcoding any absolute path.
     *
     * <p>Priority:</p>
     * <ol>
     *   <li>{@code -Dapp.db.path} — relative paths are resolved against the
     *       current working directory (which Maven sets to the project root).</li>
     *   <li>Sibling {@code data/firstbank.accdb} next to the JAR file.</li>
     *   <li>Current working directory as last resort.</li>
     * </ol>
     */
    private File resolveDbFile() {
        String prop = System.getProperty(DB_PATH_PROPERTY);
        if (prop != null && !prop.isBlank()) {
            Path p = Paths.get(prop);
            return p.isAbsolute() ? p.toFile()
                                  : Paths.get(System.getProperty("user.dir"))
                                         .resolve(p).toFile();
        }

        // Fallback: resolve relative to the JAR's parent directory.
        try {
            File jar = new File(
                    DatabaseManager.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI());
            File jarDir = jar.isFile() ? jar.getParentFile() : jar;
            return new File(jarDir, DB_PATH_DEFAULT);
        } catch (URISyntaxException e) {
            LOG.log(Level.WARNING,
                    "Could not resolve JAR location; using working directory.", e);
            return new File(System.getProperty("user.dir"), DB_PATH_DEFAULT);
        }
    }

    /**
     * Creates all required tables if they do not already exist.
     *
     * <p>UCanAccess supports a subset of SQL DDL; the statements below use
     * only constructs that UCanAccess 5.x recognises.</p>
     *
     * <p><b>Tables:</b></p>
     * <ul>
     *   <li>{@code accounts} — one row per opened account.</li>
     *   <li>{@code account_seq} — per-branch-per-year sequential counter;
     *       the unique constraint on {@code (branch_code, seq_year)} ensures
     *       the counter survives application restarts correctly.</li>
     * </ul>
     *
     * @param conn an open connection on which to execute DDL
     * @throws SQLException if DDL execution fails
     */
    private void bootstrapSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            // ── accounts table ───────────────────────────────────────────────
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS accounts ("
                + "  id               AUTOINCREMENT  PRIMARY KEY, "
                + "  account_number   TEXT(20)       NOT NULL, "
                + "  first_name       TEXT(50)       NOT NULL, "
                + "  last_name        TEXT(50)       NOT NULL, "
                + "  nin              TEXT(14)       NOT NULL, "
                + "  second_nin       TEXT(14),                "   // NULL for non-Joint
                + "  email            TEXT(100)      NOT NULL, "
                + "  phone            TEXT(15)       NOT NULL, "
                + "  dob              TEXT(10)       NOT NULL, "   // YYYY-MM-DD
                + "  account_type     TEXT(20)       NOT NULL, "
                + "  branch           TEXT(20)       NOT NULL, "
                + "  opening_deposit  CURRENCY       NOT NULL, "
                + "  pin_hash         TEXT(80)       NOT NULL, "   // bcrypt 60-char hash
                + "  created_at       DATETIME       NOT NULL  "
                + ")"
            );

            // ── account_seq table ────────────────────────────────────────────
            // No IF NOT EXISTS for CONSTRAINT in UCanAccess DDL; the table-level
            // CREATE TABLE IF NOT EXISTS guard is sufficient.
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS account_seq ("
                + "  id           AUTOINCREMENT  PRIMARY KEY, "
                + "  branch_code  TEXT(5)        NOT NULL, "
                + "  seq_year     INTEGER        NOT NULL, "
                + "  last_seq     INTEGER        NOT NULL    "   // DEFAULT 0 via INSERT
                + ")"
            );

            LOG.info("Schema bootstrap complete.");
        }
    }
}