package s4y.itag.ble;

import java.util.Map;

import s4y.rasat.Channel;

public interface BLEConnectionsInterface {
    class BLEStateNotification {
        final String id;
        final BLEConnectionState state;

        public BLEStateNotification(String id, BLEConnectionState state) {
            this.id = id;
            this.state = state;
        }
    }

    Channel<BLEStateNotification> getStateChannel();
    Map<String, BLEConnectionState> getStates();
    void connect(String id);
    void connect(String id, int timeoutSec);
    void disconnect(String id);
    Channel<AlertUpdateNotificationEvent> immediateAlertUpdateNotificationChannel();
}
