/**
 * This class provides common functionalities for handling HTTP requests and responses,
 * including JSON serialization, method validation, request parsing, and authorization checks.
 */
package server;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public abstract class BaseHandler implements HttpHandler {
    protected final Gson gson;
    private static final String ERROR_KEY = "error";
    protected static final String METHOD_NOT_ALLOWED = "Method Not Allowed";
    protected static final String GET = "GET";
    protected static final String POST = "POST";
    protected static final String PUT = "PUT";
    protected static final String DELETE = "DELETE";


    protected BaseHandler() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalTime.class, (JsonSerializer<LocalTime>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("HH:mm")))
                )
                .create();
    }

    /**
     * Sends a JSON response with the given status code and response object.
     * @param exchange
     * @param statusCode
     * @param responseObj
     * @throws IOException
     */
        protected void sendResponse(HttpExchange exchange, int statusCode, Object responseObj) throws IOException {
        String jsonResponse = gson.toJson(responseObj);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    /**
     * Validates that the request method matches the expected method.
     * If not, sends a 405 Method Not Allowed response.
     * @param exchange
     * @param method
     * @return true if the method matches, false otherwise
     * @throws IOException
     */
    protected boolean isMethod(HttpExchange exchange, String method) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase(method)) {
            sendResponse(exchange, 405, Map.of(ERROR_KEY, "Method Not Allowed"));
            return false;
        }
        return true;
    }

    /**
     * Parses the JSON body of the request into a Map.
     * If parsing fails, sends a 400 Bad Request response.
     * @param exchange
     * @return
     * @throws IOException
     */
    protected Map<String, String> parseJsonBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            return gson.fromJson(body, Map.class);
        } catch (Exception e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid JSON"));
            return null; // Caller can check for null and return
        }
    }


    /**
     * Extracts an integer ID from the request path at the specified index.
     * If the ID is missing or invalid, sends a 400 Bad Request response.
     * @param exchange
     * @param index
     * @return
     * @throws IOException
     */
    protected int getIdFromPath(HttpExchange exchange, int index) throws IOException {
        String[] pathParts = exchange.getRequestURI().getPath().split("/");

        if (pathParts.length <= index) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Bad Request: Missing ID in path"));
            return -1;
        }
        try {
            return Integer.parseInt(pathParts[index]);
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Bad Request: Invalid ID format"));
            return -1;
        }
    }

    /**
     * Extracts the student ID from the request headers.
     * If the ID is missing or invalid, sends a 400 Bad Request or 401 Unauthorized response.
     * @param exchange
     * @return
     * @throws IOException
     */
    protected int getIdFromHeader(HttpExchange exchange) throws IOException {
        String studentIdStr = exchange.getRequestHeaders().getFirst("student_id");
        if (studentIdStr == null) {
            sendResponse(exchange, 401, Map.of(ERROR_KEY, "Unauthorized: Missing Student ID header"));
            return -1;
        }
        try {
            return Integer.parseInt(studentIdStr);
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Bad Request: Invalid Student ID format"));
            return -1;
        }
    }

    /**
     * Extracts the role from the request headers.
     * If the role is missing, sends a 401 Unauthorized response.
     * @param exchange
     * @return
     * @throws IOException
     */
    protected String getRoleFromHeader(HttpExchange exchange) throws IOException {
        String role = exchange.getRequestHeaders().getFirst("role");
        if (role == null) {
            sendResponse(exchange, 401, Map.of(ERROR_KEY, "Unauthorized: Missing role header"));
            return null;
        }
        return role;
    }

    /**
     * Checks if the requester is authorized to access or modify the resource
     * associated with the given student ID.
     * Admins have full access, while users can only access their own resources.
     * If unauthorized, sends a 401 Unauthorized or 403 Forbidden response.
     * @param exchange
     * @param studentId
     * @return
     * @throws IOException
     */
    protected boolean isAuthorized(HttpExchange exchange, int studentId) throws IOException {
        String role = getRoleFromHeader(exchange);
        if (role == null) {
            sendResponse(exchange, 401, Map.of(ERROR_KEY, "Unauthorized: Missing role header"));
            return false;
        }

        int headerId = getIdFromHeader(exchange);
        if (headerId == -1) {
            sendResponse(exchange, 401, Map.of(ERROR_KEY, "Unauthorized: Missing student id in header"));
            return false;
        }
        if ("user".equals(role) && studentId != headerId) {
            sendResponse(exchange, 403, Map.of(ERROR_KEY, "Forbidden: Insufficient permissions"));
            return false;
        }
        return true;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        switch (method) {
            case GET -> handleGet(exchange);
            case POST -> handlePost(exchange);
            case PUT -> handlePut(exchange);
            case DELETE -> handleDelete(exchange);
            default -> sendResponse(exchange, 405, Map.of(ERROR_KEY, METHOD_NOT_ALLOWED));
        }
    }

    protected void handleGet(HttpExchange exchange) throws IOException {}
    protected void handlePost(HttpExchange exchange) throws IOException {}
    protected void handlePut(HttpExchange exchange) throws IOException {}
    protected void handleDelete(HttpExchange exchange) throws IOException {}

}
