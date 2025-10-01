package server;

import database.CourseDao;
import database.ScheduleDao;
import model.Course;
import model.Schedule;
import model.Weekday;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.testutils.MockHttpExchange;

import java.sql.Date;
import java.time.LocalTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScheduleHandlerTest {
    private ScheduleHandler scheduleHandler;
    private ScheduleDao mockDao;
    private CourseDao mockCourseDao;

    /**
     * Sets up the test environment before each test case. Mocks the ScheduleDao and CourseDao,
     * and initializes the ScheduleHandler with these mocks.
     */
    @BeforeEach
    public void setUp() {
           this.mockDao = mock(ScheduleDao.class);
           this.mockCourseDao = mock(CourseDao.class);
           this.scheduleHandler = new ScheduleHandler(mockDao, mockCourseDao);
    }

    /**
     * Tests the handling of a GET request to retrieve a schedule by its ID.
     */
    @Test
    public void testHandleGetById(){
        Schedule schedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(schedule);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/schedules/1", "");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle get schedule by id: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("MONDAY"));
    }

    /**
     * Tests the handling of a GET request to retrieve all schedules for a specific course.
     */
    @Test
    public void testHandleGetAllForCourse() {
        Schedule schedule1 = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        Schedule schedule2 = new Schedule(2, 1, model.Weekday.WEDNESDAY, java.time.LocalTime.of(11, 0), java.time.LocalTime.of(12, 0));
        ArrayList<Schedule> schedules = new ArrayList<Schedule>();
        schedules.add(schedule1);
        schedules.add(schedule2);
        when(mockDao.getAllSchedulesForCourse(1)).thenReturn(schedules);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/schedules/courses/1", "");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle get all schedules for course: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("MONDAY"));
        assertTrue(json.contains("WEDNESDAY"));
    }

    /**
     * Tests the handling of a GET request to retrieve all schedules for a specific student.
     */
    @Test
    public void testHandleGetAllForStudent() {
        Schedule schedule1 = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        Schedule schedule2 = new Schedule(2, 2, model.Weekday.WEDNESDAY, java.time.LocalTime.of(11, 0), java.time.LocalTime.of(12, 0));
        ArrayList<Schedule> schedules = new ArrayList<Schedule>();
        schedules.add(schedule1);
        schedules.add(schedule2);
        when(mockDao.getAllSchedulesForStudent(1)).thenReturn(schedules);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/schedules/students/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle get all schedules for student: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("MONDAY"));
        assertTrue(json.contains("WEDNESDAY"));
    }

    /**
     * Tests the handling of a GET request for schedules of a course that does not exist.
     */
    @Test
    public void testHandleGetSchedulesForCourseNotFound() {
        when(mockDao.getAllSchedulesForCourse(999)).thenReturn(new ArrayList<Schedule>());
        MockHttpExchange exchange = new MockHttpExchange("GET", "/schedules/courses/999", "");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle get schedules for course: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("No schedules found for this course"));
    }

    /**
     * Tests the handling of a GET request for schedules of a student that does not exist.
     */
    @Test
    public void testHandleGetSchedulesForStudentNotFound() {
        when(mockDao.getAllSchedulesForStudent(999)).thenReturn(new ArrayList<Schedule>());
        MockHttpExchange exchange = new MockHttpExchange("GET", "/schedules/students/999", "");
        exchange.withHeader("student_id", "999").withHeader("role", "user");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle get schedules for student: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("No schedules found for this student"));
    }

    /**
     * Tests the handling of a GET request for schedules of a student by an unauthorized user.
     */
    @Test
    public void testHandleGetUnauthorized() {
        MockHttpExchange exchange = new MockHttpExchange("GET", "/schedules/students/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle get schedules unauthorized: " + e.getMessage());
        }
        assertEquals(403, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Forbidden"));
    }

    /**
     * Tests the handling of a GET request for a schedule that does not exist.
     */
    @Test
    public void testHandleGetScheduleNotFound() {
        when(mockDao.getSchedule(999)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/schedules/999", "");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle get schedule not found: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Schedule not found"));
    }

    /**
     * Tests the handling of a POST request to create a new schedule.
     */
    @Test
    public void testHandlePostSchedule() {
        String body = "{\"course_id\":\"1\",\"weekday\":\"MONDAY\",\"start_time\":\"09:00\",\"end_time\":\"10:00\"}";
        Schedule schedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.insertSchedule(1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0))).thenReturn(schedule);
        MockHttpExchange exchange = new MockHttpExchange("POST", "/schedules", body);
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle post schedule: " + e.getMessage());
        }
        assertEquals(201, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("MONDAY"));
    }

    /**
     * Tests the handling of a POST request to create a new schedule with missing course_id.
     */
    @Test
    public void testHandlePostScheduleNoCourseId() {
        String body = "{\"weekday\":\"MONDAY\",\"start_time\":\"09:00\",\"end_time\":\"10:00\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/schedules", body);
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle post schedule no course id: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("course_id is required"));
    }

    /**
     * Tests the handling of a POST request to create a new schedule with an invalid course_id format.
     */
    @Test
    public void testHandlePostScheduleInvalidCourseId() {
        String body = "{\"course_id\":\"abc\",\"weekday\":\"MONDAY\",\"start_time\":\"09:00\",\"end_time\":\"10:00\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/schedules", body);
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle post schedule invalid course id: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Invalid course_id format"));
    }

    /**
     * Tests the handling of a POST request to create a new schedule with an invalid weekday value.
     */
    @Test
    public void testHandlePostInvalidWeekday() {
        String body = "{\"course_id\":\"1\",\"weekday\":\"FUNDAY\",\"start_time\":\"09:00\",\"end_time\":\"10:00\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/schedules", body);
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle post invalid weekday: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Invalid weekday value"));
    }

    /**
     * Tests the handling of a POST request to create a new schedule with missing required fields.
     */
    @Test
    public void testHandlePostMissingFields() {
        String body = "{\"course_id\":\"1\",\"start_time\":\"09:00\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/schedules", body);
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle post missing fields: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("course_id, weekday, start_time, and end_time are required"));
    }

    /**
     * Tests the handling of a POST request to create a new schedule with invalid time format.
     */
    @Test
    public void testHandlePostInvalidTimeFormat() {
        String body = "{\"course_id\":\"1\",\"weekday\":\"MONDAY\",\"start_time\":\"9 AM\",\"end_time\":\"10 AM\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/schedules", body);
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle post invalid time format: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Invalid time format. Use HH:MM"));
    }

    /**
     * Tests the handling of a POST request to create a new schedule where the end time is before the start time.
     */
    @Test
    public void testHandleEndTimeBeforeStartTime() {
        String body = "{\"course_id\":\"1\",\"weekday\":\"MONDAY\",\"start_time\":\"10:00\",\"end_time\":\"09:00\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/schedules", body);
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle post end time before start time: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("start_time must be before end_time"));
    }

    /**
     * Tests the handling of a POST request to create a new schedule that fails to be added to the database.
     */
    @Test
    public void testHandlePostFailure(){
        String body = "{\"course_id\":\"1\",\"weekday\":\"MONDAY\",\"start_time\":\"09:00\",\"end_time\":\"10:00\"}";
        when(mockDao.insertSchedule(1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0))).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("POST", "/schedules", body);
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle post schedule failure: " + e.getMessage());
        }
        assertEquals(500, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Failed to add schedule"));
    }

    /**
     * Tests the handling of a DELETE request to remove a schedule by its ID.
     */
    @Test
    public void testHandleDeleteSchedule() {
        Schedule schedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(schedule);
        Course course = new Course(1, 1, "Test Course", java.sql.Date.valueOf("2023-01-01"), java.sql.Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        when(mockDao.deleteSchedule(1)).thenReturn(true);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/schedules/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle delete schedule: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Schedule deleted successfully"));
    }

    /**
     * Tests the handling of a DELETE request for a schedule that does not exist.
     */
    @Test
    public void testHandleDeleteScheduleNotFound() {
        when(mockDao.getSchedule(1)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/schedules/1", "");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle delete schedule not found: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Schedule not found"));
    }

    /**
     * Tests the handling of a DELETE request for a schedule by an unauthorized user.
     */
    @Test
    public void testHandleDeleteUnauthorized() {
        Schedule schedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(schedule);
        Course course = new Course(1, 1, "Test Course", java.sql.Date.valueOf("2023-01-01"), java.sql.Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/schedules/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle delete schedule unauthorized: " + e.getMessage());
        }
        assertEquals(403, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Forbidden"));
    }

    /**
     * Tests the handling of a DELETE request that fails to remove the schedule from the database.
     */
    @Test
    public void testHandleDeleteFailure() {
        Schedule schedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(schedule);
        Course course = new Course(1, 1, "Test Course", java.sql.Date.valueOf("2023-01-01"), java.sql.Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        when(mockDao.deleteSchedule(1)).thenReturn(false);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/schedules/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle delete schedule failure: " + e.getMessage());
        }
        assertEquals(500, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Failed to delete schedule"));
    }

    /**
     * Tests the handling of a PUT request to update an existing schedule.
     */
    @Test
    public void testHandleUpdateSchedule() {
        String body = "{\"course_id\":\"1\",\"weekday\":\"TUESDAY\",\"start_time\":\"10:00\",\"end_time\":\"11:00\"}";
        Schedule existingSchedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(existingSchedule);
        Course course = new Course(1, 1, "Test Course", java.sql.Date.valueOf("2023-01-01"), java.sql.Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        when(mockDao.updateSchedule(1, 1, Weekday.TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0)))
                .thenAnswer(invocation -> {
                    existingSchedule.setWeekday(Weekday.TUESDAY);
                    existingSchedule.setStartTime(LocalTime.of(10,0));
                    existingSchedule.setEndTime(LocalTime.of(11,0));
                    return true;
                });
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle update schedule: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("TUESDAY"));
    }

    /**
     * Tests the handling of a PUT request to update a schedule that does not exist.
     */
    @Test
    public void testUpdateScheduleNotFound() {
        String body = "{\"course_id\":\"1\",\"weekday\":\"TUESDAY\",\"start_time\":\"10:00\",\"end_time\":\"11:00\"}";
        when(mockDao.getSchedule(1)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle update schedule not found: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Schedule not found"));
    }

    /**
     * Tests the handling of a PUT request to update a schedule with a course_id that does not exist.
     */
    @Test
    public void testHandleUpdateScheduleNoCourseId() {
        String body = "{\"weekday\":\"TUESDAY\",\"start_time\":\"10:00\",\"end_time\":\"11:00\"}";
        Schedule existingSchedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(existingSchedule);
        when(mockCourseDao.getCourseById(1)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle update schedule no course id: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("course not found"));
    }

    /**
     * Tests the handling of a PUT request to update a schedule by an unauthorized user.
     */
    @Test
    public void testUpdateScheduleUnauthorized() {
        String body = "{\"course_id\":\"1\",\"weekday\":\"TUESDAY\",\"start_time\":\"10:00\",\"end_time\":\"11:00\"}";
        Schedule existingSchedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(existingSchedule);
        Course course = new Course(1, 1, "Test Course", java.sql.Date.valueOf("2023-01-01"), java.sql.Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle update schedule unauthorized: " + e.getMessage());
        }
        assertEquals(403, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Forbidden"));
    }

    /**
     * Tests the handling of a PUT request to update a schedule with invalid JSON format.
     */
    @Test
    public void handlePutInvalidJson() {
        Schedule existingSchedule = new Schedule(1, 1, Weekday.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(existingSchedule);
        Course course = new Course(1, 1, "Test Course", Date.valueOf("2023-01-01"), Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        String body = "Invalid JSON";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle put invalid json: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("invalid json"));
    }

    /**
     * Tests the handling of a PUT request to update a schedule with invalid time format.
     */
    @Test
    public void testHandlePutInvalidTimeFormat() {
        String body = "{\"course_id\":\"1\",\"weekday\":\"TUESDAY\",\"start_time\":\"10 AM\",\"end_time\":\"11 AM\"}";
        Schedule existingSchedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(existingSchedule);
        Course course = new Course(1, 1, "Test Course", java.sql.Date.valueOf("2023-01-01"), java.sql.Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle put invalid time format: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Invalid time or weekday format"));
    }

    /**
     * Tests the handling of a PUT request to update a schedule with an invalid weekday value.
     */
    @Test
    public void handlePutInvalidWeekday() {
        String body = "{\"course_id\":\"1\",\"weekday\":\"FUNDAY\",\"start_time\":\"10:00\",\"end_time\":\"11:00\"}";
        Schedule existingSchedule = new Schedule(1, 1, Weekday.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(existingSchedule);
        Course course = new Course(1, 1, "Test Course", Date.valueOf("2023-01-01"), Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle put invalid weekday: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Invalid time or weekday format"));
    }

    /**
     * Tests the handling of a PUT request to update a schedule where the start time is after the end time.
     */
    @Test
    public void handlePutInvalidStartTime() {
        String body = "{\"course_id\":\"1\",\"weekday\":\"TUESDAY\",\"start_time\":\"11:00\",\"end_time\":\"10:00\"}";
        Schedule existingSchedule = new Schedule(1, 1, Weekday.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(existingSchedule);
        Course course = new Course(1, 1, "Test Course", Date.valueOf("2023-01-01"), Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle put invalid start time: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("start_time must be before end_time"));
    }

    /**
     * Tests the handling of a PUT request to update a schedule that fails to be updated in the database.
     */
    @Test
    public void testHandlePutFailure() {
        String body = "{\"course_id\":\"1\",\"weekday\":\"TUESDAY\",\"start_time\":\"10:00\",\"end_time\":\"11:00\"}";
        Schedule existingSchedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(existingSchedule);
        Course course = new Course(1, 1, "Test Course", java.sql.Date.valueOf("2023-01-01"), java.sql.Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        when(mockDao.updateSchedule(1, 1, Weekday.TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0)))
                .thenReturn(false);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            scheduleHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle update schedule failure: " + e.getMessage());
        }
        assertEquals(500, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Failed to update schedule"));
    }
}
