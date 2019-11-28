package s4y.itag.ble.internal;

import s4y.itag.ble.BLEException;
import s4y.observables.Observable;

public interface BLEConnectionInterface {
    boolean isConnected();
    Observable<AlertVolume> observableImmediateAlert();
    BLEError connect() throws InterruptedException, BLEException;
    BLEError disconnect(int timeout) throws BLEException;
    BLEError connect(int timeout) throws BLEException;
    BLEError writeImmediateAlert(AlertVolume volume, int timeout);
    BLEError writeImmediateAlert(AlertVolume volume);
    int getLastStatus();
}
