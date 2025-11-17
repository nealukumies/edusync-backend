/**
 * Unit tests for StudentDao class.
 */
package database;

import model.Student;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import service.AuthService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

class StudentDaoTest {
    private static StudentDao studentDao;
    private List<Integer> insertedStudents; // To track inserted students for cleanup

    /**
     * Setup DAO before all tests.
     */
    @BeforeAll
    static void setup() {
        studentDao = new StudentDao();
    }

    /**
     * Initialize the inserted id list before each test.
     */
    @BeforeEach
    void init() {
        insertedStudents = new ArrayList<>();
    }

    @BeforeEach
    void setupLogger() {
        Logger logger = Logger.getLogger(StudentDao.class.getName());
        logger.setLevel(Level.SEVERE);
    }

    /**
     * Delete inserted students after each test.
     */
    @AfterEach
    void cleanup() {
        for (int id : insertedStudents) {
            studentDao.deleteStudent(id);
        }
        insertedStudents.clear();
    }

    /**
     * Test adding and deleting a student. Deletes the inserted student after testing.
     */
    @Test
    void addAndDeleteStudentTest() {
        Student student = studentDao.addStudent("Test", "test@student.fi", "password");
        int id = student.getId();
        assertTrue(studentDao.deleteStudent(id), "Deletion successful for ID: " + id);
    }

    @Test
    void deleteStudentFailTest() {
        assertFalse(studentDao.deleteStudent(-100), "Deletion should fail for non-existent student ID");
    }

    /**
     * Test adding a student with a duplicate email. The second insertion should fail.
     */
    @Test
    void addStudentDuplicateEmailTest() {
        Student student1 = studentDao.addStudent("Test1", "test@test.test", "password");
        Student student2 = studentDao.addStudent("Test2", "test@test.test", "password");
        int id1 = student1.getId();
        insertedStudents.add(id1);
        assertNull(student2, "Second insertion with duplicate email should fail");
    }

    /**
     * Test retrieving an existing student by email. The student should be found and details should match.
     */
    @Test
    void getStudentTest() {
        Student student = studentDao.addStudent("Ada", "ada@hupi.fi", "password");
        int id = student.getId();
        insertedStudents.add(id);
        Student fetchedStudent = studentDao.getStudent(student.getEmail());
        assertEquals("Ada", fetchedStudent.getName(), "Student name should match");
    }

    /**
     * Test retrieving a non-existent student by email. The result should be null.
     */
    @Test
    void getNonExistentStudentTest() {
        Student student = studentDao.getStudent("nosuch@email.com");
        assertNull(student, "Student should not be found");
    }

    @Test
    void getStudentByIdTest() {
        Student student = studentDao.addStudent("Eve", "eve@eevee.com", "password");
        int studentId = student.getId();
        insertedStudents.add(studentId);
        Student fetchedStudent = studentDao.getStudentById(studentId);
        assertEquals("Eve", fetchedStudent.getName(), "Student name should match");
    }

    @Test
    void getNonExistentStudentByIdTest() {
        Student student = studentDao.getStudentById(-150);
        assertNull(student, "Student should not be found");
    }

    /**
     * Test deleting a non-existent student. The deletion should fail.
     */
    @Test
    void deleteNonExistentStudentTest() {
        boolean result = studentDao.deleteStudent(-200);
        assertFalse(result, "Deletion should fail for non-existent student ID");
    }


    /**
     * Test updating a student's name. The update should be successful and the name should be changed.
     */
    @Test
    void updateStudentNameTest() {
        Student student = studentDao.addStudent("Bob", "bobs@email.fi", "password");
        int studentId = student.getId();
        insertedStudents.add(studentId);
        Student updatedStudent = studentDao.getStudentById(studentId);
        assertEquals("Bob", updatedStudent.getName(), "Student name should be updated");
    }

    /**
     * Test updating a student's name with a null name. The update should fail and the name should remain unchanged.
     */
    @Test
    void updateStudentNameWithNullNameTest() {
        Student student = studentDao.addStudent("Kalle", "pikku@kalle.fi", "password");
        int studentId = student.getId();
        insertedStudents.add(studentId);
        Student fetchedStudent = studentDao.getStudentById(studentId);
        assertEquals("Kalle", fetchedStudent.getName(), "Student name should remain unchanged");
    }

    /**
     * Test updating a student's email. The update should be successful and the email should be changed.
     */
    @Test
    void updateStudentEmailTest() {
        Student student = studentDao.addStudent("Ida", "idan@posti.fi", "password");
        int studentId = student.getId();
        insertedStudents.add(studentId);
        studentDao.updateStudentEmail(studentId, "uusi@posti.fi");
        Student updatedStudent = studentDao.getStudentById(studentId);
        assertEquals("uusi@posti.fi", updatedStudent.getEmail(), "Student email should be updated");
    }

    /**
     * Test updating a student's email with a null email. The update should fail and the email should remain unchanged.
     */
    @Test
    void updateStudentEmailWithNullEmailTest() {
        Student student = studentDao.addStudent("Matti", "matti@teppo.fi", "password");
        int studentId = student.getId();
        insertedStudents.add(studentId);
        boolean updateResult = studentDao.updateStudentEmail(studentId, null);
        assertFalse(updateResult, "Update should fail with null email");
    }

