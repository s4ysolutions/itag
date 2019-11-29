package s4y.itag.ble;

import s4y.rasat.Channel;

public interface BLEInterface {
    BLEAlertInterface alert();
    BLEConnectionsInterface connections();
    BLEFindMeInterface findMe();
    BLEScannerInterface scanner();
    BLEState state();
    Channel<BLEState> observableState();
}
