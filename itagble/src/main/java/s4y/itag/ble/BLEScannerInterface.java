package s4y.itag.ble;

import solutions.s4y.rasat.Observable;

public interface BLEScannerInterface {
    boolean isScanning();
    Observable<Integer> observableTimer();
    Observable<BLEScanResult> observableScan();
    Observable<Boolean> observableActive();

    void start(boolean newDevices, int timeout, String[] forceCancelIds);
    void stop();
}
