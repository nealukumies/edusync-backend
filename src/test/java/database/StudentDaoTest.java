/**
 * Unit tests for StudentDao class.
 */
package database;

import model.Student;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
        assertTrue(id > 0, "Insertion successful, got ID: " + id);
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
        assertTrue(id1 > 0, "First insertion successful, got ID: " + id1);
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
        assertNotNull(fetchedStudent, "Student should be found");
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
        assertNotNull(fetchedStudent, "Student should be found");
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
        boolean updateResult = studentDao.updateStudentName(studentId, "Bobby");
        assertTrue(updateResult, "Update should be successful");
        Student updatedStudent = studentDao.getStudentById(studentId);
        assertNotNull(updatedStudent, "Updated student should be found");
        assertEquals("Bobby", updatedStudent.getName(), "Student name should be updated");
    }

    /**
     * Test updating a student's name with a null name. The update should fail and the name should remain unchanged.
     */
    @Test
    void updateStudentNameWithNullNameTest() {
        Student student = studentDao.addStudent("Kalle", "pikku@kalle.fi", "password");
        int studentId = student.getId();
        insertedStudents.add(studentId);
        boolean updateResult = studentDao.updateStudentName(studentId, null);
        assertFalse(updateResult, "Update should fail with null name");
        Student fetchedStudent = studentDao.getStudentById(studentId);
        assertNotNull(fetchedStudent, "Student should be found");
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
        boolean updateResult = studentDao.updateStudentEmail(studentId, "uusi@posti.fi");
        assertTrue(updateResult, "Update should be successful");
        Student updatedStudent = studentDao.getStudentById(studentId);
        assertNotNull(updatedStudent, "Updated student should be found");
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
}