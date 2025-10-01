/**
 * Enum representing the status of a task.
 * Each status has a corresponding string value used in the database.
 */
package model;

public enum Status {
    PENDING("pending"),
    IN_PROGRESS("in-progress"),
    COMPLETED("completed"),
    OVERDUE("overdue");

    private final String dbValue;
    Status(String dbValue) { this.dbValue = dbValue; }
    public String getDbValue() { return dbValue; }
    public static Status fromDbValue(String dbValue) {
        for (Status s : Status.values()) {
            if (s.getDbValue().equals(dbValue)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + dbValue);
    }
}
