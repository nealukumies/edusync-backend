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
        final String[] pathParts = exchange.getRequestURI().getPath().split("/");

        if (pathParts.length == 4 && "students".equals(pathParts[2])) {
            final int studentId = getIdFromPath(exchange, 3);
            if (studentId == -1) {return;}
            if (!isAuthorized(exchange, studentId)) {return;}

            final List<Assignment> assignments = assignmentDao.getAssignments(studentId);
            if (assignments == null || assignments.isEmpty()) {
                sendResponse(exchange, 404, "No assignments found");
                return;
            }
            sendResponse(exchange, 200, assignments);
            return;
        }

        final int assignmentId = getIdFromPath(exchange, 2);
        if (assignmentId == -1) {return;}

        final Assignment assignment = assignmentDao.getAssignmentById(assignmentId);
        if (assignment == null) {
            sendResponse(exchange, 404, "Assignment not found");
            return;
        }
        final int studentId = assignment.getStudentId();
        if (!isAuthorized(exchange, studentId)) {return;}

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
        final Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap == null) { return; }

        final int studentId = getIdFromHeader(exchange);

        final int courseId = Integer.parseInt(requestMap.get("course_id"));
        final String title = requestMap.get("title");
        final String description = requestMap.get("description");
        final String deadline = requestMap.get("deadline");

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

        final Assignment newAssignment = assignmentDao.insertAssignment(studentId, courseId, title, description, sqlDeadline);
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
        final int assignmentId = getIdFromPath(exchange, 2);
        if (assignmentId == -1) {return;}

        final Assignment assignment = assignmentDao.getAssignmentById(assignmentId);
        if (assignment == null) {
            sendResponse(exchange, 404, "Assignment not found");
            return;
        }
        final int assignmentStudentId = assignment.getStudentId();
        if (!isAuthorized(exchange, assignmentStudentId)) {return;}
        final boolean success = assignmentDao.deleteAssignment(assignmentId);
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
        final int assignmentId = getIdFromPath(exchange, 2);
        if (assignmentId == -1) return;

        final Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap == null) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "invalid json"));
            return;
        }

        final Assignment existingAssignment = assignmentDao.getAssignmentById(assignmentId);
        if (existingAssignment == null) {
            sendResponse(exchange, 404, "Assignment not found");
            return;
        }

        if (!isAuthorized(exchange, existingAssignment.getStudentId())) return;

        final Timestamp newDeadline = parseDeadline(requestMap, existingAssignment.getDeadline(), exchange);
        if (newDeadline == null) return;

        final Integer newCourseId = parseCourseId(requestMap, existingAssignment.getCourseId(), exchange);
        if (newCourseId == null) return;

        final boolean statusUpdated = updateStatusIfPresent(assignmentId, requestMap);

        final boolean fieldsUpdated = assignmentDao.updateAssignment(
                assignmentId,
                requestMap.getOrDefault("title", existingAssignment.getTitle()),
                requestMap.getOrDefault("description", existingAssignment.getDescription()),
                newDeadline,
                newCourseId
        );

        if (statusUpdated || fieldsUpdated) {
            sendResponse(exchange, 200, assignmentDao.getAssignmentById(assignmentId));
        } else {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "No fields were updated"));
        }
    }

    private Timestamp parseDeadline(Map<String, String> requestMap, Timestamp existingDeadline, HttpExchange exchange) throws IOException {
        if (requestMap.get("deadline") == null) return existingDeadline;
        try {
            return Timestamp.valueOf(requestMap.get("deadline"));
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid date format. Use YYYY-MM-DD"));
            return null;
        }
    }

    private Integer parseCourseId(Map<String, String> requestMap, Integer existingCourseId, HttpExchange exchange) throws IOException {
        if (requestMap.get("course_id") == null) return existingCourseId;
        try {
            int parsedId = Integer.parseInt(requestMap.get("course_id"));
            return parsedId > 0 ? parsedId : existingCourseId;
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid course_id"));
            return null;
        }
    }

    private boolean updateStatusIfPresent(int assignmentId, Map<String, String> requestMap) {
        if (requestMap.get("status") == null) return false;
        final Status status = Status.fromDbValue(requestMap.get("status"));
        return assignmentDao.updateStatus(assignmentId, status);
    }

}
