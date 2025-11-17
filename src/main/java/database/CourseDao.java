package database;

import model.Course;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Course entity. Provides methods to add and retrieve courses from the database.
 */
public class CourseDao {
    /**
     * Logger for logging errors and information.
     */
    private static final Logger LOGGER = Logger.getLogger(CourseDao.class.getName());

    /**
     * Adds a new course to the database. Returns the generated course ID, or -1 if insertion fails.
     *
     * @param studentId  The ID of the student associated with the course.
     * @param courseName The name of the course.
     * @param startDate  The start date of the course.
     * @param endDate    The end date of the course.
     */
    public Course addCourse(int studentId, String courseName, Date startDate, Date endDate) {
        if (startDate != null && endDate != null && endDate.before(startDate)) {
            return null;
        }
        if (courseName == null || courseName.isEmpty()) {
            return null;
        }
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "INSERT INTO courses (student_id, course_name, start_date, end_date) VALUES (?, ?, ?, ?);";
        Course result = null;
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setInt(1, studentId);
            preparedStatement.setString(2, courseName);
            preparedStatement.setDate(3, startDate);
            preparedStatement.setDate(4, endDate);
            final int rows = preparedStatement.executeUpdate();
            if (rows > 0) {
                try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                    if (resultSet.next()) {
                        final int newId = resultSet.getInt(1);
                        result = new Course(newId, studentId, courseName, startDate, endDate);
                    }
                }
            }
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to add course: " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Retrieves a course by its ID. Returns a Course object if found, or null if not found or an error occurs.
     *
     * @param courseId The ID of the course to retrieve.
     * @return
     */
    public Course getCourseById(final int courseId) {
        Course result = null;
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT course_id, student_id, course_name, start_date, end_date FROM courses WHERE course_id = ?;";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

            preparedStatement.setInt(1, courseId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    result = new Course(
                            resultSet.getInt("course_id"),
                            resultSet.getInt("student_id"),
                            resultSet.getString("course_name"),
                            resultSet.getDate("start_date"),
                            resultSet.getDate("end_date")
                    );
                }
            }

        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get course by ID: " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Retrieves all courses for a given student ID.
     *
     * @param studentId The ID of the student whose courses to retrieve.
     * @return List<Course>
     */
    public List<Course> getAllCourses(final int studentId) {
        final List<Course> courses = new ArrayList<>();
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT course_id, student_id, course_name, start_date, end_date FROM courses WHERE student_id = ?;";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

            preparedStatement.setInt(1, studentId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    final int courseId = resultSet.getInt("course_id");
                    final int studentIdFromDb = resultSet.getInt("student_id");
                    final String courseName = resultSet.getString("course_name");
                    final Date startDate = resultSet.getDate("start_date");
                    final Date endDate = resultSet.getDate("end_date");
                    courses.add(new Course(courseId, studentIdFromDb, courseName, startDate, endDate));
                }
            }
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get all courses: " + e.getMessage());
            }
        }
        return courses;
    }

    /**
     * Deletes a course by its ID. Also deletes associated schedules.
     *
     * @param courseId The ID of the course to delete.
     * @return boolean
     */
    public boolean deleteCourse(final int courseId) {
        boolean success = false;
        final Connection conn = MariaDBConnection.getConnection();
        final String sqlSchedules = "DELETE FROM schedule WHERE course_id = ?;";
        final String sqlCourse = "DELETE FROM courses WHERE course_id = ?;";

        try (PreparedStatement psSchedules = conn.prepareStatement(sqlSchedules);
             PreparedStatement psCourse = conn.prepareStatement(sqlCourse)) {
            psSchedules.setInt(1, courseId);
            psSchedules.executeUpdate();
            psCourse.setInt(1, courseId);
            final int rows = psCourse.executeUpdate();
            success = rows > 0;

        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to delete course: " + e.getMessage());
            }
        }
        return success;
    }

    /**
     * Updates an existing course with new values. If a parameter is null, the existing value is retained.
     *
     * @param courseId The ID of the course to update.
     * @param courseName The new name of the course (or null to keep existing).
     * @param startDate The new start date of the course (or null to keep existing).
     * @param endDate The new end date of the course (or null to keep existing).
     * @return boolean indicating success or failure of the update.
     */
    public boolean updateCourse(int courseId, String courseName, Date startDate, Date endDate) {
        final Course existingCourse = getCourseById(courseId);
        if (existingCourse == null) return false;

        final String finalCourseName = resolveCourseName(courseName, existingCourse);
        final Date finalStartDate = resolveStartDate(startDate, existingCourse);
        final Date finalEndDate = resolveEndDate(endDate, existingCourse);

        return isValidDateRange(finalStartDate, finalEndDate) &&
                executeUpdate(courseId, finalCourseName, finalStartDate, finalEndDate);
    }

    /** Helper method for updateCourse */
    private String resolveCourseName(String courseName, Course existingCourse) {
        return courseName != null ? courseName : existingCourse.getCourseName();
    }

    /** Helper method for updateCourse */
    private Date resolveStartDate(Date startDate, Course existingCourse) {
        if (startDate != null) {return startDate;}
        if (existingCourse.getStartDate() != null) {return new Date(existingCourse.getStartDate().getTime());}
        return null;
    }

    /** Helper method for updateCourse */
    private Date resolveEndDate(Date endDate, Course existingCourse) {
        if (endDate != null) {return endDate;}
        if (existingCourse.getEndDate() != null) {return new Date(existingCourse.getEndDate().getTime());}
        return null;
    }

    /** Helper method for updateCourse */
    private boolean isValidDateRange(Date startDate, Date endDate) {
        return startDate != null && endDate != null && !endDate.before(startDate);
    }

    /** Helper method for updateCourse */
    private boolean executeUpdate(int courseId, String courseName, Date startDate, Date endDate) {
        final String sql = "UPDATE courses SET course_name = ?, start_date = ?, end_date = ? WHERE course_id = ?;";
        final Connection conn = MariaDBConnection.getConnection();
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, courseName);
            preparedStatement.setDate(2, startDate);
            preparedStatement.setDate(3, endDate);
            preparedStatement.setInt(4, courseId);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to update course: " + e.getMessage());
            }
            return false;
        }
    }
}
