package s4y.itag.ble;

import androidx.annotation.NonNull;

interface BLEConnectionFactoryInterface {
    BLEConnectionInterface connection(
            @NonNull BLECentralManagerInterface manager,
            @NonNull String id,
            Boolean debug);

    BLEConnectionInterface connection(@NonNull BLECentralManagerInterface manager,
                                      @NonNull BLEPeripheralInterace peripheral,
                                      Boolean debug);
}
