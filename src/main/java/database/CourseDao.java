/**
 * Data Access Object for Course entity. Provides methods to add and retrieve courses from the database.
 */
package database;

import model.Course;

import java.sql.*;
import java.util.ArrayList;

public class CourseDao {

    /**
     * Adds a new course to the database. Returns the generated course ID, or -1 if insertion fails.
     * @param studentId
     * @param courseName
     * @param startDate
     * @param endDate
     */
    public Course addCourse(int studentId, String courseName, Date startDate, Date endDate) {
        if (startDate != null && endDate != null && endDate.before(startDate)) {
            System.out.println("Error: End date cannot be before start date.");
            return null;
        }
        if (courseName == null || courseName.isEmpty()) {
            System.out.println("Error: Course name cannot be null or empty.");
            return null;
        }
        Connection conn = MariaDBConnection.getConnection();
        String sql = "INSERT INTO courses (student_id, course_name, start_date, end_date) VALUES (?, ?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){

            ps.setInt(1, studentId);
            ps.setString(2, courseName);
            ps.setDate(3, startDate);
            ps.setDate(4, endDate);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int newId = rs.getInt(1);
                        return new Course(newId, studentId, courseName, startDate, endDate);
                    }
                }
            } return null; // Indicate failure

        } catch (SQLException e) {
            System.out.println("Error adding course: " + e.getMessage());
            return null; // Indicate failure
        }
    }

    /**
     * Retrieves a course by its ID. Returns a Course object if found, or null if not found or an error occurs.
     * @param courseId
     * @return
     */
    public Course getCourseById(int courseId) {
        Connection conn = MariaDBConnection.getConnection();
        String sql = "SELECT * FROM courses WHERE course_id = ?;";
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
            System.out.println("Error retrieving course: " + e.getMessage());
            return null;
        }
    }

    public ArrayList<Course> getAllCourses(int studentId) {
        ArrayList<Course> courses = new ArrayList<>();
        Connection conn = MariaDBConnection.getConnection();
        String sql = "SELECT * FROM courses WHERE student_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int courseId = rs.getInt("course_id");
                    studentId = rs.getInt("student_id");
                    String courseName = rs.getString("course_name");
                    Date startDate = rs.getDate("start_date");
                    Date endDate = rs.getDate("end_date");
                    courses.add(new Course(courseId, studentId, courseName, startDate, endDate));
                }
            }
            return courses;
        } catch (SQLException e) {
            System.out.println("Error retrieving courses.");
            return null;
        }
    }

    /**
     * Deletes a course by its ID. Also deletes associated schedules.
     * Returns true if deletion was successful, false otherwise.
     * @param courseId
     * @return boolean
     */
    public boolean deleteCourse(int courseId) {
        Connection conn = MariaDBConnection.getConnection();
        String sqlSchedules = "DELETE FROM schedule WHERE course_id = ?;";
        String sqlCourse = "DELETE FROM courses WHERE course_id = ?;";

        try (PreparedStatement psSchedules = conn.prepareStatement(sqlSchedules);
             PreparedStatement psCourse = conn.prepareStatement(sqlCourse)) {
            psSchedules.setInt(1, courseId);
            psSchedules.setInt(1, courseId);
            psSchedules.executeUpdate();
            psCourse.setInt(1, courseId);
            int rows = psCourse.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.out.println("Error deleting course: " + e.getMessage());
            return false;
        }
    }

    public boolean updateCourse(int courseId, String courseName, Date startDate, Date endDate) {
        Course existingCourse = getCourseById(courseId);
        if (existingCourse == null) return false;

        if (courseName == null) courseName = existingCourse.getCourseName();
        if (startDate == null && existingCourse.getStartDate() != null)
            startDate = new java.sql.Date(existingCourse.getStartDate().getTime());
        if (endDate == null && existingCourse.getEndDate() != null)
            endDate = new java.sql.Date(existingCourse.getEndDate().getTime());

        if (startDate != null && endDate != null && endDate.before(startDate)) {
            System.out.println("Error: End date cannot be before start date.");
            return false;
        }

        Connection conn = MariaDBConnection.getConnection();
        String sql = "UPDATE courses SET course_name = ?, start_date = ?, end_date = ? WHERE course_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, courseName);
            ps.setDate(2, startDate);
            ps.setDate(3, endDate);
            ps.setInt(4, courseId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error updating course.");
            return false;
        }
    }
}
