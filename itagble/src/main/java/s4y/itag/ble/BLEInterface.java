package s4y.itag.ble;

import s4y.itag.ble.internal.BLEAlertInterface;

public interface BLEInterface {
    BLEAlertInterface getAlert();
    BLEConnectionsInterface getConnections();
    BLEFindMeInterface getFindMe();
    BLEScannerInterface getScanner();
    BLEState getState();
    void addOnBLEStateChangeListener(OnBLEStateChangeListener onBLEStateChangeListener);
    void removeOnBLEStateChangeListener(OnBLEStateChangeListener onBLEStateChangeListener);
}
