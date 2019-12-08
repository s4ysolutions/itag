package s4y.itag.ble;

import androidx.annotation.NonNull;

interface BLEConnectionFactoryInterface {
    BLEConnectionInterface connection(
            @NonNull BLECentralManagerInterface manager,
            @NonNull String id);

    BLEConnectionInterface connection(@NonNull BLECentralManagerInterface manager,
                                      @NonNull BLEPeripheralInterace peripheral);
}
