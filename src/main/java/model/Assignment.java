package model;

import java.sql.Timestamp;
import java.util.Date;

/**
 * This class represents an assignment entity in the system.
 */

public class Assignment {
    private final int assignmentId;
    private final int studentId;
    private final Integer courseId;
    private final String title;
    private final String description;
    private final String deadline;
    private final Status status;

    /**
     * Constructor for Assignment.
     * @param assignmentId The unique identifier for the assignment.
     * @param studentId The ID of the student to whom the assignment belongs.
     * @param courseId The ID of the course associated with the assignment (nullable).
     * @param title The title of the assignment.
     * @param description The description of the assignment.
     * @param deadline The deadline of the assignment as a Timestamp.
     * @param status The current status of the assignment.
     */
    public Assignment(int assignmentId, int studentId, Integer courseId, String title, String description, Timestamp deadline, Status status) {
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.courseId = courseId; //note that courseId is nullable in the database
        this.title = title;
        this.description = description;
        this.deadline = DateFormater.formatTimestampToString(deadline);
        this.status = status;
    }

    public int getAssignmentId() {
        return assignmentId;
    }
    public int getStudentId() {
        return studentId;
    }
    public Integer getCourseId() {
        return courseId;
    }
    public String getTitle() {
        return title;
    }
    public String getDescription() {
        return description;
    }
    public Timestamp getDeadline() {
        final Date d = DateFormater.parseStringToTimestamp(deadline);
        return d != null ? new Timestamp(d.getTime()) : null;
    }
    public Status getStatus() {
        return status;
    }

    // For debugging purposes
    @Override
    public String toString() {
        return "Assignment{" +
                "assignmentId=" + assignmentId +
                ", studentId=" + studentId +
                ", courseId=" + courseId +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", deadline=" + deadline +
                ", status=" + status +
                '}';
    }
}
