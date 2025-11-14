/**
 * Data Access Object for Course entity. Provides methods to add and retrieve courses from the database.
 */
package database;

import model.Course;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("PMD.AtLeastOneConstructor")
public class CourseDao {
    private static final Logger LOGGER = Logger.getLogger(CourseDao.class.getName());

    /**
     * Adds a new course to the database. Returns the generated course ID, or -1 if insertion fails.
     * @param studentId
     * @param courseName
     * @param startDate
     * @param endDate
     */
    @SuppressWarnings("PMD.MethodArgumentCouldBeFinal")
    public Course addCourse(int studentId, String courseName, Date startDate, Date endDate) {
        if (startDate != null && endDate != null && endDate.before(startDate)) {
            return null;
        }
        if (courseName == null || courseName.isEmpty()) {
            return null;
        }
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "INSERT INTO courses (student_id, course_name, start_date, end_date) VALUES (?, ?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){

            ps.setInt(1, studentId);
            ps.setString(2, courseName);
            ps.setDate(3, startDate);
            ps.setDate(4, endDate);
            final int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        final int newId = rs.getInt(1);
                        return new Course(newId, studentId, courseName, startDate, endDate);
                    }
                }
            } return null; // Indicate failure

        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to add course: " + e.getMessage());
            }
            return null; // Indicate failure
        }
    }

    /**
     * Retrieves a course by its ID. Returns a Course object if found, or null if not found or an error occurs.
     * @param courseId
     * @return
     */
    public Course getCourseById(final int courseId) {
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT * FROM courses WHERE course_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Course(
                            rs.getInt("course_id"),
                            rs.getInt("student_id"),
                            rs.getString("course_name"),
                            rs.getDate("start_date"),
                            rs.getDate("end_date")
                    );
                }
            }
            return null;
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get course by ID: " + e.getMessage());
            }
            return null;
        }
    }

    public List<Course> getAllCourses(final int studentId) {
        final List<Course> courses = new ArrayList<>();
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT * FROM courses WHERE student_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final int courseId = rs.getInt("course_id");
                    final int studentIdFromDb = rs.getInt("student_id");
                    final String courseName = rs.getString("course_name");
                    final Date startDate = rs.getDate("start_date");
                    final Date endDate = rs.getDate("end_date");
                    courses.add(new Course(courseId, studentIdFromDb, courseName, startDate, endDate));
                }
            }
            return courses;
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get all courses: " + e.getMessage());
            }
            return courses;
        }
    }

    /**
     * Deletes a course by its ID. Also deletes associated schedules.
     * Returns true if deletion was successful, false otherwise.
     * @param courseId
     * @return boolean
     */
    public boolean deleteCourse(final int courseId) {
        final Connection conn = MariaDBConnection.getConnection();
        final String sqlSchedules = "DELETE FROM schedule WHERE course_id = ?;";
        final String sqlCourse = "DELETE FROM courses WHERE course_id = ?;";

        try (PreparedStatement psSchedules = conn.prepareStatement(sqlSchedules);
             PreparedStatement psCourse = conn.prepareStatement(sqlCourse)) {
            psSchedules.setInt(1, courseId);
            psSchedules.setInt(1, courseId);
            psSchedules.executeUpdate();
            psCourse.setInt(1, courseId);
            final int rows = psCourse.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to delete course: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Updates an existing course with new values. Only non-null parameters are updated.
     * Returns true if the update was successful, false otherwise.
     * @param courseId
     * @param courseName
     * @param startDate
     * @param endDate
     * @return
     */
    @SuppressWarnings("PMD.MethodArgumentCouldBeFinal")
    public boolean updateCourse(int courseId, String courseName, Date startDate, Date endDate) {
        final Course existingCourse = getCourseById(courseId);
        if (existingCourse == null) {return false;}

        final String finalCourseName = courseName != null ? courseName : existingCourse.getCourseName();

        final Date finalStartDate;
        if (startDate != null) {
            finalStartDate = startDate;
        } else if (existingCourse.getStartDate() != null) {
            finalStartDate = new Date(existingCourse.getStartDate().getTime());
        } else {
            finalStartDate = null;
        }

        final Date finalEndDate;
        if (endDate != null) {
            finalEndDate = endDate;
        } else if (existingCourse.getEndDate() != null) {
            finalEndDate = new Date(existingCourse.getEndDate().getTime());
        } else {
            finalEndDate = null;
        }

        if (finalStartDate != null && finalEndDate != null && finalEndDate.before(finalStartDate)) {
            return false;
        }

        final String sql = "UPDATE courses SET course_name = ?, start_date = ?, end_date = ? WHERE course_id = ?;";
        final Connection conn = MariaDBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, finalCourseName);
            ps.setDate(2, finalStartDate);
            ps.setDate(3, finalEndDate);
            ps.setInt(4, courseId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to update course: " + e.getMessage());
            }
            return false;
        }
    }
}
