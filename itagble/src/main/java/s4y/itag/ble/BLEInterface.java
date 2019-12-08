package s4y.itag.ble;

import androidx.annotation.NonNull;

import s4y.rasat.Observable;

public interface BLEInterface extends AutoCloseable {
    BLEScannerInterface scanner();
    @NonNull BLEState state();
    @SuppressWarnings("UnusedReturnValue")
    BLEError enable();
    Observable<BLEState> observableState();
    @NonNull BLEConnectionInterface connectionById(String id);
}
