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

public class CourseHandlerTest {
    private CourseHandler courseHandler;
    private CourseDao mockDao;

    @BeforeEach
    public void setup() {
        this.mockDao = mock(CourseDao.class);
        this.courseHandler = new CourseHandler(mockDao);
    }

    @Test
    public void testGetCourseById() {
        Course course = new Course(1, 1, "Test 101", Date.valueOf("2023-09-01"), Date.valueOf("2024-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/courses/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test course by id: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Test 101"));
    }

    @Test
    public void testGetCourseUnauthorized() {
        Course course = new Course(1, 1, "Test 101", Date.valueOf("2026-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/courses/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test unauthorized: " + e.getMessage());
        }
        assertEquals(403, exchange.getResponseCode());
    }

    @Test
    public void testCourseNotFound() {
        when(mockDao.getCourseById(999)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/courses/999", "");
        exchange.withHeader("student_id", "1").withHeader("role", "admin");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test course not found: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
    }

    @Test
    public void testGetCoursesForStudent() {
        Course course1 = new Course(1, 1, "Test 101", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        Course course2 = new Course(2, 1, "Test 102", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        ArrayList<Course> courses = new ArrayList<>();
        courses.add(course1);
        courses.add(course2);
        when(mockDao.getAllCourses(1)).thenReturn(courses);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/courses/students/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test get courses for student: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Test 101"));
        assertTrue(json.contains("Test 102"));
    }

    @Test
    public void testGetCoursesForStudentUnauthorized() {
        MockHttpExchange exchange = new MockHttpExchange("GET", "/courses/students/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test get courses for student unauthorized: " + e.getMessage());
        }
        assertEquals(403, exchange.getResponseCode());
    }

    @Test
    public void testNoCoursesForStudent() {
        when(mockDao.getAllCourses(1)).thenReturn(new ArrayList<>());
        MockHttpExchange exchange = new MockHttpExchange("GET", "/courses/students/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test no courses for student: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
    }

    @Test
    public void testPostCourse(){
        String body = "{\"course_name\":\"New Test Course\",\"start_date\":\"2025-09-01\",\"end_date\":\"2026-05-31\"}";
        Course newCourse = new Course(1, 1, "New Test Course", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.addCourse(1, "New Test Course", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"))).thenReturn(newCourse);
        MockHttpExchange exchange = new MockHttpExchange("POST", "/courses", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test post course: " + e.getMessage());
        }
        assertEquals(201, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("New Test Course"));
    }

    @Test
    public void testPostCourseMissingFields(){
        String body = "{\"course_name\":\"New Test Course\",\"start_date\":\"2025-09-01\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/courses", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test post course missing fields: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
    }

    @Test
    public void testPostCourseInvalidDate(){
        String body = "{\"course_name\":\"New Test Course\",\"start_date\":\"2025-09-01\",\"end_date\":\"2026-31-05\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/courses", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test post course invalid date: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
    }

    @Test
    public void testPostCourseStartDateAfterEndDate(){
        String body = "{\"course_name\":\"New Test Course\",\"start_date\":\"2026-09-01\",\"end_date\":\"2025-05-31\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/courses", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test start date after end date: " + e.getMessage());
        }
        assertEquals(500, exchange.getResponseCode());
    }

    @Test
    public void testDeleteCourse(){
        Course course = new Course(1, 1, "Test 101", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);
        when(mockDao.deleteCourse(1)).thenReturn(true);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/courses/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test delete course: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Course deleted successfully"));
    }

    @Test
    public void testDeleteCourseUnauthorized(){
        Course course = new Course(1, 1, "Test 101", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/courses/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test delete course unauthorized: " + e.getMessage());
        }
        assertEquals(403, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Forbidden"));
    }

    @Test
    public void testDeleteCourseNotFound(){
        when(mockDao.getCourseById(999)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/courses/999", "");
        exchange.withHeader("student_id", "1").withHeader("role", "admin");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test delete course not found: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Course not found"));
    }

    @Test
    public void testUpdateCourse() {
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
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test update course: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Updated"));
    }

    @Test
    public void testUpdateCourseMissingCourse() {
        when(mockDao.getCourseById(1)).thenReturn(null);

        String body = "{\"course_name\":\"Updated Course\",\"start_date\":\"2025-10-01\",\"end_date\":\"2026-06-30\"}";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/courses/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test update course missing course: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Course not found"));
    }

    @Test
    public void testUpdateCourseUnauthorized() {
        Course course = new Course(1, 1, "Old Course", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);

        String body = "{\"course_name\":\"Updated Course\",\"start_date\":\"2025-10-01\",\"end_date\":\"2026-06-30\"}";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/courses/1", body);
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test update course unauthorized: " + e.getMessage());
        }
        assertEquals(403, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Forbidden"));
    }

    @Test
    public void testUpdateCourseInvalidDate() {
        Course course = new Course(1, 1, "Old Course", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);

        String body = "{\"course_name\":\"Updated Course\",\"start_date\":\"2025-10-01\",\"end_date\":\"2026-31-06\"}";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/courses/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test update course invalid date: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Invalid date format"));
    }

    @Test
    public void testUpdateCourseStartDateAfterEndDate() {
        Course course = new Course(1, 1, "Old Course", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);

        String body = "{\"course_name\":\"Updated Course\",\"start_date\":\"2026-10-01\",\"end_date\":\"2025-06-30\"}";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/courses/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test update course start date after end date: " + e.getMessage());
        }
        assertEquals(500, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Failed to update course"));
    }

    @Test
    public void testUpdateCourseFailure() {
        Course course = new Course(1, 1, "Old Course", Date.valueOf("2025-09-01"), Date.valueOf("2026-05-31"));
        when(mockDao.getCourseById(1)).thenReturn(course);
        when(mockDao.updateCourse(1, "Updated Course", Date.valueOf("2025-10-01"), Date.valueOf("2026-06-30")))
                .thenReturn(false);

        String body = "{\"course_name\":\"Updated Course\",\"start_date\":\"2025-10-01\",\"end_date\":\"2026-06-30\"}";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/courses/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            courseHandler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in test update course failure: " + e.getMessage());
        }
        assertEquals(500, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Failed to update course"));
    }

}
