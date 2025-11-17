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

/**
 * This class handles HTTP requests related to schedules.
 */
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
     *
     * @param scheduleDao The ScheduleDao instance for database operations.
     * @param courseDao The CourseDao instance for database operations.
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
     *
     * @param exchange The HttpExchange object containing request and response data.
     * @throws IOException Throws IOException if an I/O error occurs.
     */
    @Override
    protected void handleGet(HttpExchange exchange) throws IOException {
        final String[] pathParts = getPathParts(exchange);

        if (handleGetByCourse(exchange, pathParts)) {return;}
        if (handleGetByStudent(exchange, pathParts)) {return;}
        handleGetByScheduleId(exchange);
    }

    /**
     * Handles GET requests to retrieve schedules by course ID.
     * Expects the course ID in the URL path as /schedules/courses/{courseId}.
     * Returns all schedules associated with the specified course.
     *
     * @param exchange The HttpExchange object for the request/response
     * @param pathParts The parts of the request path
     * @return true if the request was handled, false otherwise
     * @throws IOException Throws IOException if an I/O error occurs
     */
    private boolean handleGetByCourse(HttpExchange exchange, String[] pathParts) throws IOException {
        if (!"courses".equals(pathParts[2]) || pathParts.length != 4) {return false;}

        final int courseId = getIdFromPath(exchange, 3);
        if (courseId == -1) {return true;}

        final List<Schedule> schedules = scheduleDao.getAllSchedulesForCourse(courseId);
        if (schedules == null || schedules.isEmpty()) {
            sendResponse(exchange, 404, "No schedules found for this course");
            return true;
        }

        sendResponse(exchange, 200, schedules);
        return true;
    }

    /**
     * Handles GET requests to retrieve schedules by student ID.
     * Expects the student ID in the URL path as /schedules/students/{studentId}.
     * Checks authorization to ensure the requester has permission to access the student's schedules.
     * Returns all schedules associated with the specified student.
     *
     * @param exchange The HttpExchange object for the request/response
     * @param pathParts The parts of the request path
     * @return true if the request was handled, false otherwise
     * @throws IOException Throws IOException if an I/O error occurs
     */
    private boolean handleGetByStudent(HttpExchange exchange, String[] pathParts) throws IOException {
        if (!"students".equals(pathParts[2]) || pathParts.length != 4) {return false;}

        final int studentId = getIdFromPath(exchange, 3);
        if (studentId == -1) {return true;}
        if (!isAuthorized(exchange, studentId)) {return true;}

        final List<Schedule> schedules = scheduleDao.getAllSchedulesForStudent(studentId);
        if (schedules == null || schedules.isEmpty()) {
            sendResponse(exchange, 404, "No schedules found for this student");
            return true;
        }

        sendResponse(exchange, 200, schedules);
        return true;
    }

    /**
     * Handles GET requests to retrieve a schedule by its ID.
     * Expects the schedule ID in the URL path as /schedules/{scheduleId}.
     * Returns the schedule if found, otherwise returns a 404 Not Found response.
     *
     * @param exchange The HttpExchange object for the request/response
     * @throws IOException Throws IOException if an I/O error occurs
     */
    private void handleGetByScheduleId(HttpExchange exchange) throws IOException {
        final int scheduleId = getIdFromPath(exchange, 2);
        if (scheduleId == -1) {return;}

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
     *
     * @param exchange The HttpExchange object containing request and response data.
     * @throws IOException Throws IOException if an I/O error occurs.
     */
    @Override
    protected void handlePost(HttpExchange exchange) throws IOException {
        final Map<String, String> body = parseJsonBody(exchange);

        final int courseId;
        try {
            courseId = Integer.parseInt(body.getOrDefault("course_id", ""));
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid course_id format"));
            return;
        }

        if (body.get("weekday") == null || body.get("start_time") == null || body.get("end_time") == null) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "course_id, weekday, start_time, and end_time are required"));
            return;
        }

        final Weekday weekday;
        final LocalTime start;
        final LocalTime end;

        try {
            weekday = parseWeekdaySafe(body.get("weekday"));
            start = parseTimeSafe(body.get("start_time"));
            end   = parseTimeSafe(body.get("end_time"));
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, e.getMessage()));
            return;
        }

        if (!start.isBefore(end)) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "start_time must be before end_time"));
            return;
        }
        final Schedule created = scheduleDao.insertSchedule(courseId, weekday, start, end);
        if (created == null) {
            sendResponse(exchange, 500, Map.of(ERROR_KEY, "Failed to add schedule"));
            return;
        }
        sendResponse(exchange, 201, created);
    }

    /**
     * Handles DELETE requests to remove a schedule by its ID.
     * Expects the schedule ID in the URL path as /schedules/{scheduleId}.
     * Authorization is checked to ensure the requester owns the schedule.
     * Returns appropriate HTTP responses based on the outcome.
     *
     * @param exchange The HttpExchange object containing request and response data.
     * @throws IOException Throws IOException if an I/O error occurs.
     */
    @Override
    protected void handleDelete(HttpExchange exchange) throws IOException {
        final Schedule schedule = loadScheduleAndAuthorize(exchange);
        if (schedule == null) {return;}

        final boolean success = scheduleDao.deleteSchedule(schedule.getScheduleId());
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
     *
     * @param exchange The HttpExchange object containing request and response data.
     * @throws IOException Throws IOException if an I/O error occurs.
     */
    @Override
    protected void handlePut(HttpExchange exchange) throws IOException {
        final Schedule existing = loadScheduleAndAuthorize(exchange);
        if (existing == null) {return;}

        final Map<String, String> requestMap = parseJsonBody(exchange);
        if (requestMap.isEmpty()) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "Invalid JSON"));
            return;
        }

        LocalTime start = existing.getStartTime();
        LocalTime end   = existing.getEndTime();
        Weekday weekday = existing.getWeekday();

        try {
            final LocalTime parsedStart = parseTimeSafe(requestMap.get("start_time"));
            final LocalTime parsedEnd   = parseTimeSafe(requestMap.get("end_time"));
            final Weekday parsedWeekday = parseWeekdaySafe(requestMap.get("weekday"));

            if (parsedStart != null) {start = parsedStart;}
            if (parsedEnd != null)   {end   = parsedEnd;}
            if (parsedWeekday != null) {weekday = parsedWeekday;}

        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, e.getMessage()));
            return;
        }

        if (!start.isBefore(end)) {
            sendResponse(exchange, 400, Map.of(ERROR_KEY, "start_time must be before end_time"));
            return;
        }

        final boolean updated = scheduleDao.updateSchedule(
                existing.getScheduleId(),
                existing.getCourseId(),
                weekday,
                start,
                end
        );

        if (!updated) {
            sendResponse(exchange, 500, Map.of(ERROR_KEY, "Failed to update schedule"));
            return;
        }

        sendResponse(exchange, 200, scheduleDao.getSchedule(existing.getScheduleId()));
    }

    private Weekday parseWeekdaySafe(String weekdayStr) {
        Weekday result = null;
        if (weekdayStr != null) {
            try {
                result = Weekday.valueOf(weekdayStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid weekday value", e);
            }
        }
        return result;
    }

    private LocalTime parseTimeSafe(String timeStr) {
        LocalTime result = null;
        if (timeStr != null) {
            try {
                result = LocalTime.parse(timeStr);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid time format. Use HH:MM", e);
            }
        }
        return result;
    }

    private Course loadAndAuthorizeCourse(HttpExchange exchange, int courseId) throws IOException {
        final Course course = courseDao.getCourseById(courseId);
        if (course == null) {
            sendResponse(exchange, 404, Map.of(ERROR_KEY, "course not found"));
            return null;
        }
        if (!isAuthorized(exchange, course.getStudentId())) {
            return null;
        }
        return course;
    }

    private Schedule loadScheduleAndAuthorize(HttpExchange exchange) throws IOException {
        final int scheduleId = getIdFromPath(exchange, 2);
        if (scheduleId == -1) {
            return null;
        }

        final Schedule schedule = scheduleDao.getSchedule(scheduleId);
        if (schedule == null) {
            sendResponse(exchange, 404, SCHEDULE_NOT_FOUND);
            return null;
        }

        if (loadAndAuthorizeCourse(exchange, schedule.getCourseId()) == null) {
            return null;
        }

        return schedule;
    }



}