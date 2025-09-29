/**
 * AssignmentDao.java
 * Data Access Object for managing assignments in the database. Provides methods to insert, retrieve, update, and delete assignments.
 */


package database;

import model.Assignment;
import model.Status;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AssignmentDao {

    /**
     * Inserts a new assignment into the database.
     * Returns the created Assignment object, or null if insertion fails or no rows affected.
     * @param studentId
     * @param courseId
     * @param title
     * @param description
     * @param deadline
     * @return Assignment object or null
     */
    public Assignment insertAssignment(int studentId, Integer courseId, String title, String description, Timestamp deadline) {
        Connection conn = MariaDBConnection.getConnection();
        String sql = "INSERT INTO assignments (student_id, course_id, title, description, deadline) VALUES (?, ?, ?, ?, ?)";
        try{
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, studentId);
            if (courseId != null) {
                ps.setInt(2, courseId);
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, title);
            ps.setString(4, description);
            ps.setTimestamp(5, deadline);
            int rows = ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int newId = rs.getInt(1);
                Assignment assignment = new Assignment(newId, studentId, courseId, title, description, deadline, Status.PENDING);
                return assignment; // Return the created assignment object
            } else if (rows == 0) {
                return null; // No rows affected
            } else {
                return null; // Indicate failure
            }
        } catch (SQLException e) {
            System.out.println("Error inserting assignment: " + e.getMessage());
            return null; // Indicate failure
        }
    }

    /**
     * Retrieves all assignments for a given student ID.
     * Returns a list of Assignment objects, or null if an error occurs.
     * @param studentId
     * @return List<Assignment>
     */
    public List<Assignment> getAssignments(int studentId) {
        List<Assignment> assignments = new ArrayList<>();
        Connection conn = MariaDBConnection.getConnection();
        String sql = "SELECT * FROM assignments WHERE student_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) { // loop over all rows
                int id = rs.getInt("assignment_id");
                String title = rs.getString("title");
                String description = rs.getString("description");
                Timestamp deadline = rs.getTimestamp("deadline");
                Integer courseId = rs.getObject("course_id") != null ? rs.getInt("course_id") : null;
                Status status = Status.fromDbValue(rs.getString("status"));
                assignments.add(new Assignment(id, studentId, courseId, title, description, deadline, status));
            }
            return assignments;
        } catch (SQLException e) {
            System.out.println("Error retrieving assignments: " + e.getMessage());
            return null;
        }
    }

    /**
     * Sets the status of an assignment identified by assignmentId.
     * Returns true if the update was successful, false otherwise.
     * @param assignmentId
     * @param status
     * @return boolean
     */
    public boolean setStatus(int assignmentId, Status status){
        if (status == null) {
            System.out.println("Status cannot be null");
            return false;
        }
        Connection conn = MariaDBConnection.getConnection();
        String sql = "UPDATE assignments SET status = ? WHERE assignment_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, status.getDbValue());
            ps.setInt(2, assignmentId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error updating assignment status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves an assignment by its ID.
     * Returns the Assignment object if found, or null if not found or an error occurs.
     * @param assignmentId
     * @return Assignment
     */
    public Assignment getAssignmentById(int assignmentId) {
        Connection conn = MariaDBConnection.getConnection();
        String sql = "SELECT * FROM assignments WHERE assignment_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, assignmentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int studentId = rs.getInt("student_id");
                Integer courseId = rs.getObject("course_id") != null ? rs.getInt("course_id") : null;
                String title = rs.getString("title");
                String description = rs.getString("description");
                Timestamp deadline = rs.getTimestamp("deadline");
                Status status = Status.fromDbValue(rs.getString("status"));
                return new Assignment(assignmentId, studentId, courseId, title, description, deadline, status);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving assignment: " + e.getMessage());
        }
        return null;
    }

    /**
     * Deletes an assignment by its ID.
     * Returns true if the deletion was successful, false otherwise.
     * @param assignmentId
     * @return boolean
     */
    public boolean deleteAssignment(int assignmentId) {
        Connection conn = MariaDBConnection.getConnection();
        String sql = "DELETE FROM assignments WHERE assignment_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, assignmentId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting assignment: " + e.getMessage());
            return false;
        }
    }

    public boolean updateAssignment(int assignmentId, String title, String description, Timestamp deadline, Integer courseId) {
        Connection conn = MariaDBConnection.getConnection();
        String sql = "UPDATE assignments SET title = ?, description = ?, deadline = ?, course_id = ? WHERE assignment_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setTimestamp(3, deadline);
            if (courseId != null) {
                ps.setInt(4, courseId);
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setInt(5, assignmentId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error updating assignment: " + e.getMessage());
            return false;
        }
    }

//    // For testing purposes
//    public static void main(String[] args) {
//        AssignmentDao dao = new AssignmentDao();
//        //dao.insertAssignment(1, 1, "Math Homework", "Complete exercises 1-10", Date.valueOf("2025-10-11"));
//        //dao.setStatus(2, Status.IN_PROGRESS);
//        List<Assignment> assignments = dao.getAssignments(1);
//        for (Assignment a : assignments) {
//            System.out.println("ID: " + a.getAssignmentId() + ", Title: " + a.getTitle() + ", Status: " + a.getStatus());
//        }
////        System.out.println("Assignment with id 3: " + dao.getAssignmentById(3));
////        dao.deleteAssignment(3);
////        System.out.println("After deletion, assignment with id 2: " + dao.getAssignmentById(3));
//    }
}
