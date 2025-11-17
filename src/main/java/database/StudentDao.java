package database;

import model.Student;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) class for managing student records in the database.
 * Provides methods to add a new student and retrieve a student by email.
 */
public class StudentDao {
    private static final Logger LOGGER = Logger.getLogger(StudentDao.class.getName());

    /**
     * Adds a new student to the database.
     *
     * @param name The name of the student
     * @param email the email of the student
     * @param password The plaintext password of the student
     * @return Student - the created Student object, or null if error occurs
     */
    @SuppressWarnings("PMD.MethodArgumentCouldBeFinal")
    public Student addStudent(String name, String email, String password) {
        Student result = null;
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "INSERT INTO students (name, email, password_hash) VALUES (?, ?, ?);";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, email);
            final String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
            preparedStatement.setString(3, hashedPassword);
            final int rows = preparedStatement.executeUpdate();
            if (rows > 0) {
                try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                    if (resultSet.next()) {
                        final int newId = resultSet.getInt(1);
                        result = new Student(newId, name, email, "user"); // Default role is "user"
                    }
                }
            }
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to add student: " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Retrieves a student by email.
     *
     * @param email The email of the student
     * @return Student - the Student object, or null if not found
     */
    public Student getStudent(final String email) {
        Student result = null;
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT student_id, name, email, role FROM students WHERE email = ?;";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)){
            preparedStatement.setString(1, email);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    final int id = resultSet.getInt("student_id");
                    final String name = resultSet.getString("name");
                    final String role = resultSet.getString("role");
                    result = new Student(id, name, email, role);
                }
            }
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get student: " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Retrieves a student by student ID.
     *
     * @param studentId The ID of the student
     * @return Student - the Student object, or null if not found
     */

    public Student getStudentById(final int studentId) {
        Student result = null;
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT name, email, role FROM students WHERE student_id = ?;";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setInt(1, studentId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    final String name = resultSet.getString("name");
                    final String email = resultSet.getString("email");
                    final String role = resultSet.getString("role");
                    result = new Student(studentId, name, email, role);
                }
            }
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get student: " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Retrieves the password hash for a student by email.
     *
     * @param email The email of the student
     * @return String - password hash, or null if not found
     */
    public String getPasswordHash(final String email) {
        String result = null;
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT password_hash FROM students WHERE email = ?;";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)){
            preparedStatement.setString(1, email);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    result = resultSet.getString("password_hash");
                }
            }
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get password hash: " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Deletes a student by student ID.
     *
     * @param studentId The ID of the student
     * @return boolean
     */
    public boolean deleteStudent(final int studentId) {
        boolean success = false;
        final Connection conn = MariaDBConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement preparedStatement = conn.prepareStatement(
                    "DELETE FROM assignments WHERE student_id = ?")) {
                preparedStatement.setInt(1, studentId);
                preparedStatement.executeUpdate();
            }
            try (PreparedStatement preparedStatement = conn.prepareStatement(
                    "DELETE FROM courses WHERE student_id = ?")) {
                preparedStatement.setInt(1, studentId);
                preparedStatement.executeUpdate();
            }
            final int rows;
            try (PreparedStatement preparedStatement = conn.prepareStatement(
                    "DELETE FROM students WHERE student_id = ?")) {
                preparedStatement.setInt(1, studentId);
                rows = preparedStatement.executeUpdate();
            }
            conn.commit();
            success = rows > 0;

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, () -> "Failed to rollback: " + ex.getMessage());
                }
            }
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to delete student: " + e.getMessage());
            }
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ex) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, () -> "Failed to setAutoCommit: " + ex.getMessage());
                }
            }
        }
        return success;
    }

    /**
     * Updates a student's name.
     *
     * @param studentId The ID of the student
     * @param newName The new name of the student
     * @return True if update was successful, false otherwise
     */
    @SuppressWarnings("PMD.MethodArgumentCouldBeFinal")
    public boolean updateStudentName(int studentId, String newName) {
        boolean success = false;
        if (newName != null && !newName.isEmpty()) {
            final Connection conn = MariaDBConnection.getConnection();
            final String sql = "UPDATE students SET name = ? WHERE student_id = ?;";
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setString(1, newName);
                preparedStatement.setInt(2, studentId);
                final int rows = preparedStatement.executeUpdate();
                success = rows > 0;
            } catch (SQLException e) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, () -> "Failed to update student name: " + e.getMessage());
                }
            }
        }
        return success;
    }

    /**
     * Updates a student's email.
     *
     * @param studentId
     * @param newEmail
     * @return
     */
    @SuppressWarnings("PMD.MethodArgumentCouldBeFinal")
    public boolean updateStudentEmail(int studentId, String newEmail) {
        if (newEmail == null || newEmail.isEmpty()) {
            return false;
        }
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "UPDATE students SET email = ? WHERE student_id = ?;";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)){
            preparedStatement.setString(1, newEmail);
            preparedStatement.setInt(2, studentId);
            final int rows = preparedStatement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, () -> "Failed to update student email: " + e.getMessage());
            return false;
        }
    }
}
