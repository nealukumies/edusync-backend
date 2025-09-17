package service;

import database.StudentDao;
import model.Student;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    public int tryLogin(String email, String password) {
        StudentDao dao = new StudentDao();
        String storedHash = dao.getPasswordHash(email);

        if (storedHash != null && verifyPassword(password, storedHash)) {
            return dao.getStudent(email).getId();
        } else {
            return -1;
        }
    }

    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public boolean verifyPassword(String password, String hashed) {
        return BCrypt.checkpw(password, hashed);
    }

}
