package model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateFormater {
    public static String formatDateToString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        return sdf.format(date);
    }

    public static Date parseStringToDate(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            return sdf.parse(dateString);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String formatTimestampToString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        return sdf.format(date);
    }

    public static Date parseStringToTimestamp(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            return sdf.parse(dateString);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
