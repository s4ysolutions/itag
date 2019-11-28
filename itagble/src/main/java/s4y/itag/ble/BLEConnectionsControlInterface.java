package s4y.itag.ble;

import androidx.annotation.NonNull;

public interface BLEConnectionsControlInterface {
    void setState(@NonNull String id, BLEConnectionState state);
}
