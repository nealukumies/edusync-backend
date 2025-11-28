package server;

import com.sun.net.httpserver.HttpExchange;
import database.AssignmentDao;
import database.CourseDao;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.Course;

import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.Map;

/**
 * This class handles HTTP requests related to course management.
 */
public class CourseHandler extends BaseHandler {
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "CourseDao is controller; no exposure risk"
    )
    private final CourseDao courseDao;
    private static final String ERROR_KEY = "error";
    private static final String COURSE_ERROR = "Course not found";
    private static final String END_DATE_KEY = "end_date";
    private static final String START_DATE_KEY = "start_date";
    private record Response(int status, Object body) {}

    /**
     * Constructor for CourseHandler. Data access object (DAO) is injected via constructor.
     *
     * @param courseDao The CourseDao instance for database operations.
     */
    public CourseHandler(CourseDao courseDao) {
        super();
        this.courseDao = courseDao;
    }

    /**
     * Handles GET requests for courses.
     * Supports fetching courses by student ID or course ID.
     * Authorization checks ensure students can only access their own courses.
     *
     * @param exchange The HttpExchange object containing request and response data.
     * @throws IOException Throws IOException if an I/O error occurs.
     */
    @Override
    protected void handleGet(HttpExchange exchange) throws IOException {
        final int status;
        final Object response;

        final String[] pathParts = getPathParts(exchange);

        final Response result;
        if ("students".equals(pathParts[2]) && pathParts.length == 4) {
            result = handleGetStudentCourses(exchange);
        } else {
            result = handleGetSingleCourse(exchange);
        }
        status = result.status();
        response = result.body();

        sendResponse(exchange, status, response);
    }

    /** Helper method to handle fetching a single course by its ID.
     *
     * @param exchange The HttpExchange object containing request and response data.
     * @return A Response object containing the status code and response body.
     * @throws IOException Throws IOException if an I/O error occurs.
     */
    private Response handleGetSingleCourse(HttpExchange exchange) throws IOException {
        final int courseId = getIdFromPath(exchange, 2);
        if (courseId == -1) {
            return new Response(400, Map.of(ERROR_KEY, "Invalid course ID"));
        }

        final Course course = courseDao.getCourseById(courseId);
        if (course == null) {
            return new Response(404, COURSE_ERROR);
        }

        final int studentId = course.getStudentId();
        if (!isAuthorized(exchange, studentId)) {
            return new Response(403, Map.of(ERROR_KEY, "Not authorized"));
        }

        return new Response(200, course);
    }

    /**
     * Helper method to handle fetching all courses for a specific student.
     * @param exchange The HttpExchange object containing request and response data.
     * @return A Response object containing the status code and response body.
     * @throws IOException Throws IOException if an I/O error occurs.
     */
    private Response handleGetStudentCourses(HttpExchange exchange) throws IOException {
        final int studentId = getIdFromPath(exchange, 3);
        if (studentId == -1) {
            return new Response(400, Map.of(ERROR_KEY, "Invalid student ID"));
        }

        if (!isAuthorized(exchange, studentId)) {
            return new Response(403, Map.of(ERROR_KEY, "Not authorized"));
        }

        final List<Course> courses = courseDao.getAllCourses(studentId);
        if (courses == null || courses.isEmpty()) {
            return new Response(404, "No courses found for this student");
        }

        return new Response(200, courses);
    }

    /**
     * Handles POST requests to add a new course.
     * Expects JSON body with course_name, start_date, and end_date.
     * Validates input and returns appropriate HTTP responses.
     *
     * @param exchange The HttpExchange object containing request and response data.
     * @throws IOException Throws IOException if an I/O error occurs.
     */
    @Override
    protected void handlePost(HttpExchange exchange) throws IOException {
        final Response result = handleCreateCourse(exchange);
        sendResponse(exchange, result.status(), result.body());
    }

    /**
     * Helper method to handle course creation logic.
     * @param exchange The HttpExchange object containing request and response data.
     * @return A Response object containing the status code and response body.
     * @throws IOException Throws IOException if an I/O error occurs.
     */
    private Response handleCreateCourse(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "POST")) {
            return new Response(405, Map.of(ERROR_KEY, "Method not allowed"));
        }

        final Map<String, String> requestMap = parseJsonBody(exchange);
        final int studentId = getIdFromHeader(exchange);
        if (studentId == -1) {
            return new Response(400, Map.of(ERROR_KEY, "Invalid or missing student_id header"));
        }

        final String courseName = requestMap.get("course_name");
        final String startDate = requestMap.get(START_DATE_KEY);
        final String endDate   = requestMap.get(END_DATE_KEY);
        if (courseName == null || startDate == null || endDate == null) {
            return new Response(400, Map.of(
                    ERROR_KEY, "Course name, start date, and end date are required"
            ));
        }
        final Date sqlStartDate;
        final Date sqlEndDate;
        try {
            sqlStartDate = Date.valueOf(startDate);
            sqlEndDate   = Date.valueOf(endDate);
        } catch (IllegalArgumentException e) {
            return new Response(400, Map.of(
                    ERROR_KEY, "Invalid date format. Use YYYY-MM-DD"
            ));
        }

        final Course course = courseDao.addCourse(studentId, courseName, sqlStartDate, sqlEndDate);
        if (course == null) {
            return new Response(500, Map.of(ERROR_KEY, "Failed to add course"));
        }

        return new Response(201, course);
    }


    /**
     * Handles DELETE requests to remove a course.
     * Validates that the course exists and that the requesting student is authorized to delete it.
     * Returns appropriate HTTP responses based on the outcome.
     * Authorization is checked via the student_id header.
     *
     * @param exchange The HttpExchange object containing request and response data.
     * @throws IOException Throws IOException if an I/O error occurs.
     */
    @Override
    protected void handleDelete(HttpExchange exchange) throws IOException {
        final int studentId = getIdFromHeader(exchange);
        if (studentId == -1) {return;}
        final int courseId = getIdFromPath(exchange, 2);
        if (courseId == -1) {return;}

        final Course course = courseDao.getCourseById(courseId);
        if (course == null) {
            sendResponse(exchange, 404, Map.of(ERROR_KEY, COURSE_ERROR));
            return;
        }
        if (course.getStudentId() != studentId) {
            sendResponse(exchange, 403, Map.of(ERROR_KEY, "Forbidden: You can only delete your own courses"));
            return;
        }
        AssignmentDao assignmentDao = new AssignmentDao();
        assignmentDao.deleteAssignmentsByCourseId(courseId);
        final boolean deleted = courseDao.deleteCourse(courseId);
        if (!deleted) {
            sendResponse(exchange, 500, Map.of(ERROR_KEY, "Failed to delete course"));
            return;
        }
        sendResponse(exchange, 200, Map.of("message", "Course deleted successfully"));
    }

    /**
     * Handles PUT requests to update an existing course.
     * Expects JSON body with fields to update: course_name, start_date, end_date.
     * Validates input and checks authorization before updating.
     * Returns appropriate HTTP responses based on the outcome.
     * Authorization is checked via the student_id header.
     *
     * @param exchange The HttpExchange object containing request and response data.
     * @throws IOException Throws IOException if an I/O error occurs.
     */
    @Override
    protected void handlePut(HttpExchange exchange) throws IOException {
        final int courseId = getIdFromPath(exchange, 2);
        if (courseId == -1) {return;}

        final Map<String, String> requestMap = parseJsonBody(exchange);

        final Course existingCourse = courseDao.getCourseById(courseId);
        if (existingCourse == null) {
            sendResponse(exchange, 404, Map.of(ERROR_KEY, COURSE_ERROR));
            return;
        }

        final int studentId = existingCourse.getStudentId();
        if (!isAuthorized(exchange, studentId)) {return;}

        final String courseName = requestMap.getOrDefault("course_name", existingCourse.getCourseName());

        Date sqlStartDate = existingCourse.getStartDate() != null ?
                new Date(existingCourse.getStartDate().getTime()) : null;
        Date sqlEndDate = existingCourse.getEndDate() != null ?
                new Date(existingCourse.getEndDate().getTime()) : null;
        try {
            if (requestMap.get(START_DATE_KEY) != null) {
                sqlStartDate = Date.valueOf(requestMap.get(START_DATE_KEY));
            }
            if (requestMap.get(END_DATE_KEY) != null) {
                sqlEndDate = Date.valueOf(requestMap.get(END_DATE_KEY));
            }
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid date format. Use YYYY-MM-DD"));
            return;
        }

        final boolean updatedCourse = courseDao.updateCourse(courseId, courseName, sqlStartDate, sqlEndDate);
        if (updatedCourse) {
            final Course updated = courseDao.getCourseById(courseId);
            sendResponse(exchange, 200, updated);
        } else {
            sendResponse(exchange, 500, Map.of(ERROR_KEY, "Failed to update course"));
        }
    }
}
