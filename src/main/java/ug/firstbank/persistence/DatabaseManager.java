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

public final class DatabaseManager {

    private static final Logger LOG =
            Logger.getLogger(DatabaseManager.class.getName());

    private static final String DB_PATH_PROPERTY = "app.db.path";

    /** Default relative path used when the system property is absent. */
    private static final String DB_PATH_DEFAULT = "data/firstbank.accdb";

    /** Cached connection — one per application lifetime. */
    private Connection connection;

    // ── Connection lifecycle ─────────────────────────────────────────────────

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = openConnection();
            bootstrapSchema(connection);
        }
        return connection;
    }

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

    private Connection openConnection() throws SQLException {
        File dbFile = resolveDbFile();
        LOG.info("Connecting to database: " + dbFile.getAbsolutePath());

        // Ensure parent directory exists so UCanAccess can create the file.
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        boolean needsCreation = !dbFile.exists();

        StringBuilder url = new StringBuilder("jdbc:ucanaccess://")
                .append(dbFile.getAbsolutePath())
                .append(";memory=false")
                .append(";openLinksAsWriteable=true")
                .append(";ignoreCase=true");

        if (needsCreation) {
            // UCanAccess will NOT create a missing .accdb unless explicitly
            // told which Access file format version to create it in.
            url.append(";newDatabaseVersion=V2010");
            LOG.info("Database file does not exist — creating new V2010 .accdb.");
        }

        Connection conn = DriverManager.getConnection(url.toString());

        if (needsCreation) {
            LOG.info("New database file created: " + dbFile.getAbsolutePath());
        }

        return conn;
    }

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