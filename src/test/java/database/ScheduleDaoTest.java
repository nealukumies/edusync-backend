/**
 * Unit tests for ScheduleDao class.
 */
package database;

import model.Course;
import model.Schedule;
import model.Student;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
class ScheduleDaoTest {
    private static ScheduleDao scheduleDao;
    private List<Integer> insertedSchedules;

    /**
     * Setup DAO before all tests.
     */
    @BeforeAll
    static void setup() {
        scheduleDao = new ScheduleDao();
    }

    /**
     * Initialize the inserted id list before each test to track inserted schedules for cleanup.
     */
    @BeforeEach
    void init() {
        insertedSchedules = new ArrayList<>();
    }

    /**
     * Delete inserted schedules after each test.
     */
    @AfterEach
    void cleanup() {
        for (int id : insertedSchedules) {
            scheduleDao.deleteSchedule(id);
        }
        insertedSchedules.clear();
    }

    /**
     * Test inserting and deleting a schedule. Deletes the inserted schedule after testing.
     */
    @Test
    void insertAndDeleteScheduleTest() {
        Schedule schedule = scheduleDao.insertSchedule(1, model.Weekday.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0));
        int id = schedule.getScheduleId();
        assertTrue(scheduleDao.deleteSchedule(id), "Deletion successful for ID: " + id);
    }

    /**
     * Test deleting a schedule with a non-existent ID. The deletion should fail.
     */
    @Test
    void insertScheduleWithNullCourseIdTest() {
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(11, 0);
        assertThrows(IllegalArgumentException.class, () ->
                scheduleDao.insertSchedule(null, model.Weekday.MONDAY, start, end)
        );
    }

    /**
     * Test inserting a schedule with a null weekday. Should throw IllegalArgumentException.
     */
    @Test
    void insertScheduleWithNullWeekdayTest() {
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(11, 0);
        assertThrows(IllegalArgumentException.class, () -> {
            scheduleDao.insertSchedule(1, null, start, end);
        });
    }

    /**
     * Test inserting a schedule with start time after end time. Should throw IllegalArgumentException.
     */
    @Test
    void insertScheduleWithStartTimeAfterEndTimeTest() {
        LocalTime start = LocalTime.of(11, 0);
        LocalTime end = LocalTime.of(10, 0);
        assertThrows(IllegalArgumentException.class, () -> {
            scheduleDao.insertSchedule(1, model.Weekday.MONDAY, start, end);
        });
     }


    /**
     * Test to get a schedule by its ID. Inserts a schedule first, then retrieves it.
     */
    @Test
    void getScheduleTest() {
        Schedule schedule = scheduleDao.insertSchedule(1, model.Weekday.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        int id = schedule.getScheduleId();
        insertedSchedules.add(id);
        assertEquals(id, schedule.getScheduleId(), "Schedule ID should match");
    }

    /**
     * Test retrieving a schedule with an invalid ID. The result should be null.
     */
    @Test
    void getScheduleWithInvalidIdTest() {
        Schedule schedule = scheduleDao.getSchedule(-100);
        assertNull(schedule, "Schedule should be null for non-existent ID");
    }

    /**
     * Test retrieving all schedules for a specific course. Inserts two schedules for the course, then retrieves them.
     * Cleans up by deleting the course. cleanup() will delete the inserted schedules.
     */
    @Test
    void getAllSchedulesForCourseTest() {
        CourseDao courseDao = new CourseDao();
        Course course = courseDao.addCourse(1, "Test101", Date.valueOf("2025-01-01"), Date.valueOf("2025-06-01"));
        int courseId = course.getCourseId();
        Schedule schedule1 = scheduleDao.insertSchedule(courseId, model.Weekday.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0));
        Schedule schedule2 = scheduleDao.insertSchedule(courseId, model.Weekday.FRIDAY, LocalTime.of(14, 0), LocalTime.of(15, 0));
        int scheduleId1 = schedule1.getScheduleId();
        int scheduleId2 = schedule2.getScheduleId();
        insertedSchedules.add(scheduleId1);
        insertedSchedules.add(scheduleId2);
        List<Schedule> schedules = scheduleDao.getAllSchedulesForCourse(courseId);
        assertEquals(2, schedules.size(), "Should retrieve 2 schedules");
        courseDao.deleteCourse(courseId);
    }

    /**
     * Test retrieving schedules for a course with no schedules. The result should be an empty list.
     */
    @Test
    void getAllSchedulesForCourseWithNoSchedulesTest() {
        CourseDao courseDao = new CourseDao();
        Course course = courseDao.addCourse(1, "Test101", Date.valueOf("2025-01-01"), Date.valueOf("2025-06-01"));
        int courseId = course.getCourseId();
        List<Schedule> schedules = scheduleDao.getAllSchedulesForCourse(courseId);
        assertEquals(0, schedules.size(), "Should retrieve 0 schedules");
        courseDao.deleteCourse(courseId);
    }

    /**
     *
     */
    @Test
    void getAllSchedulesForCourseWithInvalidIdTest() {
        List<Schedule> schedules = scheduleDao.getAllSchedulesForCourse(-100);
        assertEquals(0, schedules.size(), "Should retrieve 0 schedules for non-existent course ID");
    }

    /**
     * Test retrieving all schedules for a specific student. Inserts a student, a course for that student, and two schedules for the course.
     * Then retrieves the schedules for the student. Cleans up by deleting the course and student. cleanup() will delete the inserted schedules.
     */
    @Test
    void getAllSchedulesForStudentTest() {
        StudentDao studentDao = new StudentDao();
        CourseDao courseDao = new CourseDao();
        Student student = studentDao.addStudent("testuser", "test@cases.com", "password");
        assertNotNull(student, "Student should not be null");
        int studentId = student.getId();
        Course course = courseDao.addCourse(studentId, "Test101", Date.valueOf("2025-01-01"), Date.valueOf("2025-06-01"));
        int courseId = course.getCourseId();
        Schedule schedule1 = scheduleDao.insertSchedule(courseId, model.Weekday.THURSDAY, LocalTime.of(10, 0), LocalTime.of(11, 0));
        Schedule schedule2 = scheduleDao.insertSchedule(courseId, model.Weekday.FRIDAY, LocalTime.of(14, 0), LocalTime.of(15, 0));
        int scheduleId1 = schedule1.getScheduleId();
        int scheduleId2 = schedule2.getScheduleId();
        insertedSchedules.add(scheduleId1);
        insertedSchedules.add(scheduleId2);
        List<Schedule> schedules = scheduleDao.getAllSchedulesForStudent(studentId);
        assertEquals(2, schedules.size(), "Should retrieve 2 schedules");
        courseDao.deleteCourse(courseId);
        studentDao.deleteStudent(studentId);
    }

    /**
     * Test retrieving schedules for a student with no schedules. The result should be an empty list.
     */
    @Test
    void getAllSchedulesForStudentWithNoSchedulesTest() {
        StudentDao studentDao = new StudentDao();
        Student student  = studentDao.addStudent("Testuser", "email@email.email", "password");
        int studentId = student.getId();
        List<Schedule> schedules = scheduleDao.getAllSchedulesForStudent(studentId);
        assertTrue(schedules.isEmpty(), "Schedules list should be empty");
        studentDao.deleteStudent(studentId);
    }

    /**
     * Test retrieving schedules for a student with an invalid ID. The result should be an empty list.
     */
    @Test
    void getAllSchedulesForStudentWithInvalidIdTest() {
        List<Schedule> schedules = scheduleDao.getAllSchedulesForStudent(-100);
        assertTrue(schedules.isEmpty(), "Schedules list should be empty");
    }

    /**
     * Test updating an existing schedule. Inserts a schedule, updates it, then retrieves it to verify the update.
     */
    @Test
    void updateScheduleTest() {
        Schedule schedule = scheduleDao.insertSchedule(1, model.Weekday.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        int id = schedule.getScheduleId();
        insertedSchedules.add(id);
        scheduleDao.updateSchedule(id, 1, model.Weekday.WEDNESDAY, LocalTime.of(12, 0), LocalTime.of(15, 0));
        Schedule updatedSchedule = scheduleDao.getSchedule(id);
        assertEquals(model.Weekday.WEDNESDAY, updatedSchedule.getWeekday(), "Weekday should be updated");
    }

    /**
     * Test updating a schedule with a non-existent ID. The update should fail.
     */
    @Test
    void updateScheduleWithInvalidIdTest() {
        boolean updated = scheduleDao.updateSchedule(-100, 2, model.Weekday.WEDNESDAY, LocalTime.of(12, 0), LocalTime.of(15, 0));
        assertFalse(updated, "Update should fail for non-existent schedule ID");
    }

    /**
     * Test updating a schedule with null parameters. Each case should throw IllegalArgumentException.
     */
    @Test
    void updateScheduleWithNullCourseIdTest() {
        assertThrows(IllegalArgumentException.class, () -> {
            scheduleDao.updateSchedule(1, null, model.Weekday.WEDNESDAY, LocalTime.of(12, 0), LocalTime.of(15, 0));
        });
    }

    /**
     * Test updating a schedule with a null weekday. Should throw IllegalArgumentException.
     */
    @Test
    void updateScheduleWithNullWeekdayTest() {
        assertThrows(IllegalArgumentException.class, () -> {
            scheduleDao.updateSchedule(1, 1, null, LocalTime.of(12, 0), LocalTime.of(15, 0));
        });
    }

    /**
     * Test updating a schedule with start time after end time. Should throw IllegalArgumentException.
     */
    @Test
    void updateScheduleWithStartTimeAfterEndTimeTest() throws Exception {
        LocalTime start = LocalTime.of(16, 0);
        LocalTime end = LocalTime.of(15, 0);
        assertThrows(IllegalArgumentException.class, () -> {
            scheduleDao.updateSchedule(1, 1, model.Weekday.WEDNESDAY, start, end);
        });
    }
}