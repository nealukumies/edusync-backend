package server;

import com.sun.net.httpserver.HttpExchange;
import database.AssignmentDao;
import model.Assignment;
import model.Status;

import java.io.IOException;
import java.sql.Date;
import java.util.Map;

public class AssignmentHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        switch (method) {
            case "GET":
                handleGet(exchange);
                sendResponse(exchange, 200, "GET method not implemented yet");
                break;
            case "POST":
                handlePost(exchange);
                sendResponse(exchange, 200, "POST method not implemented yet");
                break;
            case "DELETE":
                handleDelete(exchange);
                sendResponse(exchange, 200, "DELETE method not implemented yet");
                break;
            case "PUT":
                handlePut(exchange);
                sendResponse(exchange, 200, "PUT method not implemented yet");
                break;
            default:
                sendResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        int assignmentId = getIdFromPath(exchange, 2);
        if (assignmentId == -1) return;

        AssignmentDao assignmentDao = new AssignmentDao();
        Assignment assignment = assignmentDao.getAssignmentById(assignmentId);
        if (assignment == null) {
            sendResponse(exchange, 404, "Assignment not found");
            return;
        }
        int studentId = assignment.getStudentId();
        if (!isAuthorized(exchange, studentId)) return;

        sendResponse(exchange, 200, assignment);
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap == null) { return; }

        int studentId = Integer.parseInt(requestMap.get("student_id"));
        if (!isAuthorized(exchange, studentId)) return;

        int courseId = Integer.parseInt(requestMap.get("course_id"));
        String title = requestMap.get("title");
        String description = requestMap.get("description");
        String deadline = requestMap.get("deadline");

        Date sqlDeadline = null;
        try {
            if (deadline != null) {
                sqlDeadline = Date.valueOf(deadline);
            }
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, Map.of("Error", "Invalid date format. Use YYYY-MM-DD"));
            return;
        }

        if (courseId > 0 || studentId > 0 || title == null || sqlDeadline == null) {
            sendResponse(exchange, 400, Map.of("Error", "Title, and deadline are required"));
            return;
        }

        AssignmentDao assignmentDao = new AssignmentDao();
        Assignment newAssignment = assignmentDao.insertAssignment(studentId, courseId, title, description, sqlDeadline);
        sendResponse(exchange, 201, newAssignment);
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        int assignmentId = getIdFromPath(exchange, 2);
        if (assignmentId == -1) return;
        AssignmentDao assignmentDao = new AssignmentDao();
        Assignment assignment = assignmentDao.getAssignmentById(assignmentId);
        if (assignment == null) {
            sendResponse(exchange, 404, "Assignment not found");
            return;
        }
        int assignmentStudentId = assignment.getStudentId();
        if (!isAuthorized(exchange, assignmentStudentId)) return;
        boolean success = assignmentDao.deleteAssignment(assignmentId);
        if (!success) {
            sendResponse(exchange, 500, Map.of("error", "Failed to delete assignment"));
            return;
        }
        sendResponse(exchange, 200, Map.of("message", "Assignment deleted successfully"));
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        int assignmentId = getIdFromPath(exchange, 2);
        if (assignmentId == -1) return;
        Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap == null) { return; }
        int studentId = Integer.parseInt(requestMap.get("student_id"));
        if (!isAuthorized(exchange, studentId)) return;

        int courseId = Integer.parseInt(requestMap.get("course_id"));
        String newTitle = requestMap.get("title");
        String newDescription = requestMap.get("description");
        String newDeadline = requestMap.get("deadline");
        String newStatus = requestMap.get("status");

        Date sqlDeadline = null;
        try {
            if (newDeadline != null) {
                sqlDeadline = Date.valueOf(newDeadline);
            }
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, Map.of("error", "Invalid date format. Use YYYY-MM-DD"));
            return;
        }

        AssignmentDao assignmentDao = new AssignmentDao();
        Assignment existingAssignment = assignmentDao.getAssignmentById(assignmentId);
        if (existingAssignment == null) {
            sendResponse(exchange, 404, "Assignment not found");
            return;
        }

        boolean updated = false;

        if (newTitle != null || newDescription != null || sqlDeadline != null) {
            assignmentDao.updateAssignment(assignmentId, newTitle, newDescription, sqlDeadline, courseId > 0 ? courseId : null);
            updated = true;
        }
        if (newStatus != null) {
            Status status = Status.fromDbValue(newStatus);
            if (status == null) {
                sendResponse(exchange, 400, Map.of("error", "Invalid status value"));
                return;
            }
            assignmentDao.setStatus(assignmentId, status);
            updated = true;
        }

        if (updated) {
            Assignment updatedAssignment = assignmentDao.getAssignmentById(assignmentId);
            sendResponse(exchange, 200, updatedAssignment);
        } else {
            sendResponse(exchange, 400, Map.of("error", "No valid fields to update"));
        }
    }
}
