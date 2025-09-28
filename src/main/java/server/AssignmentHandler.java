package server;

import com.sun.net.httpserver.HttpExchange;
import database.AssignmentDao;
import model.Assignment;
import model.Status;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AssignmentHandler extends BaseHandler {
    private AssignmentDao assignmentDao;

    public AssignmentHandler(AssignmentDao assignmentDao) {
        this.assignmentDao = assignmentDao;
    }
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
        String[] pathParts = exchange.getRequestURI().getPath().split("/");

        // Handle /assignments/students/{studentId}
        if (pathParts.length == 4 && pathParts[2].equals("students")) {
            int studentId = getIdFromPath(exchange, 3);
            if (studentId == -1) return;
            if (!isAuthorized(exchange, studentId)) return;

            List<Assignment> assignments = assignmentDao.getAssignments(studentId);
            if (assignments == null || assignments.isEmpty()) {
                sendResponse(exchange, 404, "No assignments found");
                return;
            }
            sendResponse(exchange, 200, assignments);
            return;
        }

        // Handle /assignments/{assignmentId}
        int assignmentId = getIdFromPath(exchange, 2);
        if (assignmentId == -1) return;

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

        int studentId = getIdFromHeader(exchange);

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

        if (courseId <= 0 || studentId <= 0 || title == null || sqlDeadline == null) {
            sendResponse(exchange, 400, Map.of("Error", "Title, and deadline are required"));
            return;
        }

        Assignment newAssignment = assignmentDao.insertAssignment(studentId, courseId, title, description, sqlDeadline);
        sendResponse(exchange, 201, newAssignment);
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        int assignmentId = getIdFromPath(exchange, 2);
        if (assignmentId == -1) return;

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
        if (requestMap == null) {
            sendResponse(exchange, 400, Map.of("error", "invalid json"));
            return;
        }

        Assignment existingAssignment = assignmentDao.getAssignmentById(assignmentId);
        if (existingAssignment == null) {
            sendResponse(exchange, 404, "Assignment not found");
            return;
        }

        int studentId = existingAssignment.getStudentId();
        if (!isAuthorized(exchange, studentId)) return;

        String newTitle = requestMap.getOrDefault("title", existingAssignment.getTitle());
        String newDescription = requestMap.getOrDefault("description", existingAssignment.getDescription());
        java.sql.Date newDeadline = existingAssignment.getDeadline() != null
                ? new java.sql.Date(existingAssignment.getDeadline().getTime())
                : null;
        if (requestMap.get("deadline") != null) {
            try {
                newDeadline = java.sql.Date.valueOf(requestMap.get("deadline"));
            } catch (IllegalArgumentException e) {
                sendResponse(exchange, 400, Map.of("error", "Invalid date format. Use YYYY-MM-DD"));
                return;
            }
        }

        Integer newCourseId = existingAssignment.getCourseId();
        if (requestMap.get("course_id") != null) {
            try {
                int parsedCourseId = Integer.parseInt(requestMap.get("course_id"));
                if (parsedCourseId > 0) newCourseId = parsedCourseId;
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, Map.of("error", "Invalid course_id"));
                return;
            }
        }

        boolean updated = false;
        if (requestMap.get("status") != null) {
            Status status = Status.fromDbValue(requestMap.get("status"));
            if (status == null) {
                sendResponse(exchange, 400, Map.of("error", "Invalid status value"));
                return;
            }
            boolean statusUpdated = assignmentDao.setStatus(assignmentId, status);
            updated = updated || statusUpdated;
        }

        boolean fieldsUpdated = assignmentDao.updateAssignment(assignmentId, newTitle, newDescription, newDeadline, newCourseId);
        updated = updated || fieldsUpdated;

        if (updated) {
            Assignment updatedAssignment = assignmentDao.getAssignmentById(assignmentId);
            sendResponse(exchange, 200, updatedAssignment);
        } else {
            sendResponse(exchange, 400, Map.of("error", "No fields were updated"));
        }
    }

}
