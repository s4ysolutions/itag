package s4y.itag.ble;

import s4y.rasat.Observable;

interface BLEConnectionInterface {
    Observable<AlertVolume> observableImmediateAlert();
    Observable<Boolean> observableFindeMe();
    Observable<Boolean> observableLost();

    boolean isConnected();
    BLEError connect() throws InterruptedException;
    BLEError disconnect(int timeout);
    BLEError disconnect();
    BLEError connect(int timeout) throws InterruptedException;
    BLEError writeImmediateAlert(AlertVolume volume, int timeout);
    BLEError writeImmediateAlert(AlertVolume volume);
    int getLastStatus();
}
