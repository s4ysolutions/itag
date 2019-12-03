package s4y.itag.ble;

import androidx.annotation.NonNull;

import s4y.rasat.Channel;

public interface BLEInterface {
    BLEAlertInterface alert();
    BLEConnectionsInterface connections();
    BLEFindMeInterface findMe();
    BLEScannerInterface scanner();
    @NonNull BLEState state();
    Channel<BLEState> observableState();
}
