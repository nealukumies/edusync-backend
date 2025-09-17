package server;

import com.sun.net.httpserver.HttpExchange;
import database.StudentDao;
import model.Student;

import java.io.IOException;
import java.util.Map;

public class StudentHandler extends BaseHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        switch (method) {
            case "GET":
                handleGet(exchange);
                break;
            case "POST":
                handlePost(exchange);
                break;
            case "DELETE":
                handleDelete(exchange);
                break;
            case "PUT":
                handlePut(exchange);
                break;
            default:
                sendResponse(exchange, 405, Map.of("error", "Method Not Allowed"));

        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {

        int studentId = getIdFromPath(exchange, 2);
        if (studentId == -1) return;
        if (!isAuthorized(exchange, studentId)) return;

        StudentDao studentDao = new StudentDao();
        Student student = studentDao.getStudentById(studentId);
        if (student == null) {
            sendResponse(exchange, 404, "Student not found");
            return;
        }
        sendResponse(exchange, 200, student);
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "POST")) { return; }

        Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap == null) { return; }

        String name = requestMap.get("name");
        String email = requestMap.get("email");
        String password = requestMap.get("password");

        if (name == null || email == null || password == null) {
            sendResponse(exchange, 400, Map.of("error", "Name, email, password, and role are required"));
            return;
        }

        StudentDao studentDao = new StudentDao();
        if (studentDao.getStudent(email) != null) {
            sendResponse(exchange, 409, Map.of("error", "Email already in use"));
            return;
        }

        Student newStudent = studentDao.addStudent(name, email, password);
        sendResponse(exchange, 201, newStudent);
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        int studentId = getIdFromPath(exchange, 2);
        if (studentId == -1) return;
        String role = getRoleFromHeader(exchange);
        if (role == null) return;
        if (!role.equals("admin")) {
            sendResponse(exchange, 403, Map.of("error", "Forbidden: Admins only"));
            return;
        }

        StudentDao studentDao = new StudentDao();
        boolean deleted = studentDao.deleteStudent(studentId);
        if (!deleted) {
            sendResponse(exchange, 404, Map.of("error", "Student not found"));
            return;
        }
        sendResponse(exchange, 200, Map.of("message", "Student deleted successfully"));
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        int studentId = getIdFromPath(exchange, 2);
        if (studentId == -1) return;
        if (!isAuthorized(exchange, studentId)) return;

        Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap == null) { return; }

        String newName = requestMap.get("name");
        String newEmail = requestMap.get("email");

        if (newName == null && newEmail == null) {
            sendResponse(exchange, 400, Map.of("error", "At least one of name or email must be provided"));
            return;
        }

        if (newEmail != null) {
            StudentDao studentDao = new StudentDao();
            Student existingStudent = studentDao.getStudent(newEmail);
            if (existingStudent != null && existingStudent.getId() != studentId) {
                sendResponse(exchange, 409, Map.of("error", "Email already in use by another student"));
                return;
            }
        }

        StudentDao studentDao = new StudentDao();
        boolean updated = false;
        if (newName != null) {
            updated = studentDao.updateStudentName(studentId, newName);
        }
        if (newEmail != null) {
            updated = studentDao.updateStudentEmail(studentId, newEmail) || updated;
        }
        if (!updated) {
            sendResponse(exchange, 404, Map.of("error", "Student not found or no changes made"));
            return;
        }

        Student updatedStudent = studentDao.getStudentById(studentId);
        sendResponse(exchange, 200, updatedStudent);
    }
}

