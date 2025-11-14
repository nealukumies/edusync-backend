/**
 * Data Access Object (DAO) class for managing student records in the database.
 * Provides methods to add a new student and retrieve a student by email.
 */

package database;

import model.Student;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("PMD.AtLeastOneConstructor")
public class StudentDao {
    private static final Logger LOGGER = Logger.getLogger(StudentDao.class.getName());

    /**
     * Adds a new student to the database. Returns the Student object if successful, or null if insertion fails.
     * @param name
     * @param email
     * @param password
     * @return Student - the created Student object, or null if error occurs
     */
    @SuppressWarnings("PMD.MethodArgumentCouldBeFinal")
    public Student addStudent(String name, String email, String password) {
        if (name == null || name.isEmpty() || email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return null;
        }
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "INSERT INTO students (name, email, password_hash) VALUES (?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            ps.setString(1, name);
            ps.setString(2, email);
            final String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
            ps.setString(3, hashedPassword);
            final int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        final int newId = rs.getInt(1);
                        return new Student(newId, name, email, "user"); // Default role is "user"
                    }
                }
            } return null; // Indicate failure
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to add student: " + e.getMessage());
            }
            return null; // Indicate failure
        }
    }

    /**
     * Retrieves a student by email. Returns a Student object if found, or null if not found or an error occurs.
     * @param email
     * @return
     */
    public Student getStudent(final String email) {
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT student_id, name, email, role FROM students WHERE email = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final int id = rs.getInt("student_id");
                    final String name = rs.getString("name");
                    final String role = rs.getString("role");
                    return new Student(id, name, email, role);
                }
            }
            return null; // Student not found
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get student: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Retrieves a student by student ID. Returns a Student object if found, or null if not found or an error occurs.
     * @param studentId
     * @return
     */

    public Student getStudentById(final int studentId) {
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT name, email, role FROM students WHERE student_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final String name = rs.getString("name");
                    final String email = rs.getString("email");
                    final String role = rs.getString("role");
                    return new Student(studentId, name, email, role);
                }
            }
            return null; // Student not found
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get student: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Retrieves the password hash for a student by email. Returns the password hash if found, or null if not found or an error occurs.
     * @param email
     * @return String - password hash, or null if not found
     */
    public String getPasswordHash(final String email) {
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT password_hash FROM students WHERE email = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
            }
            return null; // Student not found
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get password hash: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Deletes a student by student ID. Returns true if deletion was successful, false otherwise.
     * Deletes related assignments and courses as well.
     * @param studentId
     * @return
     */
    public boolean deleteStudent(final int studentId) {
        final Connection conn = MariaDBConnection.getConnection();
        try {
            conn.setAutoCommit(false); // start transaction

            // Delete assignments
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM assignments WHERE student_id = ?")) {
                ps.setInt(1, studentId);
                ps.executeUpdate();
            }

            // Delete courses
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM courses WHERE student_id = ?")) {
                ps.setInt(1, studentId);
                ps.executeUpdate();
            }

            // Delete student
            final int rows;
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM students WHERE student_id = ?")) {
                ps.setInt(1, studentId);
                rows = ps.executeUpdate();
            }

            conn.commit();
            return rows > 0;

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, () -> "Failed to rollback: " + ex.getMessage());
                }
            }
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to delete student: " + e.getMessage());
            }
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ex) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, () -> "Failed to setAutoCommit: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Updates a student's name. Returns true if update was successful, false otherwise.
     * @param studentId
     * @param newName
     * @return
     */
    @SuppressWarnings("PMD.MethodArgumentCouldBeFinal")
    public boolean updateStudentName(int studentId, String newName) {
        if (newName == null || newName.isEmpty()) {
            return false;
        }
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "UPDATE students SET name = ? WHERE student_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, newName);
            ps.setInt(2, studentId);
            final int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to update student name: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Updates a student's email. Returns true if update was successful, false otherwise.
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
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, newEmail);
            ps.setInt(2, studentId);
            final int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, () -> "Failed to update student email: " + e.getMessage());
            return false;
        }
    }
}
