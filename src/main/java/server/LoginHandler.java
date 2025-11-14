/**
 * This class handles login requests for students.
 */
package server;

import com.sun.net.httpserver.HttpExchange;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.Student;
import service.AuthService;

import java.io.IOException;
import java.util.Map;

public class LoginHandler extends BaseHandler {
    private final AuthService authService;

    /**
     * Constructor for LoginHandler.
     * @param authService The AuthService instance for authentication operations.
     */
    public LoginHandler(AuthService authService) {
        super();
        this.authService = authService;
    }

    /**
     * Handles incoming HTTP requests for login.
     * Only supports POST method for login attempts.
     * @param exchange the exchange containing the request from the
     *                 client and used to send the response
     * @throws IOException
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "POST")) { return; }

        Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap == null) { return; }

        String email = requestMap.get("email");
        String password = requestMap.get("password");

        if (email == null || password == null) {
            sendResponse(exchange, 400, Map.of("error", "Email and password are required"));
            return;
        }

        Student student = authService.tryLogin(email, password);
        if (student != null) {
            sendResponse(exchange, 200, Map.of(
                    "studentId", student.getId(),
                    "name", student.getName(),
                    "email", student.getEmail(),
                    "role", student.getRole()));
        } else {
            sendResponse(exchange, 401, Map.of("error", "Invalid email or password"));
        }
}
}
