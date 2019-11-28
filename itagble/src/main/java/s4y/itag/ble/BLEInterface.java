package s4y.itag.ble;

import s4y.observables.Observable;

public interface BLEInterface {
    BLEAlertInterface alert();
    BLEConnectionsInterface connections();
    BLEFindMeInterface findMe();
    BLEScannerInterface scanner();
    BLEState state();
    Observable<BLEState> observableState();
}
