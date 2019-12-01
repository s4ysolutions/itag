package s4y.itag.ble;

public interface BLEAlertInterface {
    boolean isAlerting(String id);
    void toggleAlert(String id, int timeout);
    void startAlert(String id, int timeout);
    void stopAlert(String id, int timeout);
}
