package s4y.itag.ble;

import androidx.annotation.NonNull;

class BLEConnectionFactoryDefault implements BLEConnectionFactoryInterface {
    @Override
    public BLEConnectionInterface connection(
            @NonNull BLECentralManagerInterface manager,
            @NonNull String id,
            Boolean debug) {

        return new BLEConnectionDefault(manager, id, debug);
    }

    @Override
    public BLEConnectionInterface connection(@NonNull BLECentralManagerInterface manager,
                                             @NonNull BLEPeripheralInterace peripheral, Boolean debug) {
        return new BLEConnectionDefault(manager, peripheral, debug);
    }

}
