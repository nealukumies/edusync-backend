/**
 * Data Access Object (DAO) class for managing student records in the database.
 * Provides methods to add a new student and retrieve a student by email.
 */

package database;

import model.Student;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public class StudentDao {

    /**
     * Adds a new student to the database. Returns the Student object if successful, or null if insertion fails.
     * @param name
     * @param email
     * @param password
     * @return Student - the created Student object, or null if error occurs
     */
    public Student addStudent(String name, String email, String password) {
        if (name == null || name.isEmpty() || email == null || email.isEmpty() || password == null || password.isEmpty()) {
            System.out.println("Error: Name, email and password cannot be null or empty.");
            return null;
        }
        Connection conn = MariaDBConnection.getConnection();
        String sql = "INSERT INTO students (name, email, password_hash) VALUES (?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            ps.setString(1, name);
            ps.setString(2, email);
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
            ps.setString(3, hashedPassword);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int newId = rs.getInt(1);
                        return new Student(newId, name, email, "user"); // Default role is "user"
                    }
                }
            } return null; // Indicate failure
        } catch (SQLException e) {
            System.out.println("Error adding student: " + e.getMessage());
            return null; // Indicate failure
        }
    }

    /**
     * Retrieves a student by email. Returns a Student object if found, or null if not found or an error occurs.
     * @param email
     * @return
     */
    public Student getStudent(String email) {
        Connection conn = MariaDBConnection.getConnection();
        String sql = "SELECT student_id, name, email, role FROM students WHERE email = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("student_id");
                    String name = rs.getString("name");
                    String role = rs.getString("role");
                    return new Student(id, name, email, role);
                }
            }
            return null; // Student not found
        } catch (SQLException e) {
            System.out.println("Error retrieving student ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves a student by student ID. Returns a Student object if found, or null if not found or an error occurs.
     * @param studentId
     * @return
     */

    public Student getStudentById(int studentId) {
        Connection conn = MariaDBConnection.getConnection();
        String sql = "SELECT name, email, role FROM students WHERE student_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    String email = rs.getString("email");
                    String role = rs.getString("role");
                    return new Student(studentId, name, email, role);
                }
            }
            return null; // Student not found
        } catch (SQLException e) {
            System.out.println("Error retrieving student: " + e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves the password hash for a student by email. Returns the password hash if found, or null if not found or an error occurs.
     * @param email
     * @return String - password hash, or null if not found
     */
    public String getPasswordHash(String email) {
        Connection conn = MariaDBConnection.getConnection();
        String sql = "SELECT password_hash FROM students WHERE email = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
            }
            return null; // Student not found
        } catch (SQLException e) {
            System.out.println("Error retrieving student password hash.");
            return null;
        }
    }

    /**
     * Deletes a student by student ID. Returns true if deletion was successful, false otherwise.
     * Deletes related assignments and courses as well.
     * @param studentId
     * @return
     */
    public boolean deleteStudent(int studentId) {
        Connection conn = MariaDBConnection.getConnection();
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
            int rows;
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM students WHERE student_id = ?")) {
                ps.setInt(1, studentId);
                rows = ps.executeUpdate();
            }

            conn.commit();
            return rows > 0;

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) {
                System.out.println("Error during rollback: " + ex.getMessage());
            }
            System.out.println("Error deleting student and related data: " + e.getMessage());
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ex) {
                System.out.println("Error resetting auto-commit: " + ex.getMessage());
            }
        }
    }

    public boolean updateStudentName(int studentId, String newName) {
        if (newName == null || newName.isEmpty()) {
            System.out.println("Error: Name cannot be null or empty.");
            return false;
        }
        Connection conn = MariaDBConnection.getConnection();
        String sql = "UPDATE students SET name = ? WHERE student_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, newName);
            ps.setInt(2, studentId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error updating student name.");
            return false;
        }
    }

    public boolean updateStudentEmail(int studentId, String newEmail) {
        if (newEmail == null || newEmail.isEmpty()) {
            System.out.println("Error: Email cannot be null or empty.");
            return false;
        }
        Connection conn = MariaDBConnection.getConnection();
        String sql = "UPDATE students SET email = ? WHERE student_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, newEmail);
            ps.setInt(2, studentId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error updating student email: " + e.getMessage());
            return false;
        }
    }
}
