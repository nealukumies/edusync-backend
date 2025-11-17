package service;

import database.StudentDao;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.Student;
import org.mindrot.jbcrypt.BCrypt;

/**
 * This class handles user authentication, including login attempts and password hashing/verification.
 */
public class AuthService {
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "StudentDao is controller; no exposure risk"
    )
    private final StudentDao dao;

    /**
     * Constructor for AuthService. Initializes the StudentDao instance.
     */
    public AuthService(StudentDao studentDao) {
        this.dao = studentDao;
    }

    /**
     * Attempts to log in a user with the provided email and password.
     * Returns the Student object if successful, or null if authentication fails.
     *
     * @param email The email of the student attempting to log in
     * @param password The plaintext password provided by the student
     * @return Student - the authenticated Student object, or null if authentication fails
     */
    public Student tryLogin(String email, String password) {
        final String storedHash = dao.getPasswordHash(email);
        if (storedHash != null && verifyPassword(password, storedHash)) {
            return dao.getStudent(email);
        } else {
            return null;
        }
    }

    /**
     * Verifies a plaintext password against a stored hashed password using BCrypt.
     *
     * @param password The plaintext password to verify
     * @param hashed The stored hashed password
     * @return boolean - true if the password matches the hash, false otherwise
     */
    public boolean verifyPassword(String password, String hashed) {
        return BCrypt.checkpw(password, hashed);
    }

}
