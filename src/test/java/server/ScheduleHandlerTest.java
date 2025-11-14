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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduleHandlerTest {
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
    void testHandleGetById() throws Exception {
        Schedule schedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(schedule);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/schedules/1", "");
        scheduleHandler.handle(exchange);
        assertEquals(200, exchange.getResponseCode(), "Expected HTTP 200 OK response");
    }

    /**
     * Tests the handling of a GET request to retrieve all schedules for a specific course.
     */
    @Test
    void testHandleGetAllForCourse() throws Exception{
        Schedule schedule1 = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        Schedule schedule2 = new Schedule(2, 1, model.Weekday.WEDNESDAY, java.time.LocalTime.of(11, 0), java.time.LocalTime.of(12, 0));
        List<Schedule> schedules = new ArrayList<Schedule>();
        schedules.add(schedule1);
        schedules.add(schedule2);
        when(mockDao.getAllSchedulesForCourse(1)).thenReturn(schedules);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/schedules/courses/1", "");
        scheduleHandler.handle(exchange);
        assertEquals(200, exchange.getResponseCode(), "Expected HTTP 200 OK response");
   }

    /**
     * Tests the handling of a GET request to retrieve all schedules for a specific student.
     */
    @Test
    void testHandleGetAllForStudent() throws Exception{
        Schedule schedule1 = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        Schedule schedule2 = new Schedule(2, 2, model.Weekday.WEDNESDAY, java.time.LocalTime.of(11, 0), java.time.LocalTime.of(12, 0));
        List<Schedule> schedules = new ArrayList<Schedule>();
        schedules.add(schedule1);
        schedules.add(schedule2);
        when(mockDao.getAllSchedulesForStudent(1)).thenReturn(schedules);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/schedules/students/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        scheduleHandler.handle(exchange);
        assertEquals(200, exchange.getResponseCode(), "Expected HTTP 200 OK response");
    }

    /**
     * Tests the handling of a GET request for schedules of a course that does not exist.
     */
    @Test
    void testHandleGetSchedulesForCourseNotFound() throws Exception {
        when(mockDao.getAllSchedulesForCourse(999)).thenReturn(new ArrayList<Schedule>());
        MockHttpExchange exchange = new MockHttpExchange("GET", "/schedules/courses/999", "");
        scheduleHandler.handle(exchange);
        assertEquals(404, exchange.getResponseCode(), "No schedules should be found for non-existent course");
   }

    /**
     * Tests the handling of a GET request for schedules of a student that does not exist.
     */
    @Test
    void testHandleGetSchedulesForStudentNotFound() throws Exception {
        when(mockDao.getAllSchedulesForStudent(999)).thenReturn(new ArrayList<Schedule>());
        MockHttpExchange exchange = new MockHttpExchange("GET", "/schedules/students/999", "");
        exchange.withHeader("student_id", "999").withHeader("role", "user");
        scheduleHandler.handle(exchange);
        assertEquals(404, exchange.getResponseCode(), "No schedules should be found for non-existent student");
    }

    /**
     * Tests the handling of a GET request for schedules of a student by an unauthorized user.
     */
    @Test
    void testHandleGetUnauthorized() throws Exception {
        MockHttpExchange exchange = new MockHttpExchange("GET", "/schedules/students/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        scheduleHandler.handle(exchange);
        assertEquals(403, exchange.getResponseCode(), "Expected HTTP 403 Forbidden response");
    }

    /**
     * Tests the handling of a GET request for a schedule that does not exist.
     */
    @Test
    void testHandleGetScheduleNotFound() throws Exception {
        when(mockDao.getSchedule(999)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/schedules/999", "");
        scheduleHandler.handle(exchange);
        assertEquals(404, exchange.getResponseCode(), "Expected HTTP 404 Not Found response");
    }

    /**
     * Tests the handling of a POST request to create a new schedule.
     */
    @Test
    void testHandlePostSchedule() throws Exception {
        String body = "{\"course_id\":\"1\",\"weekday\":\"MONDAY\",\"start_time\":\"09:00\",\"end_time\":\"10:00\"}";
        Schedule schedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.insertSchedule(1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0))).thenReturn(schedule);
        MockHttpExchange exchange = new MockHttpExchange("POST", "/schedules", body);
        scheduleHandler.handle(exchange);
        assertEquals(201, exchange.getResponseCode(), "Expected HTTP 201 Created response");
    }

    /**
     * Tests the handling of a POST request to create a new schedule with missing course_id.
     */
    @Test
    void testHandlePostScheduleNoCourseId() throws Exception {
        String body = "{\"weekday\":\"MONDAY\",\"start_time\":\"09:00\",\"end_time\":\"10:00\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/schedules", body);
        scheduleHandler.handle(exchange);
        assertEquals(400, exchange.getResponseCode(), "Expected HTTP 400 Bad Request response");
   }

    /**
     * Tests the handling of a POST request to create a new schedule with an invalid course_id format.
     */
    @Test
    void testHandlePostScheduleInvalidCourseId() throws Exception {
        String body = "{\"course_id\":\"abc\",\"weekday\":\"MONDAY\",\"start_time\":\"09:00\",\"end_time\":\"10:00\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/schedules", body);
        scheduleHandler.handle(exchange);
        assertEquals(400, exchange.getResponseCode(), "Expected HTTP 400 Bad Request response");
   }

    /**
     * Tests the handling of a POST request to create a new schedule with an invalid weekday value.
     */
    @Test
    void testHandlePostInvalidWeekday() throws Exception {
        String body = "{\"course_id\":\"1\",\"weekday\":\"FUNDAY\",\"start_time\":\"09:00\",\"end_time\":\"10:00\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/schedules", body);
        scheduleHandler.handle(exchange);

        assertEquals(400, exchange.getResponseCode(), "Expected HTTP 400 Bad Request response");
    }

    /**
     * Tests the handling of a POST request to create a new schedule with missing required fields.
     */
    @Test
    void testHandlePostMissingFields() throws Exception {
        String body = "{\"course_id\":\"1\",\"start_time\":\"09:00\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/schedules", body);
        scheduleHandler.handle(exchange);
        assertEquals(400, exchange.getResponseCode(), "Expected HTTP 400 Bad Request response");
   }

    /**
     * Tests the handling of a POST request to create a new schedule with invalid time format.
     */
    @Test
    void testHandlePostInvalidTimeFormat() throws Exception {
        String body = "{\"course_id\":\"1\",\"weekday\":\"MONDAY\",\"start_time\":\"9 AM\",\"end_time\":\"10 AM\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/schedules", body);
        scheduleHandler.handle(exchange);
        assertEquals(400, exchange.getResponseCode(), "Expected HTTP 400 Bad Request response");
   }

    /**
     * Tests the handling of a POST request to create a new schedule where the end time is before the start time.
     */
    @Test
    void testHandleEndTimeBeforeStartTime() throws Exception {
        String body = "{\"course_id\":\"1\",\"weekday\":\"MONDAY\",\"start_time\":\"10:00\",\"end_time\":\"09:00\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/schedules", body);
        scheduleHandler.handle(exchange);
        assertEquals(400, exchange.getResponseCode(), "Expected HTTP 400 Bad Request response");
  }

    /**
     * Tests the handling of a POST request to create a new schedule that fails to be added to the database.
     */
    @Test
    void testHandlePostFailure() throws Exception {
        String body = "{\"course_id\":\"1\",\"weekday\":\"MONDAY\",\"start_time\":\"09:00\",\"end_time\":\"10:00\"}";
        when(mockDao.insertSchedule(1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0))).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("POST", "/schedules", body);
        scheduleHandler.handle(exchange);
        assertEquals(500, exchange.getResponseCode(), "Expected HTTP 500 Internal Server Error response");
   }

    /**
     * Tests the handling of a DELETE request to remove a schedule by its ID.
     */
    @Test
    void testHandleDeleteSchedule() throws Exception {
        Schedule schedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(schedule);
        Course course = new Course(1, 1, "Test Course", java.sql.Date.valueOf("2023-01-01"), java.sql.Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        when(mockDao.deleteSchedule(1)).thenReturn(true);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/schedules/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        scheduleHandler.handle(exchange);
        assertEquals(200, exchange.getResponseCode(), "Expected HTTP 200 OK response");
  }

    /**
     * Tests the handling of a DELETE request for a schedule that does not exist.
     */
    @Test
    void testHandleDeleteScheduleNotFound() throws Exception {
        when(mockDao.getSchedule(1)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/schedules/1", "");
        scheduleHandler.handle(exchange);
        assertEquals(404, exchange.getResponseCode(), "Expected HTTP 404 Not Found response");
   }

    /**
     * Tests the handling of a DELETE request for a schedule by an unauthorized user.
     */
    @Test
    void testHandleDeleteUnauthorized() throws Exception {
        Schedule schedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(schedule);
        Course course = new Course(1, 1, "Test Course", java.sql.Date.valueOf("2023-01-01"), java.sql.Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/schedules/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        scheduleHandler.handle(exchange);
        assertEquals(403, exchange.getResponseCode(), "Expected HTTP 403 Forbidden response");
   }

    /**
     * Tests the handling of a DELETE request that fails to remove the schedule from the database.
     */
    @Test
    void testHandleDeleteFailure() throws Exception {
        Schedule schedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(schedule);
        Course course = new Course(1, 1, "Test Course", java.sql.Date.valueOf("2023-01-01"), java.sql.Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        when(mockDao.deleteSchedule(1)).thenReturn(false);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/schedules/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        scheduleHandler.handle(exchange);
        assertEquals(500, exchange.getResponseCode(), "Expected HTTP 500 Internal Server Error response");
   }

    /**
     * Tests the handling of a PUT request to update an existing schedule.
     */
    @Test
    void testHandleUpdateSchedule() throws Exception{
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
        scheduleHandler.handle(exchange);
        assertEquals(200, exchange.getResponseCode(), "Expected HTTP 200 OK response");
    }

    /**
     * Tests the handling of a PUT request to update a schedule that does not exist.
     */
    @Test
    void testUpdateScheduleNotFound() throws Exception {
        String body = "{\"course_id\":\"1\",\"weekday\":\"TUESDAY\",\"start_time\":\"10:00\",\"end_time\":\"11:00\"}";
        when(mockDao.getSchedule(1)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        scheduleHandler.handle(exchange);
        assertEquals(404, exchange.getResponseCode(), "Expected HTTP 404 Not Found response");
   }

    /**
     * Tests the handling of a PUT request to update a schedule with a course_id that does not exist.
     */
    @Test
    void testHandleUpdateScheduleNoCourseId() throws Exception {
        String body = "{\"weekday\":\"TUESDAY\",\"start_time\":\"10:00\",\"end_time\":\"11:00\"}";
        Schedule existingSchedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(existingSchedule);
        when(mockCourseDao.getCourseById(1)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        scheduleHandler.handle(exchange);
        assertEquals(404, exchange.getResponseCode(), "Expected HTTP 404 Not Found response");
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("course not found"), "Response should indicate course not found");
    }

    /**
     * Tests the handling of a PUT request to update a schedule by an unauthorized user.
     */
    @Test
    void testUpdateScheduleUnauthorized() throws Exception {
        String body = "{\"course_id\":\"1\",\"weekday\":\"TUESDAY\",\"start_time\":\"10:00\",\"end_time\":\"11:00\"}";
        Schedule existingSchedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(existingSchedule);
        Course course = new Course(1, 1, "Test Course", java.sql.Date.valueOf("2023-01-01"), java.sql.Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        scheduleHandler.handle(exchange);
        assertEquals(403, exchange.getResponseCode(), "Expected HTTP 403 Forbidden response");
   }

    /**
     * Tests the handling of a PUT request to update a schedule with invalid JSON format.
     */
    @Test
    void handlePutInvalidJson() throws Exception {
        Schedule existingSchedule = new Schedule(1, 1, Weekday.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(existingSchedule);
        Course course = new Course(1, 1, "Test Course", Date.valueOf("2023-01-01"), Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        String body = "Invalid JSON";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        scheduleHandler.handle(exchange);
        assertEquals(400, exchange.getResponseCode(), "Expected HTTP 400 Bad Request response");
   }

    /**
     * Tests the handling of a PUT request to update a schedule with invalid time format.
     */
    @Test
    void testHandlePutInvalidTimeFormat() throws Exception {
        String body = "{\"course_id\":\"1\",\"weekday\":\"TUESDAY\",\"start_time\":\"10 AM\",\"end_time\":\"11 AM\"}";
        Schedule existingSchedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(existingSchedule);
        Course course = new Course(1, 1, "Test Course", java.sql.Date.valueOf("2023-01-01"), java.sql.Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        scheduleHandler.handle(exchange);
        assertEquals(400, exchange.getResponseCode(), "Expected HTTP 400 Bad Request response");
    }

    /**
     * Tests the handling of a PUT request to update a schedule with an invalid weekday value.
     */
    @Test
    void handlePutInvalidWeekday() throws Exception{
        String body = "{\"course_id\":\"1\",\"weekday\":\"FUNDAY\",\"start_time\":\"10:00\",\"end_time\":\"11:00\"}";
        Schedule existingSchedule = new Schedule(1, 1, Weekday.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(existingSchedule);
        Course course = new Course(1, 1, "Test Course", Date.valueOf("2023-01-01"), Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        scheduleHandler.handle(exchange);
        assertEquals(400, exchange.getResponseCode(), "Expected HTTP 400 Bad Request response");
    }

    /**
     * Tests the handling of a PUT request to update a schedule where the start time is after the end time.
     */
    @Test
    void handlePutInvalidStartTime() throws Exception {
        String body = "{\"course_id\":\"1\",\"weekday\":\"TUESDAY\",\"start_time\":\"11:00\",\"end_time\":\"10:00\"}";
        Schedule existingSchedule = new Schedule(1, 1, Weekday.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(existingSchedule);
        Course course = new Course(1, 1, "Test Course", Date.valueOf("2023-01-01"), Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        scheduleHandler.handle(exchange);
        assertEquals(400, exchange.getResponseCode(), "Expected HTTP 400 Bad Request response");
    }

    /**
     * Tests the handling of a PUT request to update a schedule that fails to be updated in the database.
     */
    @Test
    void testHandlePutFailure() throws Exception {
        String body = "{\"course_id\":\"1\",\"weekday\":\"TUESDAY\",\"start_time\":\"10:00\",\"end_time\":\"11:00\"}";
        Schedule existingSchedule = new Schedule(1, 1, model.Weekday.MONDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));
        when(mockDao.getSchedule(1)).thenReturn(existingSchedule);
        Course course = new Course(1, 1, "Test Course", java.sql.Date.valueOf("2023-01-01"), java.sql.Date.valueOf("2023-12-31"));
        when(mockCourseDao.getCourseById(1)).thenReturn(course);
        when(mockDao.updateSchedule(1, 1, Weekday.TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0)))
                .thenReturn(false);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/schedules/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        scheduleHandler.handle(exchange);
        assertEquals(500, exchange.getResponseCode(), "Expected HTTP 500 Internal Server Error response");
   }
}
