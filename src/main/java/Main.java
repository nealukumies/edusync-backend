import io.github.cdimascio.dotenv.Dotenv;
import server.Server;

import java.io.IOException;

/**
 * Main class to start the server.
 */
public class Main {
    private static Dotenv dotenv = Dotenv.load();

    /**
     * Main method to start the server.
     * Reads the port from environment variables and initializes the server.
     * throws IOException if server fails to start.
     */
    public static void main(String[] args) throws IOException {
        String portStr = dotenv.get("PORT");
        int port = (portStr != null) ? Integer.parseInt(portStr) : 8000;
        Server server = new Server(port);
        server.runServer();
    }
}
