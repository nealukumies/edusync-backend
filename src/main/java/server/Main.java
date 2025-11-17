package server;

import java.io.IOException;

/**
 * Main class to start the server.
 */
public class Main {

    /**
     * Main method to start the server.
     * Reads the port from environment variables and initializes the server
     * on that port. If no port is specified, defaults to 8000.
     * throws IOException if server fails to start.
     */
    public static void main(String[] args) throws IOException {
        final String portStr = System.getenv("PORT");
        final int port = (portStr != null) ? Integer.parseInt(portStr) : 8000;
        final Server server = new Server(port);
        server.runServer();
    }
}
