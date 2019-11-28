package s4y.itag.ble;

import java.util.Map;

import s4y.observables.Observable;

public interface BLEConnectionsInterface {
    class BLEStateNotification {
        final String id;
        final BLEConnectionState state;

        public BLEStateNotification(String id, BLEConnectionState state) {
            this.id = id;
            this.state = state;
        }
    }

    Observable<BLEStateNotification> getStateObservable();
    Map<String, BLEConnectionState> getStates();
    void connect(String id);
    void connect(String id, int timeoutSec);
    void disconnect(String id);
}
