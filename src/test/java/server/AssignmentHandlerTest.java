package server;

import database.AssignmentDao;
import model.Assignment;
import model.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.testutils.MockHttpExchange;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AssignmentHandlerTest {
    private AssignmentHandler handler;
    private AssignmentDao mockDao;

    /**
     * Setup before each test.
     */
    @BeforeEach
    public void setUp() {
        mockDao = mock(AssignmentDao.class);
        handler = new AssignmentHandler(mockDao);
    }

    /**
     * Test handling GET request for assignment by ID.
     */
    @Test
    public void testHandleGetAssignmentById() {
        Assignment assignment = new Assignment(1, 1, 1, "Test Assignment", "Description", Timestamp.valueOf("2024-12-31 23:59:59"), Status.PENDING);
        when(mockDao.getAssignmentById(1)).thenReturn(assignment);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/assignments/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle get assignment by id: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Test Assignment"));
    }

    /**
     * Test handling GET request for assignment by ID when unauthorized.
     */
    @Test
    public void testHandleGetAssignmentByIdUnauthorized() {
        Assignment assignment = new Assignment(1, 1, 1, "Test Assignment", "Description", Timestamp.valueOf("2024-12-31 23:59:59"), Status.PENDING);
        when(mockDao.getAssignmentById(1)).thenReturn(assignment);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/assignments/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle get assignment by id unauthorized: " + e.getMessage());
        }
        assertEquals(403, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Forbidden"));
    }

    /**
     * Test handling GET request for assignments by student ID.
     */
    @Test
    public void testHandleGetAssignmentsByStudentId() {
        Assignment assignment1 = new Assignment(1, 1, 1, "Assignment 1", "Description 1", Timestamp.valueOf("2024-12-31 09:00:00"), Status.PENDING);
        Assignment assignment2 = new Assignment(2, 1, 2, "Assignment 2", "Description 2", Timestamp.valueOf("2024-11-30 13:00:00"), Status.COMPLETED);
        ArrayList<Assignment> assignments = new ArrayList<>();
        assignments.add(assignment1);
        assignments.add(assignment2);
        when(mockDao.getAssignments(1)).thenReturn(assignments);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/assignments/students/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle get assignments by student id: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Assignment 1"));
        assertTrue(response.contains("Assignment 2"));
    }

    /**
     * Test handling GET request for assignments by student ID when unauthorized.
     */
    @Test
    public void testHandleGetAssignmentsByStudentIdUnauthorized() {
        Assignment assignment1 = new Assignment(1, 1, 1, "Assignment 1", "Description 1", Timestamp.valueOf("2024-12-31 12:45:00"), Status.PENDING);
        ArrayList<Assignment> assignments = new ArrayList<>();
        assignments.add(assignment1);
        when(mockDao.getAssignments(1)).thenReturn(assignments);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/assignments/students/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle get assignments by student id unauthorized: " + e.getMessage());
        }
        assertEquals(403, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Forbidden"));
    }

    /**
     * Test handling GET request for assignment with no ID in path.
     */
    @Test
    public void testHandleGetAssignmentNoId() {
        MockHttpExchange exchange = new MockHttpExchange("GET", "/assignments/", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle get assignment no id: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
    }

    /**
     * Test handling GET request for assignment that does not exist.
     */
    @Test
    public void testHandleGetAssignmentNotFound() {
        when(mockDao.getAssignmentById(999)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/assignments/999", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle get assignment not found: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Assignment not found"));
    }

    /**
     * Test handling POST request to create a new assignment.
     */
    @Test
    public void testHandlePostAssignment() {
        String body = "{\"course_id\":\"1\",\"title\":\"New Assignment\",\"description\":\"New Description\",\"deadline\":\"2024-12-31 00:00:00\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/assignments/", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");

        Assignment newAssignment = new Assignment(1, 1, 1, "New Assignment", "New Description", Timestamp.valueOf("2024-12-31 00:00:00"), Status.PENDING);
        when(mockDao.insertAssignment(1, 1, "New Assignment", "New Description", Timestamp.valueOf("2024-12-31 00:00:00"))).thenReturn(newAssignment);

        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle post assignment: " + e.getMessage());
        }
        assertEquals(201, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("New Assignment"));
    }

    /**
     * Test handling POST request to create a new assignment with missing fields.
     */
    @Test
    public void testHandlePostAssignmentMissingFields() {
        String body = "{\"course_id\":\"1\",\"description\":\"New Description\",\"deadline\":\"2024-12-31 12:20:00\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/assignments/", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");

        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle post assignment missing fields: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Title, and deadline are required"));
    }

    /**
     * Test handling POST request to create a new assignment with invalid date format.
     */
    @Test
    public void testhandlePostAssignmentInvalidDate() {
        String body = "{\"course_id\":\"1\",\"title\":\"New Assignment\",\"description\":\"New Description\",\"deadline\":\"2024-31-12\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/assignments/", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");

        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle post assignment invalid date: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Invalid date format"));
    }

    /**
     * Test handling DELETE request to delete an assignment.
     */
    @Test
    public void testHandleDeleteAssignment() {
        Assignment assignment = new Assignment(1, 1, 1, "Test Assignment", "Description", Timestamp.valueOf("2024-12-31 16:45:00"), Status.PENDING);
        when(mockDao.getAssignmentById(1)).thenReturn(assignment);
        when(mockDao.deleteAssignment(1)).thenReturn(true);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/assignments/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle delete assignment: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Assignment deleted successfully"));
    }

    /**
     * Test handling DELETE request to delete an assignment when unauthorized.
     */
    @Test
    public void testHandleDeleteAssignmentUnauthorized() {
        Assignment assignment = new Assignment(1, 1, 1, "Test Assignment", "Description", Timestamp.valueOf("2024-12-31 13:30:30"), Status.PENDING);
        when(mockDao.getAssignmentById(1)).thenReturn(assignment);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/assignments/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle delete assignment unauthorized: " + e.getMessage());
        }
        assertEquals(403, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Forbidden"));
    }

    /**
     * Test handling DELETE request to delete an assignment that does not exist.
     */
    @Test
    public void testHandleDeleteAssignmentNotFound() {
        when(mockDao.getAssignmentById(999)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/assignments/999", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle delete assignment not found: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Assignment not found"));
    }

    /**
     * Test handling PUT request to update an assignment.
     */
    @Test
    public void testHandleUpdateAssignment() {
        String body = "{\"title\":\"Updated Assignment\",\"description\":\"Updated Description\",\"deadline\":\"2024-11-30 12:30:00\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Timestamp.valueOf("2024-12-31 12:30:00"), Status.PENDING);
        Assignment updatedAssignment = new Assignment(1, 1, 1, "Updated Assignment", "Updated Description", Timestamp.valueOf("2024-11-30 12:30:00"), Status.PENDING);
        when(mockDao.updateAssignment(1, "Updated Assignment", "Updated Description", Timestamp.valueOf("2024-11-30 12:30:00"), 1)).thenReturn(true);
        when(mockDao.getAssignmentById(1)).thenReturn(existingAssignment).thenReturn(updatedAssignment);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/assignments/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle update assignment: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Updated Assignment"));
    }

    /**
     * Test handling PUT request to update an assignment with invalid JSON.
     */
    @Test
    public void testHandleUpdateAssignmentInvalidJson() {
        String body = "invalid json";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/assignments/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle update assignment invalid json: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("invalid json"));
    }

    /**
     * Test handling PUT request to update an assignment that does not exist.
     */
    @Test
    public void testHandleUpdateAssignmentNotFound() {
        String body = "{\"title\":\"Updated Assignment\"}";
        when(mockDao.getAssignmentById(999)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/assignments/999", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle update assignment not found: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Assignment not found"));
    }

    /**
     * Test handling PUT request to update an assignment when unauthorized.
     */
    @Test
    public void testHandleUpdateAssignmentUnauthorized() {
        String body = "{\"title\":\"Updated Assignment\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Timestamp.valueOf("2024-12-31 23:59:59"), Status.PENDING);
        when(mockDao.getAssignmentById(1)).thenReturn(existingAssignment);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/assignments/1", body);
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle update assignment unauthorized: " + e.getMessage());
        }
        assertEquals(403, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Forbidden"));
    }

    /**
     * Test handling PUT request to update an assignment with invalid date format.
     */
    @Test
    public void testHandleUpdateAssignmentInvalidDate() {
        String body = "{\"deadline\":\"2024-31-12\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Timestamp.valueOf("2024-12-31 23:59:59"), Status.PENDING);
        when(mockDao.getAssignmentById(1)).thenReturn(existingAssignment);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/assignments/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle update assignment invalid date: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Invalid date format"));
    }

    /**
     * Test handling PUT request to update an assignment with invalid course ID.
     */
    @Test
    public void testHandleUpdateAssignmentInvalidCourseId() {
        String body = "{\"course_id\":\"abc\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Timestamp.valueOf("2024-12-31 23:59:59"), Status.PENDING);
        when(mockDao.getAssignmentById(1)).thenReturn(existingAssignment);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/assignments/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle update assignment invalid course id: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Invalid course_id"));
    }

    /**
     * Test handling PUT request to update an assignment's status.
     */
    @Test
    public void testHandleUpdateAssignmentStatus(){
        String body = "{\"status\":\"completed\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Timestamp.valueOf("2024-12-31 13:00:00"), Status.PENDING);
        Assignment updatedAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Timestamp.valueOf("2024-12-31 13:00:00"), Status.COMPLETED);
        when(mockDao.setStatus(1, Status.COMPLETED)).thenReturn(true);
        when(mockDao.getAssignmentById(1)).thenReturn(existingAssignment).thenReturn(updatedAssignment);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/assignments/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle update assignment: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("COMPLETED"));
    }

    /**
     * Test handling PUT request to update an assignment's title and status.
     */
    @Test
    public void testHandleUpdateAssignmentAndStatus() {
        String body = "{\"title\":\"Updated Assignment\",\"status\":\"completed\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Timestamp.valueOf("2024-12-31 12:45:00"), Status.PENDING);
        Assignment updatedAssignment = new Assignment(1, 1, 1, "Updated Assignment", "Old Description", Timestamp.valueOf("2024-12-31 12:45:00"), Status.COMPLETED);
        when(mockDao.updateAssignment(1, "Updated Assignment", "Old Description", Timestamp.valueOf("2024-12-31 12:45:00"), 1)).thenReturn(true);
        when(mockDao.setStatus(1, Status.COMPLETED)).thenReturn(true);
        when(mockDao.getAssignmentById(1)).thenReturn(existingAssignment).thenReturn(updatedAssignment);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/assignments/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle update assignment and status: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Updated Assignment"));
        assertTrue(response.contains("COMPLETED"));
    }
}
