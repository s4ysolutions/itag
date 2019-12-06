package s4y.itag.ble;

import java.util.Map;

import s4y.rasat.Channel;

public interface BLEConnectionsInterface {
    class BLEStateNotification {
        final String id;
        final BLEConnectionState state;

        BLEStateNotification(String id, BLEConnectionState state) {
            this.id = id;
            this.state = state;
        }
    }

    Channel<BLEStateNotification> getStateChannel();
    Map<String, BLEConnectionState> getStates();
    void connect(String id) throws InterruptedException;
    void connect(String id, int timeoutSec) throws InterruptedException, BLEException;
    void enableRSSI(String id);
    void disableRSSI(String id);
    void disconnect(String id);
}
