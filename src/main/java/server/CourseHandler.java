package server;

import com.sun.net.httpserver.HttpExchange;
import database.CourseDao;
import model.Course;

import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.Map;

public class CourseHandler extends BaseHandler {
    private CourseDao courseDao;

    public CourseHandler(CourseDao courseDao) {
        this.courseDao = courseDao;
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

        // Handle /courses/students/{studentId}
        if (pathParts.length == 4 && pathParts[2].equals("students")) {
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
