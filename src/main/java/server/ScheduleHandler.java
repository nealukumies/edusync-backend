/**
 * This class handles HTTP requests related to schedules.
 */
package server;

import com.sun.net.httpserver.HttpExchange;
import database.CourseDao;
import database.ScheduleDao;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.Course;
import model.Schedule;
import model.Weekday;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public class ScheduleHandler extends BaseHandler {
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "ScheduleDao is controller; no exposure risk"
    )
    private final ScheduleDao scheduleDao;
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "CourseDao is controller; no exposure risk"
    )
    private final CourseDao courseDao;
    private static final String ERROR_KEY = "error";

    /**
     * Constructor for ScheduleHandler. Data access objects (DAOs) are injected via constructor.
     * @param scheduleDao
     * @param courseDao
     */
    public ScheduleHandler(ScheduleDao scheduleDao, CourseDao courseDao) {
        super();
        this.scheduleDao = scheduleDao;
        this.courseDao = courseDao;
    }

    /**
     * Handles GET requests for schedules.
     * Supports fetching schedules by course ID, student ID, or schedule ID.
     * Authorization checks ensure students can only access their own schedules.
     * @param exchange
     * @throws IOException
     */
    protected void handleGet(HttpExchange exchange) throws IOException {
        String[] pathParts = exchange.getRequestURI().getPath().split("/");

        if ("courses".equals(pathParts[2]) && pathParts.length == 4) {
            int courseId = getIdFromPath(exchange, 3);
            if (courseId == -1) return;
            List<Schedule> schedules = scheduleDao.getAllSchedulesForCourse(courseId);
            if (schedules == null || schedules.isEmpty()) {
                sendResponse(exchange, 404, "No schedules found for this course");
                return;
            }
            sendResponse(exchange, 200, schedules);
            return;
        }

        if ("students".equals(pathParts[2]) && pathParts.length == 4) {
            int studentId = getIdFromPath(exchange, 3);
            if (studentId == -1) return;
            if (!isAuthorized(exchange, studentId)) return;
            List<Schedule> schedules = scheduleDao.getAllSchedulesForStudent(studentId);
            if (schedules == null || schedules.isEmpty()) {
                sendResponse(exchange, 404, "No schedules found for this student");
                return;
            }
            sendResponse(exchange, 200, schedules);
            return;
        }

        int scheduleId = getIdFromPath(exchange, 2);
        if (scheduleId == -1) return;
        Schedule schedule = scheduleDao.getSchedule(scheduleId);
        if (schedule == null) {
            sendResponse(exchange, 404, "Schedule not found");
            return;
        }
        sendResponse(exchange, 200, schedule);
    }

    /**
     * Handles POST requests to create a new schedule.
     * Expects a JSON body with course_id, weekday, start_time, and end_time.
     * Validates input and checks authorization before creating the schedule.
     * Returns appropriate HTTP responses based on the outcome. Success returns 201 Created and the created schedule.
     * @param exchange
     * @throws IOException
     */
    protected void handlePost(HttpExchange exchange) throws IOException {
        Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap == null) { return; }

        String courseStr = (requestMap.get("course_id"));
        if (courseStr == null) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "course_id is required"));
            return;
        }
        int courseId;
        try {
            courseId = Integer.parseInt(courseStr);
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid course_id format"));
            return;
        }

        String startTime = requestMap.get("start_time");
        String endTime = requestMap.get("end_time");
        Weekday weekday = null;
        try {
            String weekdayStr = requestMap.get("weekday");
            if (weekdayStr != null) {
                weekday = Weekday.valueOf(weekdayStr.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid weekday value"));
            return;
        }

        if (courseId <= 0 || weekday == null || startTime == null || endTime == null) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "course_id, weekday, start_time, and end_time are required"));
            return;
        }

        LocalTime start;
        LocalTime end;
        try {
            start = LocalTime.parse(startTime);
            end = LocalTime.parse(endTime);
        } catch (Exception e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid time format. Use HH:MM"));
            return;
        }
        if (!start.isBefore(end)) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "start_time must be before end_time"));
            return;
        }

        Schedule schedule = scheduleDao.insertSchedule(courseId, weekday, start, end);
        if (schedule == null) {
            sendResponse(exchange, 500, Map.of(ERROR_KEY, "Failed to add schedule"));
            return;
        }
        sendResponse(exchange, 201, schedule);
    }

    /**
     * Handles DELETE requests to remove a schedule by its ID.
     * Expects the schedule ID in the URL path as /schedules/{scheduleId}.
     * Authorization is checked to ensure the requester owns the schedule.
     * Returns appropriate HTTP responses based on the outcome.
     * @param exchange
     * @throws IOException
     */
     protected void handleDelete(HttpExchange exchange) throws IOException {
        int scheduleId = getIdFromPath(exchange, 2);
        if (scheduleId == -1) return;

        Schedule schedule = scheduleDao.getSchedule(scheduleId);
        if (schedule == null) {
            sendResponse(exchange, 404, "Schedule not found");
            return;
        }
        int courseId = schedule.getCourseId();
        CourseDao courseDao = new CourseDao();
        Course course = courseDao.getCourseById(courseId);
        int studentId = course.getStudentId();
        if (!isAuthorized(exchange, studentId)) return;
        boolean success = scheduleDao.deleteSchedule(scheduleId);
        if (!success) {
            sendResponse(exchange, 500, Map.of(ERROR_KEY, "Failed to delete schedule"));
            return;
        }
        sendResponse(exchange, 200, Map.of("message", "Schedule deleted successfully"));
    }

    /**
     * Handles PUT requests to update an existing schedule.
     * Expects a JSON body with fields to update: start_time, end_time, and/or weekday.
     * Validates input and checks authorization before updating.
     * Returns appropriate HTTP responses based on the outcome.
     * @param exchange
     * @throws IOException
     */
    protected void handlePut(HttpExchange exchange) throws IOException {
        int scheduleId = getIdFromPath(exchange, 2);
        if (scheduleId == -1) return;

        Schedule existingSchedule = scheduleDao.getSchedule(scheduleId);
        if (existingSchedule == null) {
            sendResponse(exchange, 404, "Schedule not found");
            return;
        }

        int courseId = existingSchedule.getCourseId();
        Course course = courseDao.getCourseById(courseId);
        if (course == null) {
            sendResponse(exchange, 404, "course not found");
            return;
        }
        int studentId = course.getStudentId();
        if (!isAuthorized(exchange, studentId)) return;

        Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap == null){
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "invalid json"));
            return;
        }

        LocalTime start = existingSchedule.getStartTime();
        LocalTime end = existingSchedule.getEndTime();
        Weekday weekday = existingSchedule.getWeekday();

        try {
            String startTimeStr = requestMap.get("start_time");
            String endTimeStr = requestMap.get("end_time");
            String weekdayStr = requestMap.get("weekday");

            if (startTimeStr != null) start = LocalTime.parse(startTimeStr);
            if (endTimeStr != null) end = LocalTime.parse(endTimeStr);
            if (weekdayStr != null) weekday = Weekday.valueOf(weekdayStr.toUpperCase());
        } catch (Exception e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid time or weekday format"));
            return;
        }

        if (!start.isBefore(end)) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "start_time must be before end_time"));
            return;
        }

        boolean updated = scheduleDao.updateSchedule(scheduleId, courseId, weekday, start, end);
        if (!updated) {
            sendResponse(exchange, 500, Map.of(ERROR_KEY, "Failed to update schedule"));
            return;
        }
        sendResponse(exchange, 200, scheduleDao.getSchedule(scheduleId));
    }

}
