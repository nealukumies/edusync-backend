/**
 * This class provides utility methods for formatting and parsing dates and timestamps.
 */


package model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DateFormater {
    private static final Logger LOGGER = Logger.getLogger(DateFormater.class.getName());
    public static String formatDateToString(Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        return sdf.format(date);
    }

    public static Date parseStringToDate(String dateString) {
        try {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            return sdf.parse(dateString);
        } catch (ParseException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to parse date string: " + e.getMessage());
            }
            return null;
        }
    }

    public static String formatTimestampToString(Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        return sdf.format(date);
    }

    public static Date parseStringToTimestamp(String dateString) {
        try {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            return sdf.parse(dateString);
        } catch (ParseException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, () -> "Failed to parse date string: " + e.getMessage());
            }
            return null;
        }
    }
}
