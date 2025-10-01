/**
 * Unit tests for CourseDao class.
 */
package database;

import model.Course;
import model.Student;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CourseDaoTest {
    private static CourseDao courseDao;
    private List<Integer> insertedCourses;

    /**
     * Setup DAO before all tests.
     */
    @BeforeAll
    static void setup() {
        courseDao = new CourseDao();
    }

    /**
     * Initialize the inserted id list before each test to track inserted courses for cleanup.
     */
    @BeforeEach
    void init() {
        insertedCourses = new ArrayList<>();
    }

    /**
     * Delete inserted courses after each test.
     */
    @AfterEach
    void cleanup() {
        for (int id : insertedCourses) {
            courseDao.deleteCourse(id);
        }
        insertedCourses.clear();
    }

    /**
     * Test adding and deleting a course. Deletes the inserted course after testing.
     */
    @Test
    void addAndDeleteCourseTest() {
        Course course = courseDao.addCourse(1, "Test101", Date.valueOf("2025-01-01"), Date.valueOf("2025-06-01"));
        int id = course.getCourseId();
        assertTrue(course.getCourseId() > 0, "Insertion successful, got ID: " + id);
        assertTrue(courseDao.deleteCourse(id), "Deletion successful for ID: " + id);
    }

    /**
     * Test deleting a course with a non-existent ID. The deletion should fail.
     */
    @Test
    void deleteCourseFailTest() {
        boolean result = courseDao.deleteCourse(-100); // Assuming -100 is an invalid ID
        assertFalse(result, "Deletion should fail for non-existent ID");
    }

    /**
     * Test adding a course with an invalid student ID. The insertion should fail.
     */
    @Test
    void addCourseWithInvalidStudentId() {
        Course course = courseDao.addCourse(-200, "Test101", Date.valueOf("2025-04-01"), Date.valueOf("2025-10-01"));
        assertNull(course, "Insertion should fail with invalid student ID");
    }

    /**
     * Test adding a course where the end date is before the start date. The insertion should fail.
     */
    @Test
    void addCourseWithEndDateBeforeStartDate() {
        Course course = courseDao.addCourse(1, "Test101", Date.valueOf("2025-12-01"), Date.valueOf("2025-01-01"));
        assertNull(course, "Insertion should fail when end date is before start date");
    }

    /**
     * Test adding a course with a null (empty) name. The insertion should fail.
     */
    @Test
    void addCourseWithNullName() {
        Course course = courseDao.addCourse(1, "", Date.valueOf("2025-02-01"), Date.valueOf("2025-12-01"));
        assertNull(course, "Insertion should fail with null course name");
    }

    /**
     * Test retrieving an existing course by ID. The course should be found and details should match.
     */
    @Test
    void getCourseById() {
        Course course= courseDao.addCourse(1, "Test101", Date.valueOf("2025-02-01"), Date.valueOf("2025-12-01"));
        int id = course.getCourseId();
        assertTrue(id > 0, "Insertion successful, got ID: " + id);
        insertedCourses.add(id);
        assertNotNull(course, "Course should not be null");
        assertEquals("Test101", course.getCourseName(), "Course name should match");
        assertEquals(Date.valueOf("2025-02-01"), course.getStartDate(), "Start date should match");
        assertEquals(Date.valueOf("2025-12-01"), course.getEndDate(), "End date should match");
    }

    /**
     * Test retrieving a course with an invalid ID. The result should be null.
     */
    @Test
    void getCourseByInvalidId() {
        Course course = courseDao.getCourseById(-200);
        assertNull(course, "Course should be null for non-existent ID");
    }

    /**
     * Test retrieving all courses for a specific student. The correct number of courses should be returned.
     */
    @Test
    void testGetCoursesForStudent() {
        StudentDao studentDao = new StudentDao();
        Student student = studentDao.addStudent("Test Student", "course@test.fi", "testpass");

       int id = student.getId();
       Course course1 = courseDao.addCourse(id, "Math101", Date.valueOf("2025-01-01"), Date.valueOf("2025-06-01"));
       Course course2 = courseDao.addCourse(id, "History101", Date.valueOf("2025-02-01"), Date.valueOf("2025-07-01"));
       insertedCourses.add(course1.getCourseId());
       insertedCourses.add(course2.getCourseId());

       List<Course> courses = courseDao.getAllCourses(id);
       assertNotNull(courses, "Courses list should not be null");
       assertEquals(2, courses.size(), "Should retrieve 2 courses for the student");
       studentDao.deleteStudent(id);
    }

    /**
     * Test updating an existing course. The update should be successful and details should match the new values.
     */
    @Test
    void updateCourseTest() {
        Course course  = courseDao.addCourse(1, "Test101", Date.valueOf("2025-02-01"), Date.valueOf("2025-06-01"));
        int id = course.getCourseId();
        assertTrue(id > 0, "Insertion successful, got ID: " + id);
        insertedCourses.add(id);

        boolean updated = courseDao.updateCourse(id, "Test Basics", Date.valueOf("2025-03-01"), Date.valueOf("2025-07-01"));
        assertTrue(updated, "Update should be successful");

        Course updatedCourse = courseDao.getCourseById(id);
        assertNotNull(updatedCourse, "Course should not be null after update");
        assertEquals("Test Basics", updatedCourse.getCourseName(), "Course name should be updated");
        assertEquals(Date.valueOf("2025-03-01"), updatedCourse.getStartDate(), "Start date should be updated");
        assertEquals(Date.valueOf("2025-07-01"), updatedCourse.getEndDate(), "End date should be updated");
    }

    /**
     * Test updating a course with end date before start date. The update should fail.
     */
    @Test
    void updateCourseWithInvalidDatesTest() {
        Course course = courseDao.addCourse(1, "Test101", Date.valueOf("2025-02-01"), Date.valueOf("2025-06-01"));
        int id = course.getCourseId();
        assertTrue(id > 0, "Insertion successful, got ID: " + id);
        insertedCourses.add(id);

        boolean updated = courseDao.updateCourse(id, "Test101", Date.valueOf("2025-07-01"), Date.valueOf("2025-06-01"));
        assertFalse(updated, "Update should fail when end date is before start date");
    }

    /**
     * Test updating a course with a null (empty) name. The update should pass as partial updates are allowed.
     */
    @Test
    void updateCourseWithNullNameTest() {
        Course course = courseDao.addCourse(1, "Test101", Date.valueOf("2025-02-01"), Date.valueOf("2025-06-01"));
        int id = course.getCourseId();
        assertTrue(id > 0, "Insertion successful, got ID: " + id);
        insertedCourses.add(id);

        boolean updated = courseDao.updateCourse(id, null, Date.valueOf("2025-03-01"), Date.valueOf("2025-07-01"));
        assertTrue(updated, "Update should fail with null course name");
    }

    /**
     * Test updating a non-existent course. The update should fail.
     */
    @Test
    void updateNonExistentCourseTest() {
        boolean updated = courseDao.updateCourse(-300, "NonExistent", Date.valueOf("2025-03-01"), Date.valueOf("2025-07-01"));
        assertFalse(updated, "Update should fail for non-existent course ID");
    }

    /**
     * Test updating a course with null start and end dates. The update should pass as partial updates are allowed.
     */
    @Test
    void updateCourseTestWithNulLDatesTest() {
        Course course = courseDao.addCourse(1, "Test101", Date.valueOf("2025-02-01"), Date.valueOf("2025-06-01"));
        int id = course.getCourseId();
        assertTrue(id > 0, "Insertion successful, got ID: " + id);
        insertedCourses.add(id);

        boolean updated = courseDao.updateCourse(id, "Test101", null, null);
        assertTrue(updated, "Update should fail with null dates");
    }

}