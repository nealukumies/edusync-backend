package server;

import com.sun.net.httpserver.HttpExchange;
import database.StudentDao;
import model.Student;

import java.io.IOException;
import java.util.Map;

public class StudentHandler extends BaseHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "GET")) return;

        int studentId = getIdFromPath(exchange, 2);
        if (studentId == -1) return;

        String role = getRoleFromHeader(exchange);
        if (role == null) return;

        int headerId = getIdFromHeader(exchange);
        if (headerId == -1) return;
        if (role.equals("user") && studentId != headerId) {
            sendResponse(exchange, 403, Map.of("error", "Forbidden: Insufficient permissions"));
            return;
        }

        StudentDao studentDao = new StudentDao();
        Student student = studentDao.getStudentById(studentId);
        if (student == null) {
            sendResponse(exchange, 404, "Student not found");
            return;
        }
        sendResponse(exchange, 200, student);
    }
}