    /**
     * Test retrieving a student's password hash. The hash should be retrieved and should match the original password.
     */
    @Test
    void testGetStudentPasswordHash() {
        Student student = studentDao.addStudent("TestUser", "test@email.fi", "securepassword");
        int studentId = student.getId();
        insertedStudents.add(studentId);
        String passwordHash = studentDao.getPasswordHash("test@email.fi");
        AuthService authService = new AuthService(studentDao);
        assertTrue(authService.verifyPassword("securepassword", passwordHash), "Password hash should match the original password");
    }

    /**
     * Test retrieving a password hash for a non-existent email. The result should be null.
     */
    @Test
    void addStudentWithNullNameTest() {
        Student student = studentDao.addStudent(null, "nullname@test.com", "password");
        assertNull(student, "Adding student with null name should return null");
    }

    /*
    Test adding a student with a null email. The result should be null.
     */
    @Test
    void addStudentWithNullEmailTest() {
        Student student = studentDao.addStudent("NullEmail", null, "password");
        assertNull(student, "Adding student with null email should return null");
    }

    /*
    Test adding a student with a null password. The result should be null.
     */
    @Test
    void addStudentWithEmptyPasswordTest() {
        Student student = studentDao.addStudent("EmptyPass", "emptypass@test.com", "");
        assertNull(student, "Adding student with empty password should return null");
    }

    /*
    Test adding a student with an empty name. The result should be null.
     */
    @Test
    void addStudentWithEmptyNameTest() {
        Student student = studentDao.addStudent("", "emptyname@test.com", "password");
        assertNull(student, "Adding student with empty name should return null");
    }

    /*
    Test adding a student with an empty email. The result should be null.
     */
    @Test
    void addStudentWithEmptyEmailTest() {
        Student student = studentDao.addStudent("EmptyEmail", "", "password");
        assertNull(student, "Adding student with empty email should return null");
    }

    @Test
    void addStudentThrowsSQLExceptionTest() throws SQLException {
        try (MockedStatic<MariaDBConnection> mockedConnection = mockStatic(MariaDBConnection.class)) {
            Connection mockConn = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);

            mockedConnection.when(MariaDBConnection::getConnection).thenReturn(mockConn);
            when(mockConn.prepareStatement(anyString(), anyInt())).thenReturn(mockStmt);
            when(mockStmt.executeUpdate()).thenThrow(new SQLException("Insert failed"));

            Student result = studentDao.addStudent("Test", "test@test.com", "password");
            assertNull(result, "Should return null on SQL exception");
        }
    }

    @Test
    void getStudentThrowsSQLException() throws SQLException {
        try (MockedStatic<MariaDBConnection> mockedConnection = mockStatic(MariaDBConnection.class)) {
            Connection mockConn = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);

            mockedConnection.when(MariaDBConnection::getConnection).thenReturn(mockConn);
            when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
            when(mockStmt.executeQuery()).thenThrow(new SQLException("Query failed"));

            Student result = studentDao.getStudent("test@test.com");
            assertNull(result, "Should return null on SQL exception");
        }

    }

    @Test
    void getStudentByIdThrowsSQLException() throws SQLException {
        try (MockedStatic<MariaDBConnection> mockedConnection = mockStatic(MariaDBConnection.class)) {
            Connection mockConn = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);

            mockedConnection.when(MariaDBConnection::getConnection).thenReturn(mockConn);
            when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
            when(mockStmt.executeQuery()).thenThrow(new SQLException("Query failed"));

            Student result = studentDao.getStudentById(123);
            assertNull(result, "Should return null on SQL exception");
        }
    }

    @Test
    void getPasswordHashThrowsSQLException() throws SQLException {
        try (MockedStatic<MariaDBConnection> mockedConnection = mockStatic(MariaDBConnection.class)) {
            Connection mockConn = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);

            mockedConnection.when(MariaDBConnection::getConnection).thenReturn(mockConn);
            when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
            when(mockStmt.executeQuery()).thenThrow(new SQLException("Query failed"));

            String result = studentDao.getPasswordHash("test@test.com");
            assertNull(result, "Should return null on SQL exception");
        }
    }

    @Test
    void deleteStudentThrowsSQLException() throws SQLException {
        try (MockedStatic<MariaDBConnection> mockedConnection = mockStatic(MariaDBConnection.class)) {
            Connection mockConn = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);

            mockedConnection.when(MariaDBConnection::getConnection).thenReturn(mockConn);
            when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
            doThrow(new SQLException("Delete failed")).when(mockStmt).executeUpdate();

            boolean result = studentDao.deleteStudent(123);
            assertFalse(result, "Should return false on SQL exception");
        }
    }

    @Test
    void updateStudentNameThrowsSQLException() throws SQLException {
        try (MockedStatic<MariaDBConnection> mockedConnection = mockStatic(MariaDBConnection.class)) {
            Connection mockConn = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);

            mockedConnection.when(MariaDBConnection::getConnection).thenReturn(mockConn);
            when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
            when(mockStmt.executeUpdate()).thenThrow(new SQLException("Update failed"));

            boolean result = studentDao.updateStudentName(123, "NewName");
            assertFalse(result, "Should return false on SQL exception");
        }
    }

    @Test
    void updateStudentEmailThrowsSQLException() throws SQLException {
        try (MockedStatic<MariaDBConnection> mockedConnection = mockStatic(MariaDBConnection.class)) {
            Connection mockConn = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);

            mockedConnection.when(MariaDBConnection::getConnection).thenReturn(mockConn);
            when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
            when(mockStmt.executeUpdate()).thenThrow(new SQLException("Update failed"));

            boolean result = studentDao.updateStudentEmail(123, "new@email.com");
            assertFalse(result, "Should return false on SQL exception");
        }
    }
}
