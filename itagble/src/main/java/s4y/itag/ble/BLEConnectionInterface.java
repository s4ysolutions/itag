package s4y.itag.ble;

import s4y.rasat.Observable;

public interface BLEConnectionInterface {
    Observable<AlertVolume> observableImmediateAlert();

    boolean isConnected();
    BLEError connect() throws InterruptedException, BLEException;
    BLEError disconnect(int timeout) throws BLEException;
    BLEError connect(int timeout) throws BLEException;
    BLEError writeImmediateAlert(AlertVolume volume, int timeout);
    BLEError writeImmediateAlert(AlertVolume volume);
    int getLastStatus();
}
