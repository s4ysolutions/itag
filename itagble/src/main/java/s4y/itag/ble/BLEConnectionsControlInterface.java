package s4y.itag.ble;

import androidx.annotation.NonNull;

interface BLEConnectionsControlInterface {
    void setState(@NonNull String id, BLEConnectionState state);
}
