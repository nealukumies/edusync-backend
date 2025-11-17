package server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import database.AssignmentDao;
import database.CourseDao;
import database.ScheduleDao;
import database.StudentDao;
import service.AuthService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server class to handle HTTP requests for a student course management system.
 */
public class Server {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private final int port;

    public Server(int port) {
        this.port = port;

    }

    /**
     * Starts the HTTP server and sets up contexts for different endpoints.
     */
    public void runServer() throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        final HttpContext loginContext = httpServer.createContext("/login");
        loginContext.setHandler(new LoginHandler(new AuthService(new StudentDao())));

        final HttpContext studentsContext = httpServer.createContext("/students");
        studentsContext.setHandler(new StudentHandler(new StudentDao()));

        final HttpContext coursesContext = httpServer.createContext("/courses");
        coursesContext.setHandler(new CourseHandler(new CourseDao()));

        final HttpContext assignmentsContext = httpServer.createContext("/assignments");
        assignmentsContext.setHandler(new AssignmentHandler(new AssignmentDao()));

        final HttpContext scheduleContext = httpServer.createContext("/schedules");
        scheduleContext.setHandler(new ScheduleHandler(new ScheduleDao(), new CourseDao()));

        httpServer.start();
        LOGGER.log(Level.INFO, () -> "Server started on port " + port);
    }

}
