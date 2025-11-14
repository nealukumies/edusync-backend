/**
 * Data Access Object (DAO) for managing schedules in the database.
 */
package database;

import model.Schedule;
import model.Weekday;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.sql.Time.valueOf;

@SuppressWarnings("PMD.AtLeastOneConstructor")
public class ScheduleDao {
    private static final Logger LOGGER = Logger.getLogger(ScheduleDao.class.getName());

    /**
     * Inserts a new schedule into the schedules table. Returns the created Schedule object, or null if insertion fails.
     * @param courseId
     * @param weekday
     * @param startTime
     * @param endTime
     * @return Schedule object or null
     */
    public Schedule insertSchedule(Integer courseId, Weekday weekday, LocalTime startTime, LocalTime endTime) {
        if (courseId == null || weekday == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("Course ID, weekday, start time, and end time must not be null");
        }

        if (!startTime.isBefore(endTime) || startTime.equals(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "INSERT INTO schedule (course_id, weekday, start_time, end_time) VALUES (?, ?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)){
            ps.setInt(1, courseId);
            ps.setString(2, weekday.name());
            ps.setTime(3, valueOf(startTime));
            ps.setTime(4, valueOf(endTime));
            final int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        final int newId = rs.getInt(1);
                        return new Schedule(newId, courseId, weekday, startTime, endTime);
                    }
                }
            }
            return null; // Indicate no rows affected
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to insert schedule: " + e.getMessage());
            }
            return null; // Indicate failure
        }
    }

    /**
     * Deletes a schedule by its ID. Returns true if deletion was successful, false otherwise.
     * @param scheduleId
     * @return boolean
     */
    public boolean deleteSchedule(int scheduleId) {
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "DELETE FROM schedule WHERE schedule_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, scheduleId);
            int rows = ps.executeUpdate();
            return rows > 0; // Return true if a row was deleted
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to delete schedule: " + e.getMessage());
            }
            return false; // Indicate failure
        }
    }

    /**
     * Retrieves a schedule by its ID. Returns a Schedule object if found, or null if not found or an error occurs.
     * @param scheduleId
     * @return Schedule
     */
    public Schedule getSchedule(int scheduleId) {
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT * FROM schedule WHERE schedule_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final int courseId = rs.getInt("course_id");
                    final Weekday weekday = Weekday.valueOf(rs.getString("weekday").toUpperCase());
                    final LocalTime startTime = rs.getTime("start_time").toLocalTime();
                    final LocalTime endTime = rs.getTime("end_time").toLocalTime();
                    return new Schedule(scheduleId, courseId, weekday, startTime, endTime);
                }
            }
            return null; // Schedule not found
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get schedule: " + e.getMessage());
            }
            return null; // Indicate failure
        }
    }

    /**
     * Retrieves all schedules for a given course ID. Returns a list of Schedule objects, or an empty list if none found or an error occurs.
     * @param courseId
     * @return ArrayList<Schedule>
     */
    public List<Schedule> getAllSchedulesForCourse(int courseId) {
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT * FROM schedule WHERE course_id = ?;";
        final List<Schedule> schedules = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final int scheduleId = rs.getInt("schedule_id");
                    final int dbCourseId = rs.getInt("course_id");
                    final Weekday weekday = Weekday.valueOf(rs.getString("weekday").toUpperCase());
                    final LocalTime startTime = rs.getTime("start_time").toLocalTime();
                    final LocalTime endTime = rs.getTime("end_time").toLocalTime();
                    schedules.add(new Schedule(scheduleId, dbCourseId, weekday, startTime, endTime));
                }
            }
            return schedules;
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to get schedules for course: " + e.getMessage());
            }
            return schedules; // Return empty list on failure
        }
    }

    /**
     * Retrieves all schedules for all courses of a given student ID.
     * Returns a list of Schedule objects, or an empty list if none found or an error occurs.
     * @param studentId
     * @return ArrayList<Schedule>
     */

    public List<Schedule> getAllSchedulesForStudent(int studentId) {
        final Connection conn = MariaDBConnection.getConnection();
        final String sql = "SELECT s.schedule_id, s.course_id, s.weekday, s.start_time, s.end_time " +
                     "FROM schedule s " +
                     "JOIN courses c ON s.course_id = c.course_id " +
                     "WHERE c.student_id = ?;";
        final List<Schedule> schedules = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final int scheduleId = rs.getInt("schedule_id");
                    final int courseId = rs.getInt("course_id");
                    final Weekday weekday = Weekday.valueOf(rs.getString("weekday").toUpperCase());
                    final LocalTime startTime = rs.getTime("start_time").toLocalTime();
                    final LocalTime endTime = rs.getTime("end_time").toLocalTime();
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
     * Returns true if the update was successful, false otherwise.
     * @param scheduleId
     * @param courseId
     * @param weekday
     * @param startTime
     * @param endTime
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
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, courseId);
            ps.setString(2, weekday.name());
            ps.setTime(3, valueOf(startTime));
            ps.setTime(4, valueOf(endTime));
            ps.setInt(5, scheduleId);
            final int rows = ps.executeUpdate();
            return rows > 0; // Return true if a row was updated
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to update schedule: " + e.getMessage());
            }
            return false; // Indicate failure
        }
    }
}
