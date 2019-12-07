package s4y.itag.ble;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import s4y.rasat.Observable;

public interface BLEConnectionsInterface extends AutoCloseable {
    class BLEStateNotification {
        final String id;
        final BLEConnectionState state;

        BLEStateNotification(String id, BLEConnectionState state) {
            this.id = id;
            this.state = state;
        }
    }

    Observable<BLEStateNotification> observableState();
    Map<String, BLEConnectionState> getStates();
    void connect(@NonNull String id);
    void connect(@NonNull String id, int timeoutSec) throws InterruptedException, BLEException;
    void enableRSSI(@NonNull String id);
    void disableRSSI(@NonNull String id);
    void disconnect(@NonNull String id);
    @NonNull BLEConnectionInterface byId(@NonNull String id);
}
