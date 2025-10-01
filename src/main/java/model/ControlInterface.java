/**
 * This interface defines the control operations for user authentication and data retrieval.
 */

package model;

public interface ControlInterface {
    int handleLogin();
    void handleGetData(int userId);
    void displayOptions();
}
