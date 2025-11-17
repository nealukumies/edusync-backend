package model;

import java.time.LocalTime;

/**
 * This class represents a schedule entry for a course, including the day of the week and start/end times.
 */
public class Schedule {
    private int scheduleId;
    private int courseId;
    private Weekday weekday;
    private LocalTime startTime;
    private LocalTime endTime;

    /**
     * Constructor for Schedule.
     * @param scheduleId The unique identifier for the schedule entry.
     * @param courseId The ID of the course associated with this schedule.
     * @param weekday The day of the week for this schedule entry.
     * @param startTime The start time of the schedule entry.
     * @param endTime The end time of the schedule entry.
     */
    public Schedule(int scheduleId, int courseId, Weekday weekday, LocalTime startTime, LocalTime endTime) {
        this.scheduleId = scheduleId;
        this.courseId = courseId;
        this.weekday = weekday;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // For debugging purposes
    @Override
    public String toString() {
        return "Schedule{" +
                "scheduleId=" + scheduleId +
                ", courseId=" + courseId +
                ", weekday=" + weekday +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                '}';
    }

    public int getScheduleId() {
        return scheduleId;
    }
    public int getCourseId() {
        return courseId;
    }
    public Weekday getWeekday() {
        return weekday;
    }
    public LocalTime getStartTime() {
        return startTime;
    }
    public LocalTime getEndTime() {
        return endTime;
    }
    public void setScheduleId(int scheduleId) {this.scheduleId = scheduleId;}
    public void setCourseId(int courseId) {this.courseId = courseId;}
    public void setWeekday(Weekday weekday) {this.weekday = weekday;}
    public void setStartTime(LocalTime startTime) {this.startTime = startTime;}
    public void setEndTime(LocalTime endTime) {this.endTime = endTime;}
}
