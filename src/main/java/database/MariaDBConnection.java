package database;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton class to manage MariaDB database connection.
 */
public final class MariaDBConnection {
    /** Logger for logging errors and information. */
    private static final Logger LOGGER = Logger.getLogger(MariaDBConnection.class.getName());

    @SuppressFBWarnings(
            value = "MS_EXPOSE_REP",
            justification = "Singleton connection is controlled; no exposure risk"
    )

    private static Connection conn;
    private static final Dotenv DOTENV = Dotenv.load();

    private MariaDBConnection() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the singleton database connection. Initializes the connection if not already done.
     *
     * @return Connection - the MariaDB database connection
     */
    public static synchronized Connection getConnection() {
        final String url = DOTENV.get("DB_URL");
        final String user = DOTENV.get("DB_USER");
        final String password = DOTENV.get("DB_PASSWORD");

        if (url == null || user == null || password == null) {
            throw new IllegalStateException("Database credentials are not set in .env");
        }

        if (conn==null) {
            try {
                conn = DriverManager.getConnection(url, user, password);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, () -> "Failed to connect to MariaDB: " + e.getMessage());
            }
        } return conn;
    }
}
