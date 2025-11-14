package server;

import database.CourseDao;
import model.Course;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.testutils.MockHttpExchange;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseHandlerTest {
    private CourseHandler courseHandler;
    private CourseDao mockDao;

    /**
     * Setup before each test by creating a mock DAO and injecting it into the handler.
     */
    @BeforeEach
    public void setup() {
        this.mockDao = mock(CourseDao.class);
        this.courseHandler = new CourseHandler(mockDao);
    }

    /**
     * Test fetching a course by ID successfully.
     */
    @Test
    void testGetCourseById() throws Exception {
        Course course = new Course(1, 1, "Test 101", Date.valueOf("2023-09-01"), Date.valueOf("2024-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/courses/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(200, exchange.getResponseCode(), "Expected HTTP 200 OK");
    }

    /**
     * Test fetching a course by ID when the user is not authorized (different student).
     */
    @Test
    void testGetCourseUnauthorized() throws Exception {
        Course course = new Course(1, 1, "Test 101", Date.valueOf("2026-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/courses/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(403, exchange.getResponseCode(), "Expected HTTP 403 Forbidden");
    }

    /**
     * Test fetching a course by ID when the course does not exist.
     */
    @Test
    void testCourseNotFound() throws Exception {
        when(mockDao.getCourseById(999)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/courses/999", "");
        exchange.withHeader("student_id", "1").withHeader("role", "admin");
        courseHandler.handle(exchange);
        assertEquals(404, exchange.getResponseCode(), "");
    }

    /**
     * Test fetching all courses for a student successfully.
     */
    @Test
    void testGetCoursesForStudent() throws Exception {
        Course course1 = new Course(1, 1, "Test 101", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        Course course2 = new Course(2, 1, "Test 102", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        List<Course> courses = new ArrayList<>();
        courses.add(course1);
        courses.add(course2);
        when(mockDao.getAllCourses(1)).thenReturn(courses);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/courses/students/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(200, exchange.getResponseCode(), "Expected HTTP 200 OK");
    }

    /**
     * Test fetching all courses for a student when the user is not authorized (different student).
     */
    @Test
    void testGetCoursesForStudentUnauthorized() throws Exception {
        MockHttpExchange exchange = new MockHttpExchange("GET", "/courses/students/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(403, exchange.getResponseCode(), "Expected HTTP 403 Forbidden");
    }

    /**
     * Test fetching all courses for a student when no courses exist.
     */
    @Test
    void testNoCoursesForStudent() throws Exception {
        when(mockDao.getAllCourses(1)).thenReturn(new ArrayList<>());
        MockHttpExchange exchange = new MockHttpExchange("GET", "/courses/students/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(404, exchange.getResponseCode(), "Expected HTTP 404 Not Found");
    }

    /**
     * Test creating a new course successfully.
     */
    @Test
    void testPostCourse() throws Exception {
        String body = "{\"course_name\":\"New Test Course\",\"start_date\":\"2025-09-01\",\"end_date\":\"2026-05-31\"}";
        Course newCourse = new Course(1, 1, "New Test Course", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.addCourse(1, "New Test Course", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"))).thenReturn(newCourse);
        MockHttpExchange exchange = new MockHttpExchange("POST", "/courses", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(201, exchange.getResponseCode(), "Expected HTTP 201 Created");
    }

    /**
     * Test creating a new course with missing required fields.
     */
    @Test
    void testPostCourseMissingFields() throws Exception {
        String body = "{\"course_name\":\"New Test Course\",\"start_date\":\"2025-09-01\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/courses", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(400, exchange.getResponseCode(), "Expected HTTP 400 Bad Request");
    }

    /**
     * Test creating a new course with invalid date format.
     */
    @Test
    void testPostCourseInvalidDate() throws Exception {
        String body = "{\"course_name\":\"New Test Course\",\"start_date\":\"2025-09-01\",\"end_date\":\"2026-31-05\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/courses", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(400, exchange.getResponseCode(), "Expected HTTP 400 Bad Request");
    }

    /**
     * Test creating a new course where the start date is after the end date.
     */
    @Test
    void testPostCourseStartDateAfterEndDate() throws Exception {
        String body = "{\"course_name\":\"New Test Course\",\"start_date\":\"2026-09-01\",\"end_date\":\"2025-05-31\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/courses", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(500, exchange.getResponseCode(), "Expected HTTP 500 Internal Server Error");
    }

    /**
     * Test deleting a course successfully.
     */
    @Test
    void testDeleteCourse() throws Exception {
        Course course = new Course(1, 1, "Test 101", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);
        when(mockDao.deleteCourse(1)).thenReturn(true);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/courses/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(200, exchange.getResponseCode(), "Expected HTTP 200 OK");
     }

    /**
     * Test deleting a course when the user is not authorized (different student).
     */
    @Test
    void testDeleteCourseUnauthorized() throws Exception {
        Course course = new Course(1, 1, "Test 101", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/courses/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(403, exchange.getResponseCode(), "Expected HTTP 403 Forbidden");
    }

    /**
     * Test deleting a course that does not exist.
     */
    @Test
    void testDeleteCourseNotFound() throws Exception {
        when(mockDao.getCourseById(999)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/courses/999", "");
        exchange.withHeader("student_id", "1").withHeader("role", "admin");
        courseHandler.handle(exchange);
        assertEquals(404, exchange.getResponseCode(), "Expected HTTP 404 Not Found");
    }

    /**
     * Test updating a course successfully.
     */
    @Test
    void testUpdateCourse() throws Exception{
        Course course = new Course(1, 1, "Old Course", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);
        when(mockDao.updateCourse(1, "Updated Course", Date.valueOf("2025-10-01"), Date.valueOf("2026-06-30")))
                .thenAnswer(invocation -> {
                    course.setCourseName("Updated Course");
                    course.setStartDate(Date.valueOf("2025-10-01"));
                    course.setEndDate(Date.valueOf("2026-06-30"));
                    return true;
                });

        String body = "{\"course_name\":\"Updated Course\",\"start_date\":\"2025-10-01\",\"end_date\":\"2026-06-30\"}";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/courses/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(200, exchange.getResponseCode(), "Expected HTTP 200 OK");
    }

    /**
     * Test updating a course that does not exist.
     */
    @Test
    void testUpdateCourseMissingCourse() throws Exception {
        when(mockDao.getCourseById(1)).thenReturn(null);

        String body = "{\"course_name\":\"Updated Course\",\"start_date\":\"2025-10-01\",\"end_date\":\"2026-06-30\"}";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/courses/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(404, exchange.getResponseCode(), "Expected HTTP 404 Not Found");
    }

    /**
     * Test updating a course when the user is not authorized (different student).
     */
    @Test
    void testUpdateCourseUnauthorized() throws Exception {
        Course course = new Course(1, 1, "Old Course", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);

        String body = "{\"course_name\":\"Updated Course\",\"start_date\":\"2025-10-01\",\"end_date\":\"2026-06-30\"}";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/courses/1", body);
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(403, exchange.getResponseCode(), "Expected HTTP 403 Forbidden");
   }

    /**
     * Test updating a course with invalid date format.
     */
    @Test
    void testUpdateCourseInvalidDate() throws Exception {
        Course course = new Course(1, 1, "Old Course", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);

        String body = "{\"course_name\":\"Updated Course\",\"start_date\":\"2025-10-01\",\"end_date\":\"2026-31-06\"}";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/courses/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(400, exchange.getResponseCode(), "Expected HTTP 400 Bad Request");
    }

    /**
     * Test updating a course where the start date is after the end date.
     */
    @Test
    void testUpdateCourseStartDateAfterEndDate() throws Exception {
        Course course = new Course(1, 1, "Old Course", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);

        String body = "{\"course_name\":\"Updated Course\",\"start_date\":\"2026-10-01\",\"end_date\":\"2025-06-30\"}";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/courses/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(500, exchange.getResponseCode(), "Expected HTTP 500 Internal Server Error");
     }

    /**
     * Test updating a course when the DAO update operation fails.
     */
    @Test
    void testUpdateCourseFailure() throws Exception{
        Course course = new Course(1, 1, "Old Course", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);
        when(mockDao.updateCourse(1, "Updated Course", Date.valueOf("2025-10-01"), Date.valueOf("2026-06-30")))
                .thenReturn(false);

        String body = "{\"course_name\":\"Updated Course\",\"start_date\":\"2025-10-01\",\"end_date\":\"2026-06-30\"}";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/courses/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        courseHandler.handle(exchange);
        assertEquals(500, exchange.getResponseCode(), "Expected HTTP 500 Internal Server Error");
   }

}
