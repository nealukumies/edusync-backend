package model;

import org.junit.jupiter.api.Test;
import service.AuthService;

import static org.junit.jupiter.api.Assertions.*;

class LoginTest {

    private AuthService authService = new AuthService();

    @Test
    void TryLoginTest() {

        Student student  = authService.tryLogin("pellen@maili.fi", "salasana");
        assertTrue(student != null, "Login should be succesfull for correct credentials");
    }

    @Test
    void TryLoginWrongPasswordTest() {
        assertEquals(null, authService.tryLogin("pellen@maili.fi", "wrongpassword"));
    }
}