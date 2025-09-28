package server;

import database.AssignmentDao;
import model.Assignment;
import model.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.testutils.MockHttpExchange;

import java.sql.Date;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AssignmentHandlerTest {
    private AssignmentHandler handler;
    private AssignmentDao mockDao;

    @BeforeEach
    public void setUp() {
        mockDao = mock(AssignmentDao.class);
        handler = new AssignmentHandler(mockDao);
    }

    @Test
    public void testHandleGetAssignmentById() {
        Assignment assignment = new Assignment(1, 1, 1, "Test Assignment", "Description", Date.valueOf("2024-12-31"), Status.PENDING);
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

    @Test
    public void testHandleGetAssignmentByIdUnauthorized() {
        Assignment assignment = new Assignment(1, 1, 1, "Test Assignment", "Description", Date.valueOf("2024-12-31"), Status.PENDING);
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

    @Test
    public void testHandleGetAssignmentsByStudentId() {
        Assignment assignment1 = new Assignment(1, 1, 1, "Assignment 1", "Description 1", Date.valueOf("2024-12-31"), Status.PENDING);
        Assignment assignment2 = new Assignment(2, 1, 2, "Assignment 2", "Description 2", Date.valueOf("2024-11-30"), Status.COMPLETED);
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

    @Test
    public void testHandleGetAssignmentsByStudentIdUnauthorized() {
        Assignment assignment1 = new Assignment(1, 1, 1, "Assignment 1", "Description 1", Date.valueOf("2024-12-31"), Status.PENDING);
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

    @Test
    public void testHandlePostAssignment() {
        String body = "{\"course_id\":\"1\",\"title\":\"New Assignment\",\"description\":\"New Description\",\"deadline\":\"2024-12-31\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/assignments/", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");

        Assignment newAssignment = new Assignment(1, 1, 1, "New Assignment", "New Description", Date.valueOf("2024-12-31"), Status.PENDING);
        when(mockDao.insertAssignment(1, 1, "New Assignment", "New Description", Date.valueOf("2024-12-31"))).thenReturn(newAssignment);

        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in handle post assignment: " + e.getMessage());
        }
        assertEquals(201, exchange.getResponseCode());
        String response = exchange.getResponseBodyAsString();
        assertTrue(response.contains("New Assignment"));
    }

    @Test
    public void testHandlePostAssignmentMissingFields() {
        String body = "{\"course_id\":\"1\",\"description\":\"New Description\",\"deadline\":\"2024-12-31\"}";
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

    @Test
    public void testHandleDeleteAssignment() {
        Assignment assignment = new Assignment(1, 1, 1, "Test Assignment", "Description", Date.valueOf("2024-12-31"), Status.PENDING);
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

    @Test
    public void testHandleDeleteAssignmentUnauthorized() {
        Assignment assignment = new Assignment(1, 1, 1, "Test Assignment", "Description", Date.valueOf("2024-12-31"), Status.PENDING);
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

    @Test
    public void testHandleUpdateAssignment() {
        String body = "{\"title\":\"Updated Assignment\",\"description\":\"Updated Description\",\"deadline\":\"2024-11-30\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Date.valueOf("2024-12-31"), Status.PENDING);
        Assignment updatedAssignment = new Assignment(1, 1, 1, "Updated Assignment", "Updated Description", Date.valueOf("2024-11-30"), Status.PENDING);
        when(mockDao.updateAssignment(1, "Updated Assignment", "Updated Description", Date.valueOf("2024-11-30"), 1)).thenReturn(true);
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

    @Test
    public void testHandleUpdateAssignmentUnauthorized() {
        String body = "{\"title\":\"Updated Assignment\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Date.valueOf("2024-12-31"), Status.PENDING);
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

    @Test
    public void testHandleUpdateAssignmentInvalidDate() {
        String body = "{\"deadline\":\"2024-31-12\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Date.valueOf("2024-12-31"), Status.PENDING);
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

    @Test
    public void testHandleUpdateAssignmentInvalidCourseId() {
        String body = "{\"course_id\":\"abc\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Date.valueOf("2024-12-31"), Status.PENDING);
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

    @Test
    public void testHandleUpdateAssignmentStatus(){
        String body = "{\"status\":\"completed\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Date.valueOf("2024-12-31"), Status.PENDING);
        Assignment updatedAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Date.valueOf("2024-12-31"), Status.COMPLETED);
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

    @Test
    public void testHandleUpdateAssignmentAndStatus() {
        String body = "{\"title\":\"Updated Assignment\",\"status\":\"completed\"}";
        Assignment existingAssignment = new Assignment(1, 1, 1, "Old Assignment", "Old Description", Date.valueOf("2024-12-31"), Status.PENDING);
        Assignment updatedAssignment = new Assignment(1, 1, 1, "Updated Assignment", "Old Description", Date.valueOf("2024-12-31"), Status.COMPLETED);
        when(mockDao.updateAssignment(1, "Updated Assignment", "Old Description", Date.valueOf("2024-12-31"), 1)).thenReturn(true);
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
