package s4y.itag.ble;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import s4y.rasat.Observable;
import s4y.rasat.android.Channel;

/**
 * Keeps references to connections made on demand
 * Compose the connection store and connections in
 * the singe interface
 * Provides the minimal functionality needed by a consumer
 */
class BLEConnectionsDefault implements BLEConnectionsInterface, BLEConnectionsControlInterface {

    private final Channel<BLEStateNotification> stateChannel = new Channel<>();

    private final Map<String, BLEConnectionState> states = new HashMap<>();
    private final BLEConnectionsStoreInterface store;

    BLEConnectionsDefault(BLEConnectionsStoreInterface store) {
        this.store = store;
    }

    @Override
    public Observable<BLEStateNotification> observableState() {
        return stateChannel.observable;
    }

    @Override
    public Map<String, BLEConnectionState> getStates() {
        return states;
    }

    @Override
    public void connect(@NonNull String id) {
        connect(id, 0);
    }

    @Override
    public void connect(@NonNull String id, int timeoutSec) {
        BLEConnectionInterface connection = store.getOrMake(id);
        connection.connect(timeoutSec);
    }

    @Override
    public void enableRSSI(@NonNull String id) {
        BLEConnectionInterface connection = store.get(id);
        if (connection != null) {
            connection.enableRSSI();
        }
    }

    @Override
    public void disableRSSI(@NonNull String id) {
        BLEConnectionInterface connection = store.get(id);
        if (connection != null) {
            connection.disableRSSI();
        }
    }

    @Override
    public void disconnect(@NonNull String id) {
        BLEConnectionInterface connection = store.getOrMake(id);
        connection.disconnect();
    }

    @NonNull
    @Override
    public BLEConnectionInterface byId(@NonNull String id) {
        return store.getOrMake(id);
    }

    @Override
    public void setState(@NonNull String id, BLEConnectionState state) {
        states.put(id, state);
        stateChannel.broadcast(new BLEStateNotification(id, state));
    }

    @Override
    public void close() throws Exception {
        store.close();
    }
}
