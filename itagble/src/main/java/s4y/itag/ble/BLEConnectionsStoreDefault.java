package s4y.itag.ble;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

class BLEConnectionsStoreDefault implements BLEConnectionsStoreInterface {
    private final Map<String, BLEConnectionInterface> map = new HashMap<>();
    private final BLEConnectionFactoryInterface connectionFactory;
    private BLEConnectionsControlInterface connectionsControl;
    private final BLEFindMeControlInterface findMeControl;
    private final BLECentralManagerInterface manager;

    BLEConnectionsStoreDefault(BLEConnectionFactoryInterface connectionFactory,
                               BLEFindMeControlInterface findMeControl,
                               BLECentralManagerInterface manager) {
        this.connectionFactory = connectionFactory;
        this.findMeControl = findMeControl;
        this.manager = manager;
    }


    @Override
    public void setConnectionsControl(BLEConnectionsControlInterface connectionsControl) {
        this.connectionsControl = connectionsControl;
    }

    @Nullable
    @Override
    public BLEConnectionInterface get(String id) {
        return map.get(id);
    }

    @NonNull
    public BLEConnectionInterface getOrMake(String id) {
        BLEConnectionInterface connection = map.get(id);
        if (connection == null) {
            connection = connectionFactory.connection(connectionsControl, findMeControl, manager, id);
            map.put(id, connection);
        }
        return connection;
    }

    @Override
    public void restorePeripherals(BLEPeripheralInterace[] peripherals) {

    }
}
