package server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import database.StudentDao;
import service.AuthService;

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
        loginContext.setHandler(new LoginHandler(new AuthService()));

        HttpContext studentsContext = server.createContext("/students");
        studentsContext.setHandler(new StudentHandler(new StudentDao()));

        HttpContext coursesContext = server.createContext("/courses");
        coursesContext.setHandler(new CourseHandler());

        HttpContext assignmentsContext = server.createContext("/assignments");
        assignmentsContext.setHandler(new AssignmentHandler());

        HttpContext scheduleContext = server.createContext("/schedules");
        scheduleContext.setHandler(new ScheduleHandler());

        server.start();
        System.out.println("Server started on port " + port);
    }

}
