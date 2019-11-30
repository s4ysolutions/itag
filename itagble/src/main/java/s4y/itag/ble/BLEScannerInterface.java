package s4y.itag.ble;

import s4y.rasat.Observable;

interface BLEScannerInterface {
    boolean isScanning();
    int scanningTimeout();
    Observable<Integer> observableTimer();
    Observable<BLEScanResult> observableScan();

    void start(int timeout, String[] forceCancelIds);
    void stop();
}
