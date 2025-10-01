/**
 * This class handles HTTP requests related to student operations such as
 * creating, retrieving, updating, and deleting student records.
 */
package server;

import com.sun.net.httpserver.HttpExchange;
import database.StudentDao;
import model.Student;

import java.io.IOException;
import java.util.Map;

public class StudentHandler extends BaseHandler {
    private StudentDao studentDao;

    /**
     * Constructor for StudentHandler. Data access object (DAO) is injected via constructor.
     * @param studentDao
     */
    public StudentHandler(StudentDao studentDao) {
        this.studentDao = studentDao;
    }

    /**
     * Handles incoming HTTP requests and routes them to the appropriate method
     * based on the HTTP method (GET, POST, DELETE, PUT).
     * @param exchange the exchange containing the request from the
     *                 client and used to send the response
     * @throws IOException
     */
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

    /**
     * Handles GET requests for students.
     * Supports fetching students by student ID.
     * Authorization checks ensure students can only access their own records.
     * @param exchange
     * @throws IOException
     */
    private void handleGet(HttpExchange exchange) throws IOException {

        int studentId = getIdFromPath(exchange, 2);
        if (studentId == -1) return;
        if (!isAuthorized(exchange, studentId)) return;

        Student student = studentDao.getStudentById(studentId);
        if (student == null) {
            sendResponse(exchange, 404, "Student not found");
            return;
        }
        sendResponse(exchange, 200, student);
    }

    /**
     * Handles POST requests to create a new student.
     * Expects a JSON body with name, email, and password.
     * Validates input and checks for email uniqueness.
     * Responds with the created student object or appropriate error messages.
     * @param exchange
     * @throws IOException
     */
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

        if (studentDao.getStudent(email) != null) {
            sendResponse(exchange, 409, Map.of("error", "Email already in use"));
            return;
        }

        Student newStudent = studentDao.addStudent(name, email, password);
        sendResponse(exchange, 201, newStudent);
    }

    /**
     * Handles DELETE requests to remove a student by ID.
     * Authorization checks ensure students can only delete their own records.
     * Responds with success message or appropriate error messages.
     * @param exchange
     * @throws IOException
     */
    private void handleDelete(HttpExchange exchange) throws IOException {
        int studentId = getIdFromPath(exchange, 2);
        if (studentId == -1) return;
        if (!isAuthorized(exchange, studentId)) return;

        boolean deleted = studentDao.deleteStudent(studentId);
        if (!deleted) {
            sendResponse(exchange, 404, Map.of("error", "Student not found"));
            return;
        }
        sendResponse(exchange, 200, Map.of("message", "Student deleted successfully"));
    }

    /**
     * Handles PUT requests to update an existing student's name and/or email.
     * Expects a JSON body with fields to update: name and/or email.
     * Validates input and checks for email uniqueness.
     * Authorization checks ensure students can only update their own records.
     * Responds with the updated student object or appropriate error messages.
     * @param exchange
     * @throws IOException
     */
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
            Student existingStudent = studentDao.getStudent(newEmail);
            if (existingStudent != null && existingStudent.getId() != studentId) {
                sendResponse(exchange, 409, Map.of("error", "Email already in use by another student"));
                return;
            }
        }

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

