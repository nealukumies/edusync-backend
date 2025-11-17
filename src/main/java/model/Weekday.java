package model;

/**
 * Enum representing the days of the week.
 */
public enum Weekday {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    public static Weekday fromString(String dbValue) {
        return Weekday.valueOf(dbValue.toUpperCase());
    }
}
