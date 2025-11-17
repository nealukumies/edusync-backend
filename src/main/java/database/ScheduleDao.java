package database;

import model.Schedule;
import model.Weekday;
import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.sql.Time.valueOf;

/**
 * Data Access Object (DAO) for managing schedules in the database.
 */
public class ScheduleDao {
    private static final Logger LOGGER = Logger.getLogger(ScheduleDao.class.getName());
    private static final String COURSE_KEY = "course_id";
    private static final String WEEKDAY_KEY = "weekday";
    private static final String START_TIME_KEY = "start_time";
    private static final String END_TIME_KEY = "end_time";

    /**
     * Inserts a new schedule into the schedules table. Returns the created Schedule object, or null if insertion fails.
     *
     * @param courseId The ID of the course.
     * @param weekday The day of the week.
     * @param startTime The start time of the schedule.
     * @param endTime The end time of the schedule.
     * @return Schedule object or null
     */
    @SuppressWarnings("PMD.MethodArgumentCouldBeFinal")
    public Schedule insertSchedule(Integer courseId, Weekday weekday, LocalTime startTime, LocalTime endTime) {
        if (courseId == null || weekday == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("Course ID, weekday, start time, and end time must not be null");
        }

        if (!startTime.isBefore(endTime) || startTime.equals(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "INSERT INTO schedule (course_id, weekday, start_time, end_time) VALUES (?, ?, ?, ?);";
        Schedule result = null;
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            preparedStatement.setInt(1, courseId);
            preparedStatement.setString(2, weekday.name());
            preparedStatement.setTime(3, valueOf(startTime));
            preparedStatement.setTime(4, valueOf(endTime));
            final int rows = preparedStatement.executeUpdate();
            if (rows > 0) {
                try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                    if (resultSet.next()) {
                        final int newId = resultSet.getInt(1);
                        result = new Schedule(newId, courseId, weekday, startTime, endTime);
                    }
                }
            }
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to insert schedule: " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Deletes a schedule by its ID.
     *
     * @param scheduleId The ID of the schedule to delete.
     * @return boolean
     */
    public boolean deleteSchedule(final int scheduleId) {
        boolean success = false;
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "DELETE FROM schedule WHERE schedule_id = ?;";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)){
            preparedStatement.setInt(1, scheduleId);
            final int rows = preparedStatement.executeUpdate();
            success = rows > 0;
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to delete schedule: " + e.getMessage());
            }
        }
        return success;
    }

    /**
     * Retrieves a schedule by its ID.
     *
     * @param scheduleId
     * @return Schedule
     */
    public Schedule getSchedule(final int scheduleId) {
        Schedule result = null;
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT course_id, weekday, start_time, end_time FROM schedule WHERE schedule_id = ?;";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)){
            preparedStatement.setInt(1, scheduleId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    final int courseId = resultSet.getInt(COURSE_KEY);
                    final Weekday weekday = Weekday.fromString(resultSet.getString(WEEKDAY_KEY));
                    final LocalTime startTime = resultSet.getTime(START_TIME_KEY).toLocalTime();
                    final LocalTime endTime = resultSet.getTime(END_TIME_KEY).toLocalTime();
                    result = new Schedule(scheduleId, courseId, weekday, startTime, endTime);
                }
            }
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get schedule: " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Retrieves all schedules for a given course ID.
     *
     * @param courseId The ID of the course.
     * @return ArrayList<Schedule>
     */
    public List<Schedule> getAllSchedulesForCourse(final int courseId) {
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT schedule_id, course_id, weekday, start_time, end_time  FROM schedule WHERE course_id = ?;";
        final List<Schedule> schedules = new ArrayList<>();
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)){
            preparedStatement.setInt(1, courseId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    final int scheduleId = resultSet.getInt("schedule_id");
                    final int dbCourseId = resultSet.getInt(COURSE_KEY);
                    final Weekday weekday = Weekday.fromString(resultSet.getString(WEEKDAY_KEY));
                    final LocalTime startTime = resultSet.getTime(START_TIME_KEY).toLocalTime();
                    final LocalTime endTime = resultSet.getTime(END_TIME_KEY).toLocalTime();
                    schedules.add(new Schedule(scheduleId, dbCourseId, weekday, startTime, endTime));
                }
            }
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get schedules for course: " + e.getMessage());
            }
        }
        return schedules;
    }

    /**
     * Retrieves all schedules for all courses of a given student ID.
     *
     * @param studentId The ID of the student.
     * @return ArrayList<Schedule>
     */

    public List<Schedule> getAllSchedulesForStudent(final int studentId) {
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT s.schedule_id, s.course_id, s.weekday, s.start_time, s.end_time " +
                     "FROM schedule s " +
                     "JOIN courses c ON s.course_id = c.course_id " +
                     "WHERE c.student_id = ?;";
        final List<Schedule> schedules = new ArrayList<>();
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)){
            preparedStatement.setInt(1, studentId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    final int scheduleId = resultSet.getInt("schedule_id");
                    final int courseId = resultSet.getInt(COURSE_KEY);
                    final Weekday weekday = Weekday.fromString(resultSet.getString(WEEKDAY_KEY));
                    final LocalTime startTime = resultSet.getTime(START_TIME_KEY).toLocalTime();
                    final LocalTime endTime = resultSet.getTime(END_TIME_KEY).toLocalTime();
                    schedules.add(new Schedule(scheduleId, courseId, weekday, startTime, endTime));
                }
            }
            return schedules;
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get schedules for student: " + e.getMessage());
            }
            return schedules; // Return empty list on failure
        }
    }

    /**
     * Updates an existing schedule identified by scheduleId with new details.
     *
     * @param scheduleId The ID of the schedule to update.
     * @param courseId The ID of the course.
     * @param weekday The day of the week.
     * @param startTime The start time of the schedule.
     * @param endTime The end time of the schedule.
     * @return boolean
     */
    public boolean updateSchedule(int scheduleId, Integer courseId, Weekday weekday, LocalTime startTime, LocalTime endTime) {
        if (courseId == null || weekday == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("Course ID, weekday, start time, and end time must not be null");
        }

        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "UPDATE schedule SET course_id = ?, weekday = ?, start_time = ?, end_time = ? WHERE schedule_id = ?;";
        boolean success = false;
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, courseId);
            ps.setString(2, weekday.name());
            ps.setTime(3, valueOf(startTime));
            ps.setTime(4, valueOf(endTime));
            ps.setInt(5, scheduleId);
            final int rows = ps.executeUpdate();
            success = rows > 0; // Return true if a row was updated
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to update schedule: " + e.getMessage());
            }
        }
        return success;
    }
}
