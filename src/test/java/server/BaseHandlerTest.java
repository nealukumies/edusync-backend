package server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.testutils.DummyHandler;
import server.testutils.MockHttpExchange;
import java.io.IOException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class BaseHandlerTest {
    private BaseHandler baseHandler;

    /**
     * Sets up a new instance of DummyHandler before each test.
     */
    @BeforeEach
    void setUp() {
        baseHandler = new DummyHandler();
    }

    /**
     * Tests the isMethod function of BaseHandler. Verifies that it correctly identifies HTTP methods.
     * @throws Exception
     */
    @Test
    void isMethodReturnsFalseForMismatchedMethod() throws Exception {
        MockHttpExchange exchange = new MockHttpExchange("POST", "/students", "");
        boolean result = baseHandler.isMethod(exchange, "GET");
        assertFalse(result, "Expected isMethod to return false for GET when method is POST");
    }

    /**
     * Tests the isMethod function of BaseHandler. Verifies that it correctly identifies HTTP methods.
     * @throws Exception
     */
    @Test
    void isMethodReturnsTrueForPostMethod() throws Exception {
        MockHttpExchange exchange = new MockHttpExchange("POST", "/students", "");
        boolean result = baseHandler.isMethod(exchange, "POST");
        assertTrue(result, "Expected isMethod to return true for POST when method is POST");
    }

    /**
     * Tests the isMethod function of BaseHandler. Verifies that it correctly identifies HTTP methods.
     * @throws Exception
     */
    @Test
    void isMethodReturnsTrueForDeleteMethod() throws Exception {
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/students", "");
        boolean result = baseHandler.isMethod(exchange, "DELETE");
        assertTrue(result, "Expected isMethod to return true for DELETE when method is DELETE");
    }

    /**
     * Tests the isMethod function of BaseHandler. Verifies that it correctly identifies HTTP methods.
     * @throws Exception
     */
    @Test
    void isMethodReturnsTrueForPutMethod() throws Exception {
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/students", "");
        boolean result = baseHandler.isMethod(exchange, "PUT");
        assertTrue(result, "Expected isMethod to return true for PUT when method is PUT");
    }

    /**
     * Tests the parseJsonBody function of BaseHandler. Verifies that it correctly parses a valid JSON body and returns null for invalid JSON.
     * @throws Exception
     */
    @Test
    void testParseJsonBody() throws Exception {
        String json = "{\"key\":\"value\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/students", json);
        exchange = new MockHttpExchange("POST", "/students", "invalid json");
        Map<String, String> result = baseHandler.parseJsonBody(exchange);
        assertNull(result, "Expected parseJsonBody to return null for invalid JSON");
    }

    /**
     * Tests the getIdFromPath function of BaseHandler. Verifies that it correctly extracts an integer ID from the URL path.
     * @throws IOException
     */
    @Test
    void testGetIdFromPath() throws IOException {
        MockHttpExchange exchange = new MockHttpExchange("POST", "/students/123", "");
        exchange = new MockHttpExchange("POST", "/students/abc", "");
        int id = baseHandler.getIdFromPath(exchange, 2);
        assertEquals(-1, id, "Expected extracted ID to be -1 for invalid format");
    }

    /**
     * Tests the getRoleFromHeader function of BaseHandler. Verifies that it correctly extracts the role from request headers.
     * @throws IOException
     */
    @Test
    void testGetRoleFromHeader() throws IOException {
        MockHttpExchange exchange = new MockHttpExchange("POST", "/students/123", "");
        String role = baseHandler.getRoleFromHeader(exchange);
        assertNull(role, "Expected role to be null when header is missing");
        exchange.getRequestHeaders().add("role", "student");
        role = baseHandler.getRoleFromHeader(exchange);
        assertEquals("student", role, "Expected role to be 'student'");
    }

    /**
     * Tests the getIdFromHeader function of BaseHandler. Verifies that it correctly extracts the student ID from request headers.
     * @throws IOException
     */
    @Test
    void testGetIdFromHeader() throws IOException {
        MockHttpExchange exchange = new MockHttpExchange("POST", "/students/123", "");
        int id = baseHandler.getIdFromHeader(exchange);
        assertEquals(-1, id, "Expected ID to be -1 when header is missing");
        exchange.getRequestHeaders().add("student_id", "456");
        id = baseHandler.getIdFromHeader(exchange);
        assertEquals(456, id, "Expected extracted ID to be 456");
        exchange.getRequestHeaders().set("student_id", "abc");
        id = baseHandler.getIdFromHeader(exchange);
        assertEquals(-1, id, "Expected extracted ID to be -1 for invalid format");
    }

    /**
     * Tests the isAuthorized function of BaseHandler. Verifies that it correctly checks authorization based on student ID and role.
     * @throws IOException
     */
    @Test
    void testAuthorizationFails() throws IOException {
        MockHttpExchange exchange = new MockHttpExchange("POST", "/students/123", "");
        boolean authorized = baseHandler.isAuthorized(exchange, 456);
        assertFalse(authorized, "Expected authorization to fail when student ID does not match and role is missing");
    }

    /**
     * Tests the isAuthorized function of BaseHandler. Verifies that it correctly authorizes when student ID matches or role is admin.
     * @throws IOException
     */
    @Test
    void testAuthorizationSucceeds() throws IOException {
        MockHttpExchange exchange = new MockHttpExchange("POST", "/students/123", "");
        exchange.withHeader("student_id", "123").withHeader("role", "user");
        boolean authorized = baseHandler.isAuthorized(exchange, 123);
        assertTrue(authorized, "Expected authorization to succeed when student ID matches");
    }

    /**
     * Tests the isAuthorized function of BaseHandler. Verifies that it correctly authorizes admin role regardless of student ID.
     * @throws IOException
     */
    @Test
    void testAuthorizationAdmin() throws IOException {
        MockHttpExchange exchange = new MockHttpExchange("POST", "/students/123", "");
        exchange.getRequestHeaders().add("student_id", "456");
        exchange.getRequestHeaders().add("role", "admin");
        boolean authorized = baseHandler.isAuthorized(exchange, 123);
        assertTrue(authorized, "Expected authorization to succeed for admin role regardless of student ID");
    }

    /**
     * Tests the sendResponse function of BaseHandler. Verifies that it correctly sends an HTTP response with the given status code and body.
     * @throws IOException
     */
    @Test
    void testSendResponse() throws IOException {
        MockHttpExchange exchange = new MockHttpExchange("GET", "/students", "");
        baseHandler.sendResponse(exchange, 200, Map.of("message", "OK"));
        assertEquals(200, exchange.getResponseCode(), "Expected response code to be 200");
        assertEquals("{\"message\":\"OK\"}", exchange.getResponseBodyAsString(), "Expected response body to match the sent JSON");
    }




}
