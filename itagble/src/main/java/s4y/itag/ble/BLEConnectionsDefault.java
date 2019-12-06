package s4y.itag.ble;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import s4y.rasat.Channel;

class BLEConnectionsDefault implements BLEConnectionsInterface, BLEConnectionsControlInterface, AutoCloseable {

    private final Channel<BLEStateNotification> stateChannel = new Channel<>();
    private final Map<String, BLEConnectionState> states = new HashMap<>();
    private final BLEConnectionsStoreInterface store;

    public BLEConnectionsDefault(BLEConnectionsStoreInterface store) {
        this.store = store;
    }

    @Override
    public Channel<BLEStateNotification> getStateChannel() {
        return stateChannel;
    }

    @Override
    public Map<String, BLEConnectionState> getStates() {
        return states;
    }

    @Override
    public void connect(String id) throws InterruptedException {
        connect(id, 0);
    }

    @Override
    public void connect(String id, int timeoutSec) throws InterruptedException {
        BLEConnectionInterface connection = store.getOrMake(id);
        connection.connect(timeoutSec);
    }

    @Override
    public void enableRSSI(String id) {
        BLEConnectionInterface connection = store.get(id);
        if (connection != null) {
            connection.enableRSSI();
        }
    }

    @Override
    public void disableRSSI(String id) {
        BLEConnectionInterface connection = store.get(id);
        if (connection != null) {
            connection.disableRSSI();
        }
    }

    @Override
    public void disconnect(String id) {
        BLEConnectionInterface connection = store.getOrMake(id);
        connection.disconnect();
    }

    @Override
    public void setState(@NonNull String id, BLEConnectionState state) {
        states.put(id, state);
        stateChannel.broadcast(new BLEStateNotification(id, state));
    }

    @Override
    public void close() throws Exception {

    }
}
