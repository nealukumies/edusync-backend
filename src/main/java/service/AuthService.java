/**
 * This class handles user authentication, including login attempts and password hashing/verification.
 */
package service;

import database.StudentDao;
import model.Student;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    /**
     * Attempts to log in a user with the provided email and password.
     * Returns the Student object if successful, or null if authentication fails.
     * @param email
     * @param password
     * @return Student - the authenticated Student object, or null if authentication fails
     */
    public Student tryLogin(String email, String password) {
        StudentDao dao = new StudentDao();
        String storedHash = dao.getPasswordHash(email);

        if (storedHash != null && verifyPassword(password, storedHash)) {
            return dao.getStudent(email);
        } else {
            return null;
        }
    }

    /**
     * Verifies a plaintext password against a stored hashed password using BCrypt.
     * @param password
     * @param hashed
     * @return
     */
    public boolean verifyPassword(String password, String hashed) {
        return BCrypt.checkpw(password, hashed);
    }

}
