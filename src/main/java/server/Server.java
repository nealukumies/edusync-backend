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

        HttpContext loginContext = server.createContext("/login");
        loginContext.setHandler(new LoginHandler());

        HttpContext studentsContext = server.createContext("/students");
        studentsContext.setHandler(new StudentHandler());

        HttpContext coursesContext = server.createContext("/courses");
        coursesContext.setHandler(new CourseHandler());

        server.start();
    }

}
