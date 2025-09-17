package server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Server {

    private int port;

    public Server(int port) {
        this.port = port;
    }

    public void runServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        HttpContext context = server.createContext("/login");
        context.setHandler(new LoginHandler());
        server.start();
    }

}
