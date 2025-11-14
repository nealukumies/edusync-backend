package server;

import model.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.testutils.MockHttpExchange;
import service.AuthService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginHandlerTest{
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
    void wrongMethodTest() throws Exception {
        MockHttpExchange exchange = new MockHttpExchange("GET", "/login", "");
        handler.handle(exchange);
        assertEquals(405, exchange.getResponseCode(), "Expected 405 Method Not Allowed");
   }

    /**
     * Tests handling of requests with missing email or password fields. Expects a 400 Bad Request response.
     */
    @Test
    void missingDetailsTest() throws Exception {
        String body = "{}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/login", body);
        handler.handle(exchange);
        assertEquals(400, exchange.getResponseCode(), "Expected 400 Bad Request");
    }

    /**
     * Tests a successful login attempt with valid credentials. Expects a 200 OK response with student details.
     */
    @Test
    void successfulLoginTest() throws Exception {
        when(fakeAuthService.tryLogin("test@example.com", "password"))
                .thenReturn(new Student(1, "Test", "test@example.com", "user"));
        String body = "{ \"email\": \"test@example.com\", \"password\": \"password\" }";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/login", body);
        handler.handle(exchange);
        assertEquals(200, exchange.getResponseCode(), "Expected 200 OK");

    }

    /**
     * Tests handling of login attempts with invalid credentials. Expects a 401 Unauthorized response.
     */
    @Test
    void invalidCredentialsTest() throws Exception {
        when(fakeAuthService.tryLogin("wrong@email.com", "wrongpass"))
                .thenReturn(null);
        String body = "{ \"email\": \"wrong@example.com\", \"password\": \"bad\" }";

        MockHttpExchange exchange = new MockHttpExchange("POST", "/login", body);
        handler.handle(exchange);
        assertEquals(401, exchange.getResponseCode(), "Expected 401 Unauthorized");
    }
}
