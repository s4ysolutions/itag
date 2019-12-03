package s4y.itag.ble;

import java.util.List;

import s4y.rasat.Observable;

public interface BLEScannerInterface {
    boolean isScanning();
    int scanningTimeout();
    Observable<Integer> observableTimer();
    Observable<BLEScanResult> observableScan();
    Observable<Boolean> observableActive();

    void start(int timeout, String[] forceCancelIds);
    void stop();
}
