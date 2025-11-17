package server;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * This class provides common functionalities for handling HTTP requests and responses,
 * including JSON serialization, method validation, request parsing, and authorization checks.
 */
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
     *
     * @param exchange The HttpExchange object for the request/response
     * @param statusCode The HTTP status code to send
     * @param responseObj The response object to serialize to JSON
     * @throws IOException Throws IOException if an I/O error occurs
     */
    protected void sendResponse(HttpExchange exchange, int statusCode, Object responseObj) throws IOException {
        final String jsonResponse = gson.toJson(responseObj);
        final byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

        setJsonContentType(exchange);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
            outputStream.flush();
        }
    }

    /** Helper to set JSON content-type, avoids direct Headers access in main method */
    private void setJsonContentType(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
    }

    /**
     * Validates that the request method matches the expected method.
     * If not, sends a 405 Method Not Allowed response.
     *
     * @param exchange The HttpExchange object for the request/response
     * @param method The expected HTTP method
     * @return true if the method matches, false otherwise
     * @throws IOException Throws IOException if an I/O error occurs
     */
    protected boolean isMethod(HttpExchange exchange, String method) throws IOException {
        boolean success = true;
        if (!exchange.getRequestMethod().equalsIgnoreCase(method)) {
            sendResponse(exchange, 405, Map.of(ERROR_KEY, METHOD_NOT_ALLOWED));
            success = false;
        }
        return success;
    }

    /**
     * Parses the JSON body of the request into a Map.
     * If parsing fails, sends a 400 Bad Request response.
     *
     * @param exchange The HttpExchange object for the request/response
     * @return A Map representing the parsed JSON body
     * @throws IOException Throws IOException if an I/O error occurs
     */
    protected Map<String, String> parseJsonBody(HttpExchange exchange) throws IOException {
        final String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            return gson.fromJson(body, Map.class);
        } catch (JsonSyntaxException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid JSON"));
            return Map.of();
        }
    }


    /**
     * Extracts an integer ID from the request path at the specified index.
     * If the ID is missing or invalid, sends a 400 Bad Request response.
     *
     * @param exchange The HttpExchange object for the request/response
     * @param index The index in the path segments where the ID is expected
     * @return The extracted integer ID, or -1 if invalid
     * @throws IOException Throws IOException if an I/O error occurs
     */
    protected int getIdFromPath(HttpExchange exchange, int index) throws IOException {
        int result = -1;
        final String[] pathParts = getPathParts(exchange);

        if (index >= pathParts.length) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Bad Request: Missing ID in path"));
        } else {
            try {
                result = Integer.parseInt(pathParts[index]);
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, Map.of(ERROR_KEY, "Bad Request: Invalid ID format"));
            }
        }
        return result;
    }

    /**
     * Splits the request path into its segments.
     *
     * @param exchange The HttpExchange object for the request/response
     * @return An array of path segments
     */
    protected String[] getPathParts(HttpExchange exchange) {
        final URI uri = exchange.getRequestURI();
        final String path = uri.getPath();
        return path.split("/");
    }


    /**
     * Extracts the student ID from the request headers.
     * If the ID is missing or invalid, sends a 400 Bad Request or 401 Unauthorized response.
     *
     * @param exchange The HttpExchange object for the request/response
     * @return The extracted student ID, or -1 if invalid
     * @throws IOException Throws IOException if an I/O error occurs
     */
    protected int getIdFromHeader(HttpExchange exchange) throws IOException {
        int result = -1;
        final String studentIdStr = getStudentIdHeader(exchange);

        if (studentIdStr == null) {
            sendResponse(exchange, 401, Map.of(ERROR_KEY, "Unauthorized: Missing Student ID header"));
        } else {
            try {
                result = Integer.parseInt(studentIdStr);
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, Map.of(ERROR_KEY, "Bad Request: Invalid Student ID format"));
            }
        }
        return result;
    }


    /**
     * Extracts the role from the request headers.
     * If the role is missing, sends a 401 Unauthorized response.
     *
     * @param exchange The HttpExchange object for the request/response
     * @return The extracted role, or null if missing
     * @throws IOException Throws IOException if an I/O error occurs
     */
    protected String getRoleFromHeader(HttpExchange exchange) throws IOException {
        String role = null;
        if (exchange.getRequestHeaders().getFirst("role") != null) {
            role = getRoleHeader(exchange);
        }
        return role;
    }

    /**
     * Checks if the requester is authorized to access or modify the resource
     * associated with the given student ID.
     * Admins have full access, while users can only access their own resources.
     * If unauthorized, sends a 401 Unauthorized or 403 Forbidden response.
     *
     * @param exchange The HttpExchange object for the request/response
     * @param studentId The student ID associated with the resource
     * @return true if authorized, false otherwise
     * @throws IOException Throws IOException if an I/O error occurs
     */
    protected boolean isAuthorized(HttpExchange exchange, int studentId) throws IOException {
        boolean success = true;
        final String role = getRoleFromHeader(exchange);
        if (role == null) {
            sendResponse(exchange, 401, Map.of(ERROR_KEY, "Unauthorized: Missing role header"));
            success = false;
        }

        final int headerId = getIdFromHeader(exchange);
        if (headerId == -1) {
            sendResponse(exchange, 401, Map.of(ERROR_KEY, "Unauthorized: Missing student id in header"));
            success = false;
        }
        if ("user".equals(role) && studentId != headerId) {
            sendResponse(exchange, 403, Map.of(ERROR_KEY, "Forbidden: Insufficient permissions"));
            success = false;
        }
        return success;
    }

    /**
     * Parses an entity ID from the request path and validates it.
     * If the ID is missing or invalid, sends a 400 Bad Request response with a custom message.
     *
     * @param exchange   The HttpExchange object for the request/response
     * @param entityName The name of the entity for error messaging
     * @return The extracted integer ID, or null if invalid
     * @throws IOException Throws IOException if an I/O error occurs
     */
    protected Integer parseEntityId(HttpExchange exchange, String entityName) throws IOException {
        final int id = getIdFromPath(exchange, 2);
        if (id == -1) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, entityName + " ID is required or invalid"));
        }
        return id;
    }

    /**
     * Extracts the student ID from the request headers.
     *
     * @param exchange The HttpExchange object for the request/response
     * @return The student ID from the headers
     */
    private String getStudentIdHeader(HttpExchange exchange) {
        return exchange.getRequestHeaders().getFirst("student_id");
    }

    /**
     * Extracts the role from the request headers.
     *
     * @param exchange The HttpExchange object for the request/response
     * @return The role from the headers
     */
    private String getRoleHeader(HttpExchange exchange) {
        return exchange.getRequestHeaders().getFirst("role");
    }

    /**
     * Handles incoming HTTP requests.
     * Delegates to specific methods based on the HTTP method (GET, POST, PUT, DELETE).
     * If the method is not supported, sends a 405 Method Not Allowed response.
     * @param exchange the exchange containing the request from the
     *                 client and used to send the response
     * @throws IOException Throws IOException if an I/O error occurs
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        final String method = exchange.getRequestMethod().toUpperCase();
        switch (method) {
            case GET -> handleGet(exchange);
            case POST -> handlePost(exchange);
            case PUT -> handlePut(exchange);
            case DELETE -> handleDelete(exchange);
            default -> sendResponse(exchange, 405, Map.of(ERROR_KEY, METHOD_NOT_ALLOWED));
        }
    }

    /**
     * Handles GET requests, should be overridden by subclasses.
     *
     * @param exchange The HttpExchange object for the request/response
     * @throws IOException Throws IOException if an I/O error occurs
     */
    protected void handleGet(HttpExchange exchange) throws IOException{
        sendResponse(exchange, 405, Map.of(ERROR_KEY, "GET not allowed"));
    }
    /**
     * Handles POST requests, should be overridden by subclasses.
     *
     * @param exchange The HttpExchange object for the request/response
     * @throws IOException Throws IOException if an I/O error occurs
     */
    protected void handlePost(HttpExchange exchange) throws IOException{
        sendResponse(exchange, 405, Map.of(ERROR_KEY, "POST not allowed"));
    }
    /**
     * Handles PUT requests, should be overridden by subclasses.
     *
     * @param exchange The HttpExchange object for the request/response
     * @throws IOException Throws IOException if an I/O error occurs
     */
    protected void handlePut(HttpExchange exchange) throws IOException{
        sendResponse(exchange, 405, Map.of(ERROR_KEY, "PUT not allowed"));
    }

    /**
     * Handles DELETE requests, should be overridden by subclasses.
     * @param exchange The HttpExchange object for the request/response
     * @throws IOException Throws IOException if an I/O error occurs
     */
    protected void handleDelete(HttpExchange exchange) throws IOException{
        sendResponse(exchange, 405, Map.of(ERROR_KEY, "DELETE not allowed"));
    }

}
