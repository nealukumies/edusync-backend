package database;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MariaDBConnection {
    private static final Logger LOGGER = Logger.getLogger(MariaDBConnection.class.getName());

    @SuppressFBWarnings(
            value = "MS_EXPOSE_REP",
            justification = "Singleton connection is controlled; no exposure risk"
    )
    private static Connection conn;
    private static Dotenv dotenv = Dotenv.load();

    private MariaDBConnection() {
        // Private constructor to prevent instantiation
    }

    public static synchronized Connection getConnection() {
        final String url = dotenv.get("DB_URL");
        final String user = dotenv.get("DB_USER");
        final String password = dotenv.get("DB_PASSWORD");

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
