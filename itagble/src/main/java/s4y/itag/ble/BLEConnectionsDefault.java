package s4y.itag.ble;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import s4y.rasat.Channel;

public class BLEConnectionsDefault implements BLEConnectionsInterface, BLEConnectionsControlInterface {

    private final Channel<BLEStateNotification> stateChannel = new Channel<>();
    private final Map<String, BLEConnectionState> states = new HashMap<>();

    @Override
    public Channel<BLEStateNotification> getStateChannel() {
        return stateChannel;
    }

    @Override
    public Map<String, BLEConnectionState> getStates() {
        return states;
    }

    @Override
    public void connect(String id) {

    }

    @Override
    public void connect(String id, int timeoutSec) {

    }

    @Override
    public void disconnect(String id) {

    }

    @Override
    public void setState(@NonNull String id, BLEConnectionState state) {

    }
}
