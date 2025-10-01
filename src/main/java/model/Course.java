/**
 * Course class represents a course taken by a student.
 */

package model;

import java.util.Date;

public class Course {
    private int courseId;
    private int studentId;
    private String courseName;
    private String startDate;
    private String endDate;

    public Course(int courseId, int studentId, String courseName, Date startDate, Date endDate) {
        this.courseId = courseId;
        this.studentId = studentId;
        this.courseName = courseName;
        this.startDate = DateFormater.formatDateToString(startDate);
        this.endDate = DateFormater.formatDateToString(endDate);
    }

    @Override
    public String toString() {
        return "Course{" +
                "courseId=" + courseId +
                ", studentId=" + studentId +
                ", courseName='" + courseName + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }

    public int getCourseId() {
        return courseId;
    }
    public int getStudentId() {
        return studentId;
    }
    public String getCourseName() {
        return courseName;
    }
    public Date getStartDate() {
        return DateFormater.parseStringToDate(startDate);
    }
    public Date getEndDate() {
        return DateFormater.parseStringToDate(endDate);
    }
    public void setCourseName(String name) {this.courseName = name;}
    public void setStartDate(Date startDate) {
        this.startDate = DateFormater.formatDateToString(startDate);
    }
    public void setEndDate(Date endDate) {
        this.endDate = DateFormater.formatDateToString(endDate);
    }
}
