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
import java.time.format.DateTimeParseException;
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
    private static final String SCHEDULE_NOT_FOUND = "Schedule not found";

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
    @Override
    protected void handleGet(HttpExchange exchange) throws IOException {
        final String[] pathParts = exchange.getRequestURI().getPath().split("/");

        if (handleGetByCourse(exchange, pathParts)) {return;}
        if (handleGetByStudent(exchange, pathParts)) {return;}
        handleGetByScheduleId(exchange);
    }

    private boolean handleGetByCourse(HttpExchange exchange, String[] pathParts) throws IOException {
        if (!"courses".equals(pathParts[2]) || pathParts.length != 4) return false;

        final int courseId = getIdFromPath(exchange, 3);
        if (courseId == -1) return true;

        final List<Schedule> schedules = scheduleDao.getAllSchedulesForCourse(courseId);
        if (schedules == null || schedules.isEmpty()) {
            sendResponse(exchange, 404, "No schedules found for this course");
            return true;
        }

        sendResponse(exchange, 200, schedules);
        return true;
    }

    private boolean handleGetByStudent(HttpExchange exchange, String[] pathParts) throws IOException {
        if (!"students".equals(pathParts[2]) || pathParts.length != 4) return false;

        final int studentId = getIdFromPath(exchange, 3);
        if (studentId == -1) return true;
        if (!isAuthorized(exchange, studentId)) return true;

        final List<Schedule> schedules = scheduleDao.getAllSchedulesForStudent(studentId);
        if (schedules == null || schedules.isEmpty()) {
            sendResponse(exchange, 404, "No schedules found for this student");
            return true;
        }

        sendResponse(exchange, 200, schedules);
        return true;
    }

    private void handleGetByScheduleId(HttpExchange exchange) throws IOException {
        final int scheduleId = getIdFromPath(exchange, 2);
        if (scheduleId == -1) return;

        final Schedule schedule = scheduleDao.getSchedule(scheduleId);
        if (schedule == null) {
            sendResponse(exchange, 404, SCHEDULE_NOT_FOUND);
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
    @Override
    protected void handlePost(HttpExchange exchange) throws IOException {
        final Map<String, String> requestMap = parseJsonBody(exchange);

        final String courseStr = requestMap.get("course_id");
        if (courseStr == null) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "course_id is required"));
            return;
        }
        final int courseId;
        try {
            courseId = Integer.parseInt(courseStr);
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid course_id format"));
            return;
        }

        final String startTime = requestMap.get("start_time");
        final String endTime = requestMap.get("end_time");
        Weekday weekday = null;
        try {
            final String weekdayStr = requestMap.get("weekday");
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

        final LocalTime start;
        final LocalTime end;
        try {
            start = LocalTime.parse(startTime);
            end = LocalTime.parse(endTime);
        } catch (DateTimeParseException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid time format. Use HH:MM"));
            return;
        }
        if (!start.isBefore(end)) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "start_time must be before end_time"));
            return;
        }

        final Schedule schedule = scheduleDao.insertSchedule(courseId, weekday, start, end);
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
    @Override
     protected void handleDelete(HttpExchange exchange) throws IOException {
         final int scheduleId = getIdFromPath(exchange, 2);
         if (scheduleId == -1) {return;}

         final Schedule schedule = scheduleDao.getSchedule(scheduleId);
         if (schedule == null) {
            sendResponse(exchange, 404, SCHEDULE_NOT_FOUND);
            return;
         }
        final int courseId = schedule.getCourseId();
        final Course course = courseDao.getCourseById(courseId);
        final int studentId = course.getStudentId();
        if (!isAuthorized(exchange, studentId)) {return;}
        final boolean success = scheduleDao.deleteSchedule(scheduleId);
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
    @Override
    protected void handlePut(HttpExchange exchange) throws IOException {
        final int scheduleId = getIdFromPath(exchange, 2);
        if (scheduleId == -1) {return;}

        final Schedule existingSchedule = scheduleDao.getSchedule(scheduleId);
        if (existingSchedule == null) {
            sendResponse(exchange, 404, SCHEDULE_NOT_FOUND);
            return;
        }

        final int courseId = existingSchedule.getCourseId();
        final Course course = courseDao.getCourseById(courseId);
        if (course == null) {
            sendResponse(exchange, 404, "course not found");
            return;
        }
        final int studentId = course.getStudentId();
        if (!isAuthorized(exchange, studentId)) {return;}

        final Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap.isEmpty()) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid JSON"));
            return;
        }

        LocalTime start = existingSchedule.getStartTime();
        LocalTime end = existingSchedule.getEndTime();
        Weekday weekday = existingSchedule.getWeekday();

        try {
            final String startTimeStr = requestMap.get("start_time");
            final String endTimeStr = requestMap.get("end_time");
            final String weekdayStr = requestMap.get("weekday");

            if (startTimeStr != null) {start = LocalTime.parse(startTimeStr);}
            if (endTimeStr != null) {end = LocalTime.parse(endTimeStr);}
            if (weekdayStr != null) {weekday = Weekday.valueOf(weekdayStr.toUpperCase());}
        } catch (DateTimeParseException | IllegalArgumentException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid time or weekday format"));
            return;
        }

        if (!start.isBefore(end)) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "start_time must be before end_time"));
            return;
        }

        final boolean updated = scheduleDao.updateSchedule(scheduleId, courseId, weekday, start, end);
        if (!updated) {
            sendResponse(exchange, 500, Map.of(ERROR_KEY, "Failed to update schedule"));
            return;
        }
        sendResponse(exchange, 200, scheduleDao.getSchedule(scheduleId));
    }
}
