/**
 * This class handles HTTP requests related to assignments.
 */

package server;

import com.sun.net.httpserver.HttpExchange;
import database.AssignmentDao;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.Assignment;
import model.Status;

import java.io.IOException;
import java.util.List;
import java.sql.Timestamp;
import java.util.Map;

public class AssignmentHandler extends BaseHandler {
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "AssignmentDao is controller; no exposure risk"
    )
    private final AssignmentDao assignmentDao;
    private static final String ERROR_KEY = "error";


    /**
     * Constructor for AssignmentHandler.
     *
     * @param assignmentDao The AssignmentDao instance for database operations.
     */
    public AssignmentHandler(AssignmentDao assignmentDao) {
        super();
        this.assignmentDao = assignmentDao;
    }

    /**
     * Handles GET requests for assignments.
     * Supports fetching assignments by student ID or assignment ID.
     * @param exchange
     * @throws IOException
     */
    protected void handleGet(HttpExchange exchange) throws IOException {
        String[] pathParts = exchange.getRequestURI().getPath().split("/");

        if (pathParts.length == 4 && "students".equals(pathParts[2])) {
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

    /**
     * Handles POST requests to create a new assignment.
     * Expects a JSON body with course_id, title, description, and deadline.
     * Authorization is checked via the student_id header.
     * @param exchange
     * @throws IOException
     */
    protected void handlePost(HttpExchange exchange) throws IOException {
        Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap == null) { return; }

        int studentId = getIdFromHeader(exchange);

        int courseId = Integer.parseInt(requestMap.get("course_id"));
        String title = requestMap.get("title");
        String description = requestMap.get("description");
        String deadline = requestMap.get("deadline");

        Timestamp sqlDeadline = null;
        try {
            if (deadline != null) {
                sqlDeadline = Timestamp.valueOf(deadline);
            }
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid date format. Use YYYY-MM-DD"));
            return;
        }

        if (courseId <= 0 || studentId <= 0 || title == null || sqlDeadline == null) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Title, and deadline are required"));
            return;
        }

        Assignment newAssignment = assignmentDao.insertAssignment(studentId, courseId, title, description, sqlDeadline);
        sendResponse(exchange, 201, newAssignment);
    }

    /**
     * Handles DELETE requests to remove an assignment by its ID.
     * Expects the assignment ID in the URL path as /assignments/{assignmentId}.
     * Authorization is checked to ensure the requester owns the assignment.
     * @param exchange
     * @throws IOException
     */
    protected void handleDelete(HttpExchange exchange) throws IOException {
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
            sendResponse(exchange, 500, Map.of(ERROR_KEY, "Failed to delete assignment"));
            return;
        }
        sendResponse(exchange, 200, Map.of("message", "Assignment deleted successfully"));
    }

    /**
     * Handles PUT requests to update an existing assignment.
     * Expects the assignment ID in the URL path as /assignments/{assignmentId}.
     * The request body can contain any of the fields: title, description, deadline, course_id, status.
     * Only the fields provided in the request body will be updated.
     * Authorization is checked to ensure the requester owns the assignment.
     * @param exchange
     * @throws IOException
     */
    protected void handlePut(HttpExchange exchange) throws IOException {
        int assignmentId = getIdFromPath(exchange, 2);
        if (assignmentId == -1) return;
        Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap == null) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "invalid json"));
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
        Timestamp newDeadline = existingAssignment.getDeadline() != null
                ? new Timestamp(existingAssignment.getDeadline().getTime())
                : null;
        if (requestMap.get("deadline") != null) {
            try {
                newDeadline = java.sql.Timestamp.valueOf(requestMap.get("deadline"));
            } catch (IllegalArgumentException e) {
                sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid date format. Use YYYY-MM-DD"));
                return;
            }
        }

        Integer newCourseId = existingAssignment.getCourseId();
        if (requestMap.get("course_id") != null) {
            try {
                int parsedCourseId = Integer.parseInt(requestMap.get("course_id"));
                if (parsedCourseId > 0) newCourseId = parsedCourseId;
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid course_id"));
                return;
            }
        }

        boolean updated = false;
        if (requestMap.get("status") != null) {
            Status status = Status.fromDbValue(requestMap.get("status"));
            boolean statusUpdated = assignmentDao.updateStatus(assignmentId, status);
            updated = updated || statusUpdated;
        }

        boolean fieldsUpdated = assignmentDao.updateAssignment(assignmentId, newTitle, newDescription, newDeadline, newCourseId);
        updated = updated || fieldsUpdated;

        if (updated) {
            Assignment updatedAssignment = assignmentDao.getAssignmentById(assignmentId);
            sendResponse(exchange, 200, updatedAssignment);
        } else {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "No fields were updated"));
        }
    }

}
