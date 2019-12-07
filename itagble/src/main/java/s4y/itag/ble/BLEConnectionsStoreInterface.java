package s4y.itag.ble;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

interface BLEConnectionsStoreInterface extends AutoCloseable {
    void setConnectionsControl(BLEConnectionsControlInterface connectionsControl);
    @Nullable
    BLEConnectionInterface get(String id);
    @NonNull
    BLEConnectionInterface getOrMake(String id);
    void restorePeripherals(BLEPeripheralInterace[] peripherals);
}
