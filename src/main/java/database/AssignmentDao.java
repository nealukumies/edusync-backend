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
        Assignment result = null;

        try (PreparedStatement prepareStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            prepareStatement.setInt(1, studentId);
            if (courseId != null) {
                prepareStatement.setInt(2, courseId);
            } else {
                prepareStatement.setNull(2, Types.INTEGER);
            }
            prepareStatement.setString(3, title);
            prepareStatement.setString(4, description);
            prepareStatement.setTimestamp(5, deadline);
            prepareStatement.executeUpdate();
            try (ResultSet resultSet = prepareStatement.getGeneratedKeys()){
                if (resultSet.next()) {
                    final int newId = resultSet.getInt(1);
                    result = new Assignment(newId, studentId, courseId, title, description, deadline, Status.PENDING);
            }
            }
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to insert assignment: " + e.getMessage());
            }
        }
        return result;
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
            try (ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) { // loop over all rows
                    final int id = resultSet.getInt("assignment_id");
                    final String title = resultSet.getString("title");
                    final String description = resultSet.getString("description");
                    final Timestamp deadline = resultSet.getTimestamp("deadline");
                    final String column = COURSE_KEY;
                    final Integer courseId = resultSet.getObject(column) != null ? resultSet.getInt(column) : null;
                    final Status status = Status.fromDbValue(resultSet.getString("status"));
                    assignments.add(new Assignment(id, studentId, courseId, title, description, deadline, status));
                }
            }
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get assignments: " + e.getMessage());
            }
        }
        return assignments;
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
        boolean success = false;
        if (status != null) {
            final Connection conn = MariaDBConnection.getConnection();
            final String sql = "UPDATE assignments SET status = ? WHERE assignment_id = ?";
            try (PreparedStatement prepareStatement = conn.prepareStatement(sql)) {
                prepareStatement.setString(1, status.getDbValue());
                prepareStatement.setInt(2, assignmentId);
                final int rows = prepareStatement.executeUpdate();
                success = rows > 0;
            } catch (SQLException e) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, () -> "Failed to set assignment status: " + e.getMessage());
                }
                success = false;
            }
        }
        return success;
    }

    /**
     * Retrieves an assignment by its ID.
     *
     * @param assignmentId The ID of the assignment.
     * @return Assignment
     */
    public Assignment getAssignmentById(final int assignmentId) {
        Assignment result = null;
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT assignment_id, student_id, course_id, title, description, deadline, status FROM assignments WHERE assignment_id = ?";
        try (PreparedStatement prepareStatement = conn.prepareStatement(sql)){
            prepareStatement.setInt(1, assignmentId);
            try (ResultSet resultSet = prepareStatement.executeQuery()) {
                if (resultSet.next()) {
                    final int studentId = resultSet.getInt("student_id");
                    final Integer courseId = resultSet.getObject(COURSE_KEY) != null ? resultSet.getInt(COURSE_KEY) : null;
                    final String title = resultSet.getString("title");
                    final String description = resultSet.getString("description");
                    final Timestamp deadline = resultSet.getTimestamp("deadline");
                    final Status status = Status.fromDbValue(resultSet.getString("status"));
                    result =  new Assignment(assignmentId, studentId, courseId, title, description, deadline, status);
                }
            }
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get assignment: " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Deletes an assignment by its ID.
     *
     * @param assignmentId The ID of the assignment.
     * @return boolean
     */
    public boolean deleteAssignment(final int assignmentId) {
        boolean success = false;
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "DELETE FROM assignments WHERE assignment_id = ?";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)){
            preparedStatement.setInt(1, assignmentId);
            final int rows = preparedStatement.executeUpdate();
            success = rows > 0;
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to delete assignment: " + e.getMessage());
            }
        }
        return success;
    }

    public void deleteAssignmentsByCourseId(final int courseId) {
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "DELETE FROM assignments WHERE course_id = ?";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)){
            preparedStatement.setInt(1, courseId);
            final int rows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to delete assignments by course ID: " + e.getMessage());
            }
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
        boolean success = false;
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "UPDATE assignments SET title = ?, description = ?, deadline = ?, course_id = ? WHERE assignment_id = ?";

        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)){
            preparedStatement.setString(1, title);
            preparedStatement.setString(2, description);
            preparedStatement.setTimestamp(3, deadline);
            if (courseId != null) {
                preparedStatement.setInt(4, courseId);
            } else {
                preparedStatement.setNull(4, Types.INTEGER);
            }
            preparedStatement.setInt(5, assignmentId);
            final int rows = preparedStatement.executeUpdate();
            success = rows > 0;
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to update assignment: " + e.getMessage());
            }
        }
        return success;
    }
}
