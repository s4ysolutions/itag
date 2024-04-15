package s4y.itag.ble;

import androidx.annotation.NonNull;

import solutions.s4y.rasat.Observable;

public interface BLEConnectionInterface extends AutoCloseable {
    Observable<AlertVolume> observableImmediateAlert();
    Observable<Integer> observableClick();
    Observable<Integer> observableRSSI();
    Observable<BLEConnectionState> observableState();

    @NonNull
    String id();
    boolean isConnected();
    boolean isDisconnected();
    void connect();
    BLEError disconnect(int timeout);
    BLEError disconnect();
    BLEError writeImmediateAlert(AlertVolume volume, int timeout);
    BLEError writeImmediateAlert(AlertVolume volume);
    void enableRSSI();
    void disableRSSI();
    void broadcastRSSI(int rssi);
    boolean rssiEnabled();
    int rssi();
    int getLastStatus();
    BLEConnectionState state();
    void setOldState(BLEConnectionState oldState);
    BLEConnectionState oldState();
    boolean isAlerting();
    boolean isFindMe();
    void resetFindMe();
}
