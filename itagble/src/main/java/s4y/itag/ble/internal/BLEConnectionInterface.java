package s4y.itag.ble.internal;

import s4y.itag.ble.BLEException;

public interface BLEConnectionInterface {
    boolean isConnected();
    // var immediateAlertUpdateNotification: Subject<(id: String, volume: AlertVolume)> { payload }
    boolean hasPeripheral();
    BLEError establishConnection() throws InterruptedException, BLEException;
    BLEError disconnect(int timeout) throws BLEException;
    BLEError makeAvailabe(int timeout) throws BLEException;
    BLEError writeImmediateAlert(AlertVolume volume, int timeout);
    BLEError writeImmediateAlert(AlertVolume volume);
}
