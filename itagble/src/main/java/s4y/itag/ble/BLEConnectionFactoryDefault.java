package s4y.itag.ble;

import androidx.annotation.NonNull;

class BLEConnectionFactoryDefault implements BLEConnectionFactoryInterface {
    @Override
    public BLEConnectionInterface connection(
            @NonNull BLECentralManagerInterface manager,
            @NonNull String id) {

        return new BLEConnectionDefault(manager, id);
    }

    @Override
    public BLEConnectionInterface connection(@NonNull BLECentralManagerInterface manager,
                                             @NonNull BLEPeripheralInterace peripheral) {
        return new BLEConnectionDefault(manager, peripheral);
    }

}
