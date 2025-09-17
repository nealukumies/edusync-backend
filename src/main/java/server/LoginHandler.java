package server;

import com.sun.net.httpserver.HttpExchange;
import service.AuthService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LoginHandler extends BaseHandler {
    private final AuthService authService = new AuthService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> requestMap;
        try {
            requestMap = gson.fromJson(body, Map.class);
        } catch (Exception e) {
            sendResponse(exchange, 400, Map.of("error", "Invalid JSON"));
            return;
        }
        String email = requestMap.get("email");
        String password = requestMap.get("password");
        if (email == null || password == null) {
            sendResponse(exchange, 400, Map.of("error", "Email and password are required"));
            return;
        }

        int studentId = authService.tryLogin(email, password);
        if (studentId != -1) {
            sendResponse(exchange, 200, Map.of("studentId", studentId));
        } else {
            sendResponse(exchange, 401, Map.of("error", "Invalid email or password"));
        }

}
}
