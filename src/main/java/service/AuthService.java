package service;

import database.StudentDao;
import model.Student;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    public Student tryLogin(String email, String password) {
        StudentDao dao = new StudentDao();
        String storedHash = dao.getPasswordHash(email);

        if (storedHash != null && verifyPassword(password, storedHash)) {
            return dao.getStudent(email);
        } else {
            return null;
        }
    }

    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public boolean verifyPassword(String password, String hashed) {
        return BCrypt.checkpw(password, hashed);
    }

}
