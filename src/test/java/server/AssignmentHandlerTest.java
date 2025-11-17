package server;

import database.AssignmentDao;
import model.Assignment;
import model.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.testutils.MockHttpExchange;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssignmentHandlerTest {
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
   void testHandleGetAssignmentById() throws Exception {
        Assignment assignment = new Assignment(1, 1, 1, "Test Assignment", "Description", Timestamp.valueOf("2024-12-31 23:59:59"), Status.PENDING);
        when(mockDao.getAssignmentById(1)).thenReturn(assignment);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/assignments/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        handler.handle(exchange);
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("Test Assignment"), "Response should contain assignment title");
    }

    /**
     * Test handling GET request for assignment by ID when unauthorized.
     */
    @Test
    void testHandleGetAssignmentByIdUnauthorized() throws Exception {
        Assignment assignment = new Assignment(1, 1, 1, "Test Assignment", "Description", Timestamp.valueOf("2024-12-31 23:59:59"), Status.PENDING);
        when(mockDao.getAssignmentById(1)).thenReturn(assignment);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/assignments/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        handler.handle(exchange);
        assertEquals(403, exchange.getResponseCode(), "Response code should be 403");
    }

    /**
     * Test handling GET request for assignments by student ID.
     */
    @Test
    void testHandleGetAssignmentsByStudentId() throws Exception {
        Assignment assignment1 = new Assignment(1, 1, 1, "Assignment 1", "Description 1", Timestamp.valueOf("2024-12-31 09:00:00"), Status.PENDING);
        Assignment assignment2 = new Assignment(2, 1, 2, "Assignment 2", "Description 2", Timestamp.valueOf("2024-11-30 13:00:00"), Status.COMPLETED);
        List<Assignment> assignments = new ArrayList<>();
        assignments.add(assignment1);
        assignments.add(assignment2);
        when(mockDao.getAssignments(1)).thenReturn(assignments);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/assignments/students/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        handler.handle(exchange);
        assertEquals(200, exchange.getResponseCode(), "Response code should be 200");
    }

    /**
     * Test handling GET request for assignments by student ID when unauthorized.
     */
    @Test
    void testHandleGetAssignmentsByStudentIdUnauthorized() throws Exception {
        Assignment assignment1 = new Assignment(1, 1, 1, "Assignment 1", "Description 1", Timestamp.valueOf("2024-12-31 12:45:00"), Status.PENDING);
        List<Assignment> assignments = new ArrayList<>();
        assignments.add(assignment1);
        when(mockDao.getAssignments(1)).thenReturn(assignments);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/assignments/students/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        handler.handle(exchange);
        assertEquals(403, exchange.getResponseCode(), "Response code should be 403");}

    /**
     * Test handling GET request for assignment with no ID in path.
     */
    @Test
    void testHandleGetAssignmentNoId() {
        MockHttpExchange exchange = new MockHttpExchange("GET", "/assignments/", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle get assignment no id: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode(), "Response code should be 400");
    }

    /**
     * Test handling GET request for assignment that does not exist.
     */
    @Test
    void testHandleGetAssignmentNotFound() throws Exception {
        when(mockDao.getAssignmentById(999)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/assignments/999", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        handler.handle(exchange);
        assertEquals(404, exchange.getResponseCode(), "Response code should be 404");
        }

    /**
     * Test handling POST request to create a new assignment.
     */
    @Test
    void testHandlePostAssignment() throws Exception {
        String body = "{\"course_id\":\"1\",\"title\":\"New Assignment\",\"description\":\"New Description\",\"deadline\":\"2024-12-31 00:00:00\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/assignments/", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");

        Assignment newAssignment = new Assignment(1, 1, 1, "New Assignment", "New Description", Timestamp.valueOf("2024-12-31 00:00:00"), Status.PENDING);
        when(mockDao.insertAssignment(1, 1, "New Assignment", "New Description", Timestamp.valueOf("2024-12-31 00:00:00"))).thenReturn(newAssignment);
        handler.handle(exchange);
        assertEquals(201, exchange.getResponseCode(), "Response code should be 201");
     }

    /**
     * Test handling POST request to create a new assignment with missing fields.
     */
    @Test
    void testHandlePostAssignmentMissingFields() throws Exception {
        String body = "{\"course_id\":\"1\",\"description\":\"New Description\",\"deadline\":\"2024-12-31 12:20:00\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/assignments/", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        handler.handle(exchange);

        assertEquals(400, exchange.getResponseCode(), "Response code should be 400");
     }

    /**
     * Test handling POST request to create a new assignment with invalid date format.
     */
    @Test
    void testhandlePostAssignmentInvalidDate() throws Exception {
        String body = "{\"course_id\":\"1\",\"title\":\"New Assignment\",\"description\":\"New Description\",\"deadline\":\"2024-31-12\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/assignments/", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        handler.handle(exchange);
        assertEquals(400, exchange.getResponseCode(), "Response code should be 400");
    }

    /**
     * Test handling DELETE request to delete an assignment.
     */
    @Test
    void testHandleDeleteAssignment() throws Exception {
        Assignment assignment = new Assignment(1, 1, 1, "Test Assignment", "Description", Timestamp.valueOf("2024-12-31 16:45:00"), Status.PENDING);
        when(mockDao.getAssignmentById(1)).thenReturn(assignment);
        when(mockDao.deleteAssignment(1)).thenReturn(true);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/assignments/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        handler.handle(exchange);
        assertEquals(200, exchange.getResponseCode(), "Response code should be 200");
    }

    /**
     * Test handling DELETE request to delete an assignment when unauthorized.
     */
    @Test
    void testHandleDeleteAssignmentUnauthorized() throws Exception {
        Assignment assignment = new Assignment(1, 1, 1, "Test Assignment", "Description", Timestamp.valueOf("2024-12-31 13:30:30"), Status.PENDING);
        when(mockDao.getAssignmentById(1)).thenReturn(assignment);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/assignments/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        handler.handle(exchange);

        assertEquals(403, exchange.getResponseCode(), "Response code should be 403");
     }

    /**
     * Test handling DELETE request to delete an assignment that does not exist.
     */
    @Test
    void testHandleDeleteAssignmentNotFound() throws Exception {
        when(mockDao.getAssignmentById(999)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/assignments/999", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        handler.handle(exchange);
        assertEquals(404, exchange.getResponseCode(), "Response code should be 404");
     }

    /**
     * Test handling PUT request to update an assignment.
     */
    @Test
    void testHandleUpdateAssignment() throws Exception {
        String body = "{\"title\":\"Updated Assignment\",\"description\":\"Updated Description\",\"deadline\":\"2024-11-30 12:30:00\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Timestamp.valueOf("2024-12-31 12:30:00"), Status.PENDING);
        Assignment updatedAssignment = new Assignment(1, 1, 1, "Updated Assignment", "Updated Description", Timestamp.valueOf("2024-11-30 12:30:00"), Status.PENDING);
        when(mockDao.updateAssignment(1, "Updated Assignment", "Updated Description", Timestamp.valueOf("2024-11-30 12:30:00"), 1)).thenReturn(true);
        when(mockDao.getAssignmentById(1)).thenReturn(existingAssignment).thenReturn(updatedAssignment);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/assignments/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        handler.handle(exchange);
        assertEquals(200, exchange.getResponseCode(), "Response code should be 200");
    }

    /**
     * Test handling PUT request to update an assignment with invalid JSON.
     */
    @Test
    void testHandleUpdateAssignmentInvalidJson() throws Exception {
        String body = "invalid json";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/assignments/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        handler.handle(exchange);
        assertEquals(404, exchange.getResponseCode(), "Response code should be 404");
    }

    /**
     * Test handling PUT request to update an assignment that does not exist.
     */
    @Test
    void testHandleUpdateAssignmentNotFound() throws Exception {
        String body = "{\"title\":\"Updated Assignment\"}";
        when(mockDao.getAssignmentById(999)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/assignments/999", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        handler.handle(exchange);
        assertEquals(404, exchange.getResponseCode(), "Response code should be 404");
    }

    /**
     * Test handling PUT request to update an assignment when unauthorized.
     */
    @Test
    void testHandleUpdateAssignmentUnauthorized() throws Exception {
        String body = "{\"title\":\"Updated Assignment\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Timestamp.valueOf("2024-12-31 23:59:59"), Status.PENDING);
        when(mockDao.getAssignmentById(1)).thenReturn(existingAssignment);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/assignments/1", body);
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        handler.handle(exchange);
        assertEquals(403, exchange.getResponseCode(), "Response code should be 403");
   }

    /**
     * Test handling PUT request to update an assignment with invalid date format.
     */
    @Test
    void testHandleUpdateAssignmentInvalidDate() throws Exception {
        String body = "{\"deadline\":\"2024-31-12\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Timestamp.valueOf("2024-12-31 23:59:59"), Status.PENDING);
        when(mockDao.getAssignmentById(1)).thenReturn(existingAssignment);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/assignments/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        handler.handle(exchange);
        assertEquals(400, exchange.getResponseCode(), "Response code should be 400");
   }

    /**
     * Test handling PUT request to update an assignment with invalid course ID.
     */
    @Test
    void testHandleUpdateAssignmentInvalidCourseId() throws Exception {
        String body = "{\"course_id\":\"abc\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Timestamp.valueOf("2024-12-31 23:59:59"), Status.PENDING);
        when(mockDao.getAssignmentById(1)).thenReturn(existingAssignment);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/assignments/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        handler.handle(exchange);
        assertEquals(400, exchange.getResponseCode(), "Response code should be 400");
    }

    /**
     * Test handling PUT request to update an assignment's status.
     */
    @Test
    void testHandleUpdateAssignmentStatus() throws Exception{
        String body = "{\"status\":\"completed\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Timestamp.valueOf("2024-12-31 13:00:00"), Status.PENDING);
        Assignment updatedAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Timestamp.valueOf("2024-12-31 13:00:00"), Status.COMPLETED);
        when(mockDao.updateStatus(1, Status.COMPLETED)).thenReturn(true);
        when(mockDao.getAssignmentById(1)).thenReturn(existingAssignment).thenReturn(updatedAssignment);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/assignments/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        handler.handle(exchange);
        assertEquals(200, exchange.getResponseCode(), "Response code should be 200");
   }

    /**
     * Test handling PUT request to update an assignment's title and status.
     */
    @Test
    void testHandleUpdateAssignmentAndStatus() throws Exception {
        String body = "{\"title\":\"Updated Assignment\",\"status\":\"completed\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Timestamp.valueOf("2024-12-31 12:45:00"), Status.PENDING);
        Assignment updatedAssignment = new Assignment(1, 1, 1, "Updated Assignment", "Old Description", Timestamp.valueOf("2024-12-31 12:45:00"), Status.COMPLETED);
        when(mockDao.updateAssignment(1, "Updated Assignment", "Old Description", Timestamp.valueOf("2024-12-31 12:45:00"), 1)).thenReturn(true);
        when(mockDao.updateStatus(1, Status.COMPLETED)).thenReturn(true);
        when(mockDao.getAssignmentById(1)).thenReturn(existingAssignment).thenReturn(updatedAssignment);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/assignments/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        handler.handle(exchange);
        assertEquals(200, exchange.getResponseCode(), "Response code should be 200");
    }
}
