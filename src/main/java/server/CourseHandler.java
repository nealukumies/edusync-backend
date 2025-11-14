/**
 * This class handles HTTP requests related to course management.
 */
package server;

import com.sun.net.httpserver.HttpExchange;
import database.CourseDao;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.Course;

import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.Map;

public class CourseHandler extends BaseHandler {
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "CourseDao is controller; no exposure risk"
    )
    private final CourseDao courseDao;

    /**
     * Constructor for CourseHandler. Data access object (DAO) is injected via constructor.
     * @param courseDao
     */
    public CourseHandler(CourseDao courseDao) {
        this.courseDao = courseDao;
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
     * Handles GET requests for courses.
     * Supports fetching courses by student ID or course ID.
     * Authorization checks ensure students can only access their own courses.
     * @param exchange
     * @throws IOException
     */
    private void handleGet(HttpExchange exchange) throws IOException {
        String[] pathParts = exchange.getRequestURI().getPath().split("/");

        // Handle /courses/students/{studentId}
        if (pathParts[2].equals("students") && pathParts.length == 4) {
            int studentId = getIdFromPath(exchange, 3);
            if (studentId == -1) return;
            if (!isAuthorized(exchange, studentId)) return;
            List<Course> courses = courseDao.getAllCourses(studentId);
            if (courses == null || courses.isEmpty()) {
                sendResponse(exchange, 404, "No courses found for this student");
                return;
            }
            sendResponse(exchange, 200, courses);
            return;
        }

        // Handle /courses/{courseId}
        int courseId = getIdFromPath(exchange, 2);
        if (courseId == -1) return;

        Course course = courseDao.getCourseById(courseId);
        if (course == null) {
            sendResponse(exchange, 404, "Course not found");
            return;
        }
        int studentId = course.getStudentId();
        if (!isAuthorized(exchange, studentId)) return;
        sendResponse(exchange, 200, course);
    }

    /**
     * Handles POST requests to add a new course.
     * Expects JSON body with course_name, start_date, and end_date.
     * Validates input and returns appropriate HTTP responses.
     * @param exchange
     * @throws IOException
     */
    private void handlePost(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "POST")) { return; }

        Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap == null) { return; }

        int studentId = getIdFromHeader(exchange);
        if (studentId == -1) return;
        String courseName = requestMap.get("course_name");
        String startDate = requestMap.get("start_date");
        String endDate = requestMap.get("end_date");

        Date sqlStartDate = null;
        Date sqlEndDate = null;
        try {
            if (startDate != null) {
                sqlStartDate = Date.valueOf(startDate);
            }
            if (endDate != null) {
            sqlEndDate = Date.valueOf(endDate);}
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, Map.of("error", "Invalid date format. Use YYYY-MM-DD"));
            return;
        }
        if (courseName == null || sqlStartDate == null || sqlEndDate == null) {
            sendResponse(exchange, 400, Map.of("error", "Course name, start date, and end date are required"));
            return;
        }
        Course course  = courseDao.addCourse(studentId, courseName, sqlStartDate, sqlEndDate);
        if (course == null) {
            sendResponse(exchange, 500, Map.of("error", "Failed to add course"));
            return;
        }
        sendResponse(exchange, 201, course);
    }

    /**
     * Handles DELETE requests to remove a course.
     * Validates that the course exists and that the requesting student is authorized to delete it.
     * Returns appropriate HTTP responses based on the outcome.
     * Authorization is checked via the student_id header.
     * @param exchange
     * @throws IOException
     */
    private void handleDelete(HttpExchange exchange) throws IOException {
        int studentId = getIdFromHeader(exchange);
        if (studentId == -1) return;
        int courseId = getIdFromPath(exchange, 2);
        if (courseId == -1) return;

        Course course = courseDao.getCourseById(courseId);
        if (course == null) {
            sendResponse(exchange, 404, Map.of("error", "Course not found"));
            return;
        }
        if (course.getStudentId() != studentId) {
            sendResponse(exchange, 403, Map.of("error", "Forbidden: You can only delete your own courses"));
            return;
        }
        boolean deleted = courseDao.deleteCourse(courseId);
        if (!deleted) {
            sendResponse(exchange, 500, Map.of("error", "Failed to delete course"));
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
     * @param exchange
     * @throws IOException
     */
    private void handlePut(HttpExchange exchange) throws IOException {
        int courseId = getIdFromPath(exchange, 2);
        if (courseId == -1) return;

        Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap == null) { return; }

        Course existingCourse = courseDao.getCourseById(courseId);
        if (existingCourse == null) {
            sendResponse(exchange, 404, Map.of("error", "Course not found"));
            return;
        }

        int studentId = existingCourse.getStudentId();
        if (!isAuthorized(exchange, studentId)) return;

        String courseName = requestMap.getOrDefault("course_name", existingCourse.getCourseName());

        Date sqlStartDate = existingCourse.getStartDate() != null ?
                new java.sql.Date(existingCourse.getStartDate().getTime()) : null;
        Date sqlEndDate = existingCourse.getEndDate() != null ?
                new java.sql.Date(existingCourse.getEndDate().getTime()) : null;
        try {
            if (requestMap.get("start_date") != null) {
                sqlStartDate = Date.valueOf(requestMap.get("start_date"));
            }
            if (requestMap.get("end_date") != null) {
                sqlEndDate = Date.valueOf(requestMap.get("end_date"));
            }
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, Map.of("error", "Invalid date format. Use YYYY-MM-DD"));
            return;
        }

        boolean updatedCourse = courseDao.updateCourse(courseId, courseName, sqlStartDate, sqlEndDate);
        if (updatedCourse) {
            Course updated = courseDao.getCourseById(courseId);
            sendResponse(exchange, 200, updated);
        } else {
            sendResponse(exchange, 500, Map.of("error", "Failed to update course"));
        }
    }
}
