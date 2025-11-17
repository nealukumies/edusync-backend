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

/**
 * This class handles HTTP requests related to assignments.
 */
public class AssignmentHandler extends BaseHandler {
    private static final String NO_ASSIGNMENTS_FOUND = "No assignments found";
    private static final String COURSE_ID_KEY = "course_id";
    private static final String TITLE_KEY = "title";
    private static final String DESCRIPTION_KEY = "description";
    private static final String DEADLINE_KEY = "deadline";

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
     *
     * @param exchange
     * @throws IOException
     */
    @Override
    protected void handleGet(HttpExchange exchange) throws IOException {
        final String[] pathParts = exchange.getRequestURI().getPath().split("/");

        if (pathParts.length == 4 && "students".equals(pathParts[2])) {
            final int studentId = getIdFromPath(exchange, 3);
            if (studentId == -1) {return;}
            if (!isAuthorized(exchange, studentId)) {return;}

            final List<Assignment> assignments = assignmentDao.getAssignments(studentId);
            if (assignments == null || assignments.isEmpty()) {
                sendResponse(exchange, 404, NO_ASSIGNMENTS_FOUND);
                return;
            }
            sendResponse(exchange, 200, assignments);
            return;
        }

        final int assignmentId = getIdFromPath(exchange, 2);
        if (assignmentId == -1) {return;}

        final Assignment assignment = assignmentDao.getAssignmentById(assignmentId);
        if (assignment == null) {
            sendResponse(exchange, 404, NO_ASSIGNMENTS_FOUND);
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
     *
     * @param exchange
     * @throws IOException
     */
    @Override
    protected void handlePost(HttpExchange exchange) throws IOException {
        final Map<String, String> requestMap = parseJsonBody(exchange);

        final int studentId = getIdFromHeader(exchange);

        final int courseId = Integer.parseInt(requestMap.get(COURSE_ID_KEY));
        final String title = requestMap.get(TITLE_KEY);
        final String description = requestMap.get(DESCRIPTION_KEY);
        final String deadline = requestMap.get(DEADLINE_KEY);

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
     *
     * @param exchange
     * @throws IOException
     */
    @Override
    protected void handleDelete(HttpExchange exchange) throws IOException {
        final int assignmentId = getIdFromPath(exchange, 2);
        if (assignmentId == -1) {return;}

        final Assignment assignment = assignmentDao.getAssignmentById(assignmentId);
        if (assignment == null) {
            sendResponse(exchange, 404, NO_ASSIGNMENTS_FOUND);
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
     *
     * @param exchange
     * @throws IOException
     */
    @Override
    protected void handlePut(HttpExchange exchange) throws IOException {
        final int assignmentId = getIdFromPath(exchange, 2);
        if (assignmentId == -1) {return;}

        final Map<String, String> requestMap = parseJsonBody(exchange);

        final Assignment existingAssignment = assignmentDao.getAssignmentById(assignmentId);
        if (existingAssignment == null) {
            sendResponse(exchange, 404, NO_ASSIGNMENTS_FOUND);
            return;
        }

        if (!isAuthorized(exchange, existingAssignment.getStudentId())) {return;}

        final Timestamp newDeadline = parseDeadline(requestMap, existingAssignment.getDeadline(), exchange);
        if (newDeadline == null) {return;}

        final Integer newCourseId = parseCourseId(requestMap, existingAssignment.getCourseId(), exchange);
        if (newCourseId == null) {return;}

        final boolean statusUpdated = updateStatusIfPresent(assignmentId, requestMap);

        final boolean fieldsUpdated = assignmentDao.updateAssignment(
                assignmentId,
                requestMap.getOrDefault(TITLE_KEY, existingAssignment.getTitle()),
                requestMap.getOrDefault(DESCRIPTION_KEY, existingAssignment.getDescription()),
                newDeadline,
                newCourseId
        );

        if (statusUpdated || fieldsUpdated) {
            sendResponse(exchange, 200, assignmentDao.getAssignmentById(assignmentId));
        } else {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "No fields were updated"));
        }
    }

    /**
     * Parses the deadline from the request map.
     *
     * @param requestMap The map containing request parameters.
     * @param existingDeadline The existing deadline to fall back on if not provided.
     * @param exchange The HttpExchange object for sending responses.
     * @return The parsed Timestamp deadline.
     * @throws IOException If an I/O error occurs.
     */
    private Timestamp parseDeadline(Map<String, String> requestMap, Timestamp existingDeadline, HttpExchange exchange) throws IOException {
        if (requestMap.get(DEADLINE_KEY) == null) return existingDeadline;
        try {
            return Timestamp.valueOf(requestMap.get(DEADLINE_KEY));
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid date format. Use YYYY-MM-DD"));
            return null;
        }
    }

    /**
     * Parses the course ID from the request map.
     *
     * @param requestMap The map containing request parameters.
     * @param existingCourseId The existing course ID to fall back on if not provided.
     * @param exchange The HttpExchange object for sending responses.
     * @return The parsed Integer course ID.
     * @throws IOException If an I/O error occurs.
     */
    private Integer parseCourseId(Map<String, String> requestMap, Integer existingCourseId, HttpExchange exchange) throws IOException {
        if (requestMap.get(COURSE_ID_KEY) == null) return existingCourseId;
        int parsedId;
        try {
            parsedId = Integer.parseInt(requestMap.get(COURSE_ID_KEY));
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid course_id"));
            return null;
        }

        return (parsedId > 0) ? parsedId : existingCourseId;
    }

    /**
     * Updates the status of the assignment if present in the request map.
     * @param assignmentId The ID of the assignment to update.
     * @param requestMap The map containing request parameters.
     * @return True if the status was updated, false otherwise.
     */
    private boolean updateStatusIfPresent(int assignmentId, Map<String, String> requestMap) {
        if (requestMap.get("status") == null) return false;
        final Status status = Status.fromDbValue(requestMap.get("status"));
        return assignmentDao.updateStatus(assignmentId, status);
    }

}
