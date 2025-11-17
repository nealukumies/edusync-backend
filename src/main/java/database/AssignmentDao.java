package database;

import model.Assignment;
import model.Status;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("PMD.AtLeastOneConstructor")
/**
 * Data Access Object (DAO) for managing assignments in the database.
 */
public class AssignmentDao {
    /** Logger for logging errors and information. */
    private static final Logger LOGGER = Logger.getLogger(AssignmentDao.class.getName());
    /** Key for course ID column in the database. */
    private static final String COURSE_KEY = "course_id";
    /**
     * Inserts a new assignment into the database.
     * .
     * @param studentId The ID of the student.
     * @param courseId The ID of the course (can be null).
     * @param title The title of the assignment.
     * @param description The description of the assignment.
     * @param deadline The deadline of the assignment.
     * @return Assignment object or null
     */
    @SuppressWarnings("PMD.MethodArgumentCouldBeFinal")
    public Assignment insertAssignment(int studentId, Integer courseId, String title, String description, Timestamp deadline) {
        final String sql = "INSERT INTO assignments (student_id, course_id, title, description, deadline) VALUES (?, ?, ?, ?, ?)";
        final Connection conn = MariaDBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            ps.setInt(1, studentId);
            if (courseId != null) {
                ps.setInt(2, courseId);
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, title);
            ps.setString(4, description);
            ps.setTimestamp(5, deadline);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()){
                if (rs.next()) {
                    final int newId = rs.getInt(1);
                    return new Assignment(newId, studentId, courseId, title, description, deadline, Status.PENDING);
            }
            }
            return null; // Indicate no rows affected
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to insert assignment: " + e.getMessage());
            }
            return null; // Indicate failure
        }
    }

    /**
     * Retrieves all assignments for a given student ID.
     *
     * @param studentId The ID of the student.
     * @return List<Assignment>
     */
    public List<Assignment> getAssignments(final int studentId) {
        final List<Assignment> assignments = new ArrayList<>();

        final String sql = "SELECT assignment_id, course_id, title, description, deadline, status FROM assignments WHERE student_id = ?";
        final Connection conn = MariaDBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setInt(1, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { // loop over all rows
                    final int id = rs.getInt("assignment_id");
                    final String title = rs.getString("title");
                    final String description = rs.getString("description");
                    final Timestamp deadline = rs.getTimestamp("deadline");
                    final String column = COURSE_KEY;
                    final Integer courseId = rs.getObject(column) != null ? rs.getInt(column) : null;
                    final Status status = Status.fromDbValue(rs.getString("status"));
                    assignments.add(new Assignment(id, studentId, courseId, title, description, deadline, status));
                }
            }
            return assignments;
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get assignments: " + e.getMessage());
            }
            return assignments;
        }
    }

    /**
     * Sets the status of an assignment identified by assignmentId.
     *
     * @param assignmentId The ID of the assignment.
     * @param status The new status to set.
     * @return boolean
     */
    @SuppressWarnings("PMD.MethodArgumentCouldBeFinal")
    public boolean updateStatus(int assignmentId, Status status){
        if (status == null) {
            return false;
        }
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "UPDATE assignments SET status = ? WHERE assignment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.getDbValue());
            ps.setInt(2, assignmentId);
            final int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to set assignment status: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Retrieves an assignment by its ID.
     *
     * @param assignmentId The ID of the assignment.
     * @return Assignment
     */
    public Assignment getAssignmentById(final int assignmentId) {
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT assignment_id, student_id, course_id, title, description, deadline, status FROM assignments WHERE assignment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setInt(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final int studentId = rs.getInt("student_id");
                    final Integer courseId = rs.getObject(COURSE_KEY) != null ? rs.getInt(COURSE_KEY) : null;
                    final String title = rs.getString("title");
                    final String description = rs.getString("description");
                    final Timestamp deadline = rs.getTimestamp("deadline");
                    final Status status = Status.fromDbValue(rs.getString("status"));
                    return new Assignment(assignmentId, studentId, courseId, title, description, deadline, status);
                }
            }
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get assignment: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Deletes an assignment by its ID.
     *
     * @param assignmentId The ID of the assignment.
     * @return boolean
     */
    public boolean deleteAssignment(final int assignmentId) {
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "DELETE FROM assignments WHERE assignment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, assignmentId);
            final int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to delete assignment: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Updates an existing assignment's details.
     *
     * @param assignmentId The ID of the assignment.
     * @param title The new title of the assignment.
     * @param description The new description of the assignment.
     * @param deadline The new deadline of the assignment.
     * @param courseId The new course ID of the assignment (can be null).
     * @return boolean
     */
    @SuppressWarnings("PMD.MethodArgumentCouldBeFinal")
    public boolean updateAssignment(int assignmentId, String title, String description, Timestamp deadline, Integer courseId) {
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "UPDATE assignments SET title = ?, description = ?, deadline = ?, course_id = ? WHERE assignment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setTimestamp(3, deadline);
            if (courseId != null) {
                ps.setInt(4, courseId);
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setInt(5, assignmentId);
            final int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to update assignment: " + e.getMessage());
            }
            return false;
        }
    }
}
