/**
 * Server class to handle HTTP requests for a student course management system.
 */
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

public class Server {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private int port;

    public Server(int port) {
        this.port = port;
    }

    /**
     * Starts the HTTP server and sets up contexts for different endpoints.
     * @throws IOException
     */
    public void runServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        HttpContext loginContext = server.createContext("/login");
        loginContext.setHandler(new LoginHandler(new AuthService(new StudentDao())));

        HttpContext studentsContext = server.createContext("/students");
        studentsContext.setHandler(new StudentHandler(new StudentDao()));

        HttpContext coursesContext = server.createContext("/courses");
        coursesContext.setHandler(new CourseHandler(new CourseDao()));

        HttpContext assignmentsContext = server.createContext("/assignments");
        assignmentsContext.setHandler(new AssignmentHandler(new AssignmentDao()));

        HttpContext scheduleContext = server.createContext("/schedules");
        scheduleContext.setHandler(new ScheduleHandler(new ScheduleDao(), new CourseDao()));

        server.start();
        LOGGER.log(Level.SEVERE, () -> "Server started on port " + port);
    }

}
