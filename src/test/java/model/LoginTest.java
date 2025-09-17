package model;

import org.junit.jupiter.api.Test;

import service.AuthService.tryLogin;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginTest {

    @Test
    void TryWorkingEmailToLogin() {
        assertEquals(32, tryLogin("Katti.Matikainen@katti.org"));
    }

    @Test
    void TryInvalidEmailToLogin() {
        assertEquals(-1, tryLogin("Matti.Katikainen@katti.org"));
    }
}