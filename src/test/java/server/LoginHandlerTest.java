package server;

import model.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.testutils.MockHttpExchange;
import service.AuthService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LoginHandlerTest{
    private AuthService fakeAuthService;
    private LoginHandler handler;

    /**
     * Sets up the test environment before each test case. Mocks the AuthService and initializes the LoginHandler.
     */
    @BeforeEach
    void setup() {
        fakeAuthService = mock(AuthService.class);
        handler = new LoginHandler(fakeAuthService);
    }

    /**
     * Tests handling of unsupported HTTP methods. Expects a 405 Method Not Allowed response.
     */
    @Test
    void wrongMethodTest() {
        MockHttpExchange exchange = new MockHttpExchange("GET", "/login", "");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in login handler: " + e.getMessage());
        }
        assertEquals(405, exchange.getResponseCode());
        assertTrue(exchange.getResponseBodyAsString().contains("Method Not Allowed"));
    }

    /**
     * Tests handling of requests with missing email or password fields. Expects a 400 Bad Request response.
     */
    @Test
    void missingDetailsTest() {
        String body = "{}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/login", body);
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in login handler: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        assertTrue(exchange.getResponseBodyAsString().contains("Email and password are required"));
    }

    /**
     * Tests a successful login attempt with valid credentials. Expects a 200 OK response with student details.
     */
    @Test
    void successfulLoginTest() {
        when(fakeAuthService.tryLogin("test@example.com", "password"))
                .thenReturn(new Student(1, "Test", "test@example.com", "user"));
        String body = "{ \"email\": \"test@example.com\", \"password\": \"password\" }";
        try {
            MockHttpExchange exchange = new MockHttpExchange("POST", "/login", body);
            handler.handle(exchange);
            assertEquals(200, exchange.getResponseCode());
            assertTrue(exchange.getResponseBodyAsString().contains("test@example.com"));
        } catch (Exception e) {
            fail("Exception thrown in login handler: " + e.getMessage());
        }
    }

    /**
     * Tests handling of login attempts with invalid credentials. Expects a 401 Unauthorized response.
     */
    @Test
    void invalidCredentialsTest() {
        when(fakeAuthService.tryLogin("wrong@email.com", "wrongpass"))
                .thenReturn(null);
        String body = "{ \"email\": \"wrong@example.com\", \"password\": \"bad\" }";
        try {
            MockHttpExchange exchange = new MockHttpExchange("POST", "/login", body);
            handler.handle(exchange);
            assertEquals(401, exchange.getResponseCode());
            assertTrue(exchange.getResponseBodyAsString().contains("Invalid email or password"));
        } catch (Exception e) {
            fail("Exception thrown in login handler: " + e.getMessage());
        }
    }
}
