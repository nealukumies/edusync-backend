package server;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public abstract class BaseHandler implements HttpHandler {
    protected final Gson gson;

    public BaseHandler() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalTime.class, (JsonSerializer<LocalTime>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("HH:mm")))
                )
                .create();
    }


        protected void sendResponse(HttpExchange exchange, int statusCode, Object responseObj) throws IOException {
        String jsonResponse = gson.toJson(responseObj);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    protected boolean isMethod(HttpExchange exchange, String method) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase(method)) {
            sendResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
            return false;
        }
        return true;
    }

    protected Map<String, String> parseJsonBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            return gson.fromJson(body, Map.class);
        } catch (Exception e) {
            sendResponse(exchange, 400, Map.of("error", "Invalid JSON"));
            return null; // Caller can check for null and return
        }
    }


    protected int getIdFromPath(HttpExchange exchange, int index) throws IOException {
        String[] pathParts = exchange.getRequestURI().getPath().split("/");

        if (pathParts.length <= index) {
            sendResponse(exchange, 400, Map.of("error", "Bad Request: Missing ID in path"));
            return -1;
        }
        try {
            return Integer.parseInt(pathParts[index]);
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, Map.of("error", "Bad Request: Invalid ID format"));
            return -1;
        }
    }

    protected int getIdFromHeader(HttpExchange exchange) throws IOException {
        String studentIdStr = exchange.getRequestHeaders().getFirst("student_id");
        if (studentIdStr == null) {
            sendResponse(exchange, 401, Map.of("error", "Unauthorized: Missing Student ID header"));
            return -1;
        }
        try {
            return Integer.parseInt(studentIdStr);
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, Map.of("error", "Bad Request: Invalid Student ID format"));
            return -1;
        }
    }

    protected String getRoleFromHeader(HttpExchange exchange) throws IOException {
        String role = exchange.getRequestHeaders().getFirst("role");
        if (role == null) {
            sendResponse(exchange, 401, Map.of("error", "Unauthorized: Missing role header"));
            return null;
        }
        return role;
    }

    protected boolean isAuthorized(HttpExchange exchange, int studentId) throws IOException {
        String role = getRoleFromHeader(exchange);
        if (role == null) {
            sendResponse(exchange, 401, Map.of("error", "Unauthorized: Missing role header"));
            return false;
        }

        int headerId = getIdFromHeader(exchange);
        if (headerId == -1) {
            sendResponse(exchange, 401, Map.of("error", "Unauthorized: Missing student id in header"));
            return false;
        }
        if (role.equals("user") && studentId != headerId) {
            sendResponse(exchange, 403, Map.of("error", "Forbidden: Insufficient permissions"));
            return false;
        }
        return true;
    }

}
