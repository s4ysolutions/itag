package s4y.itag.ble;

import androidx.annotation.NonNull;

import s4y.rasat.Channel;
import s4y.rasat.Observable;

public interface BLEInterface extends AutoCloseable {
    BLEAlertInterface alert();
    BLEConnectionsInterface connections();
    BLEFindMeInterface findMe();
    BLEScannerInterface scanner();
    @NonNull BLEState state();
    BLEError enable();
    Observable<BLEState> observableState();
}
