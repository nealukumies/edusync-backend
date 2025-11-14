package database;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MariaDBConnection {

    private static Connection conn = null;
    private static Dotenv dotenv = Dotenv.load();

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "Singleton connection is controlled; no exposure risk"
    )

    public static Connection getConnection() {
        String url = dotenv.get("DB_URL");
        String user = dotenv.get("DB_USER");
        String password = dotenv.get("DB_PASSWORD");

        if (url == null || user == null || password == null) {
            throw new IllegalStateException("Database credentials are not set in .env");
        }

        if (conn==null) {
            try {
                conn = DriverManager.getConnection(url, user, password);
            } catch (SQLException e) {
                System.out.println("Connection failed.");
                e.printStackTrace();
            }
        } return conn;
    }
}
