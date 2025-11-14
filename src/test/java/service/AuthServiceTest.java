package service;

import database.StudentDao;
import model.Student;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void testTryLogin() {
        StudentDao mockDao = mock(StudentDao.class);
        String email = "test@example.com";
        String password = "secret";
        String storedHash = BCrypt.hashpw(password, BCrypt.gensalt(12));

        when(mockDao.getPasswordHash(email)).thenReturn(storedHash);
        Student student = new Student(1, email, "Test User", "user"); // assuming ID is 0
        when(mockDao.getStudent(email)).thenReturn(student);
        AuthService authService = new AuthService(mockDao);
        Student result = authService.tryLogin(email, password);

        assertNotNull(result);
    }

    @Test
    void testVerifyPassword() {
        AuthService authService = new AuthService(null);
        String password = "myPassword";
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));
        boolean result = authService.verifyPassword(password, hashed);
        assertTrue(result, "The password should be verified successfully against the hash");
    }

    @Test
    void testUnsuccessfulLogin() {
        StudentDao mockDao = mock(StudentDao.class);
        String email = "test@example.com";
        String password = "wrongPassword";
        String storedHash = BCrypt.hashpw("correctPassword", BCrypt.gensalt(12));
        when(mockDao.getPasswordHash(email)).thenReturn(storedHash);
        AuthService authService = new AuthService(mockDao);
        Student result = authService.tryLogin(email, password);
        assertNull(result, "Login should fail and return null for incorrect password");
    }

    @Test
    void testVerifyWrongPassword() {
        AuthService authService = new AuthService(null);
        String password = "myPassword";
        String hashed = BCrypt.hashpw("differentPassword", BCrypt.gensalt(12));
        boolean result = authService.verifyPassword(password, hashed);
        assertFalse(result, "The password verification should fail for incorrect password");
    }
}