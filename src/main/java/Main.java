import io.github.cdimascio.dotenv.Dotenv;
import server.Server;

import java.io.IOException;


public class Main {
    private static Dotenv dotenv = Dotenv.load();

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(dotenv.get("PORT"));
        Server server = new Server(port);
        server.runServer();
    }
}
