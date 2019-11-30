package s4y.itag.ble;

import androidx.annotation.NonNull;

class BLEConnectionFactoryDefault implements BLEConnectionFactoryInterface {
    @Override
    public BLEConnectionInterface connection(
            @NonNull BLEConnectionsControlInterface connectionsControl,
            @NonNull BLEFindMeControlInterface findMeControl,
            @NonNull BLECentralManagerInterface manager,
            @NonNull String id) {

        return new BLEConnectionDefault(connectionsControl, findMeControl, manager, id);
    }

    @Override
    public BLEConnectionInterface connection(@NonNull BLEConnectionsControlInterface connectionsControl,
                                             @NonNull BLEFindMeControlInterface findMeControl,
                                             @NonNull BLECentralManagerInterface manager,
                                             @NonNull BLEPeripheralInterace peripheral) {
        return new BLEConnectionDefault(connectionsControl, findMeControl, manager, peripheral);
    }

}
