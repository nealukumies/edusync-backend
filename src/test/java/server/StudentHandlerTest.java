package server;

import database.StudentDao;
import model.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.testutils.MockHttpExchange;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StudentHandlerTest {
    private StudentHandler handler;
    private StudentDao mockDao;

    @BeforeEach
    void setup() {
        this.mockDao = mock(StudentDao.class);
        this.handler = new StudentHandler(mockDao);
    }

    @Test
    void testGetStudent() {
        Student student = new Student(1, "Test", "test@test.fi", "user");
        when(mockDao.getStudentById(1)).thenReturn(student);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/students/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in get student: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Test"));
    }

    @Test
    void testGetStudentUnauthorizedAccess() {
        MockHttpExchange exchange = new MockHttpExchange("GET", "/students/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in unauthorized access test: " + e.getMessage());
        }
        assertEquals(403, exchange.getResponseCode());
    }

    @Test
    void testStudentNotFound() {
        when(mockDao.getStudentById(999)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("GET", "/students/999", "");
        exchange.withHeader("student_id", "999").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in student not found test: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
    }

    @Test
    void testPostStudent() {
        Student newStudent = new Student(2, "Testeri", "testeri@email.com", "user");
        when(mockDao.addStudent("Testeri", "tester@email.com", "password")).thenReturn(newStudent);
        String requestBody = "{\"name\":\"Testeri\",\"email\":\"tester@email.com\",\"password\":\"password\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/students", requestBody);
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in post student test: " + e.getMessage());
        }
        assertEquals(201, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Testeri"));
    }

    @Test
    void testPostStudentNoName() {
        String requestBody = "{\"email\":\"tester@tester.fi\",\"password\":\"password\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/students", requestBody);
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in post student no name test: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Name, email, password, and role are required"));
    }

    @Test
    void testPostStudentDuplicateEmail() {
        when(mockDao.getStudent("tester@email.fi")).thenReturn(new Student(1, "Existing", "tester@email.fi", "user"));
        String requestBody = "{\"name\":\"Testeri\",\"email\":\"tester@email.fi\",\"password\":\"password\"}";
        MockHttpExchange exchange = new MockHttpExchange("POST", "/students", requestBody);
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in post student duplicate email test: " + e.getMessage());
        }
        assertEquals(409, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Email already in use"));
    }

    @Test
    void testHandleDeleteStudent() {
        when(mockDao.deleteStudent(1)).thenReturn(true);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/students/1", "");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in delete student test: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Student deleted successfully"));
    }

    @Test
    void testHandleDeleteNonExistentStudent() {
        when(mockDao.deleteStudent(999)).thenReturn(false);
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/students/999", "");
        exchange.withHeader("student_id", "999").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in delete non-existent student test: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Student not found"));
    }

    @Test
    void testHandleDeleteUnauthorized() {
        MockHttpExchange exchange = new MockHttpExchange("DELETE", "/students/1", "");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in delete unauthorized test: " + e.getMessage());
        }
        assertEquals(403, exchange.getResponseCode());
    }

    @Test
    void testSuccessfulNameUpdate() {
        Student student = new Student(1, "Old", "old@test.fi", "user");
        when(mockDao.getStudentById(1)).thenReturn(student);
        when(mockDao.updateStudentName(1, "New")).thenAnswer(invocation -> {
            student.setName("New");
            return true;
        });

        String requestBody = "{\"name\":\"New\"}";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/students/1", requestBody);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in successful name update test: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("New"));
    }

    @Test
    void testNameUpdateNoNameProvided() {
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/students/1", "{}");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in name update no name provided test: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("At least one of name or email must be provided"));
    }

    @Test
    void testEmailUpdate() {
        Student student = new Student(1, "Old", "old@test.fi", "user");
        when(mockDao.getStudentById(1)).thenReturn(student);
        when(mockDao.updateStudentEmail(1, "new@test.fi")).thenAnswer(invocation -> {
            student.setEmail("new@test.fi");
            return true;
        });

        String requestBody = "{\"email\":\"new@test.fi\"}";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/students/1", requestBody);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in successful name update test: " + e.getMessage());
        }
        assertEquals(200, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("new@test.fi"));
    }

    @Test
    void testEmailUpdateNoEmailProvided() {
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/students/1", "{}");
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in email update no email provided test: " + e.getMessage());
        }
        assertEquals(400, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("At least one of name or email must be provided"));
    }

    @Test
    void testUpdateDuplicateEmail() {
        Student student = new Student(2, "NumberOne", "one@test.fi", "user");
        when(mockDao.getStudent("one@test.fi")).thenReturn(student);
        String body = "{\"email\":\"one@test.fi\"}";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/students/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in update duplicate email test: " + e.getMessage());
        }
        assertEquals(409, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Email already in use"));
    }

    @Test
    void testUpdateStudentUnauthorized() {
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/students/1", "{\"name\":\"NewName\"}");
        exchange.withHeader("student_id", "2").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in update student unauthorized test: " + e.getMessage());
        }
        assertEquals(403, exchange.getResponseCode());
    }

    @Test
    void testUpdateStudentInvalidStudentId() {
        when(mockDao.getStudentById(999)).thenReturn(null);
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/students/999", "{\"email\":\"new@test.fi\"}");
        exchange.withHeader("student_id", "999").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in update student invalid student id test: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Student not found"));
    }

    @Test
    void testStudentNotUpdated() {
        when(mockDao.updateStudentName(1, "Old")).thenReturn(false);
        when(mockDao.updateStudentEmail(1, "old@test.fi")).thenReturn(false);
        String body = "{\"name\":\"Old\",\"email\":\"old@test.fi\"}";
        MockHttpExchange exchange = new MockHttpExchange("PUT", "/students/1", body);
        exchange.withHeader("student_id", "1").withHeader("role", "user");
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            fail("Exception thrown in student not updated test: " + e.getMessage());
        }
        assertEquals(404, exchange.getResponseCode());
        String json = exchange.getResponseBodyAsString();
        assertTrue(json.contains("Student not found or no changes made"));
    }
}
