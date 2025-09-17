package model;

import org.junit.jupiter.api.Test;

import static model.AssignmentsModifier.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentsModifierTest {
    private int assignmentId;

    @Test
    void tryAddAssignment() {
        assignmentId = addAssignment(32,1,"Test","Test","2024-12-12");
        assertNotEquals(-1, assignmentId);
        removeAssignment(assignmentId);
    }

    @Test
    void tryChangeAssignmentStatus() {
        assertTrue(changeAssignmentStatus(140, Status.COMPLETED));
    }

    @Test
    void tryRemoveAssignment() {
        assignmentId = addAssignment(32,1,"Test","Test","2024-12-12");
        assertTrue(removeAssignment(assignmentId));
    }
}