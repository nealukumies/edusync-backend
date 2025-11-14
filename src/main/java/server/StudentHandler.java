/**
 * This class handles HTTP requests related to student operations such as
 * creating, retrieving, updating, and deleting student records.
 */
package server;

import com.sun.net.httpserver.HttpExchange;
import database.StudentDao;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.Student;

import java.io.IOException;
import java.util.Map;

public class StudentHandler extends BaseHandler {
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "StudentDao is controller; no exposure risk"
    )
    private final StudentDao studentDao;
    private static final String ERROR_KEY = "error";

    /**
     * Constructor for StudentHandler. Data access object (DAO) is injected via constructor.
     * @param studentDao
     */
    public StudentHandler(StudentDao studentDao) {
        super();
        this.studentDao = studentDao;
    }

    /**
     * Handles GET requests for students.
     * Supports fetching students by student ID.
     * Authorization checks ensure students can only access their own records.
     * @param exchange
     * @throws IOException
     */
    protected void handleGet(HttpExchange exchange) throws IOException {

        final int studentId = getIdFromPath(exchange, 2);
        if (studentId == -1) {return;}
        if (!isAuthorized(exchange, studentId)) {return;}

        final Student student = studentDao.getStudentById(studentId);
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
    protected void handlePost(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "POST")) { return; }

        final Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap == null) { return; }

        final String name = requestMap.get("name");
        final String email = requestMap.get("email");
        final String password = requestMap.get("password");

        if (name == null || email == null || password == null) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Name, email, password, and role are required"));
            return;
        }

        if (studentDao.getStudent(email) != null) {
            sendResponse(exchange, 409, Map.of(ERROR_KEY, "Email already in use"));
            return;
        }

        final Student newStudent = studentDao.addStudent(name, email, password);
        sendResponse(exchange, 201, newStudent);
    }

    /**
     * Handles DELETE requests to remove a student by ID.
     * Authorization checks ensure students can only delete their own records.
     * Responds with success message or appropriate error messages.
     * @param exchange
     * @throws IOException
     */
    protected void handleDelete(HttpExchange exchange) throws IOException {
        final int studentId = getIdFromPath(exchange, 2);
        if (studentId == -1) {return;}
        if (!isAuthorized(exchange, studentId)) {return;}

        final boolean deleted = studentDao.deleteStudent(studentId);
        if (!deleted) {
            sendResponse(exchange, 404, Map.of(ERROR_KEY, "Student not found"));
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
    protected void handlePut(HttpExchange exchange) throws IOException {
        final int studentId = getIdFromPath(exchange, 2);
        if (studentId == -1) {return;}
        if (!isAuthorized(exchange, studentId)) {return;}

        final Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap == null) { return; }

        final String newName = requestMap.get("name");
        final String newEmail = requestMap.get("email");

        if (newName == null && newEmail == null) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "At least one of name or email must be provided"));
            return;
        }

        if (newEmail != null) {
            final Student existingStudent = studentDao.getStudent(newEmail);
            if (existingStudent != null && existingStudent.getId() != studentId) {
                sendResponse(exchange, 409, Map.of(ERROR_KEY, "Email already in use by another student"));
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
            sendResponse(exchange, 404, Map.of(ERROR_KEY, "Student not found or no changes made"));
            return;
        }

        final Student updatedStudent = studentDao.getStudentById(studentId);
        sendResponse(exchange, 200, updatedStudent);
    }
}

