package s4y.itag.ble;

import s4y.itag.ble.internal.BLEDiscoveryResult;
import s4y.itag.ble.observables.Subject;

public interface BLEScannerInterface {
    boolean isScanning();
    int scanningTimeout();
    Subject<Integer> getTimerSubject();
    Subject<BLEDiscoveryResult> getDiscoverySubject();

    void start(int timeout, String[] forceCancelIds);
    void stop();
}
