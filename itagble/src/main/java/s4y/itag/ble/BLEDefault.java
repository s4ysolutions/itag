package s4y.itag.ble;

import android.content.Context;

import androidx.annotation.NonNull;

import s4y.rasat.Channel;

public class BLEDefault implements BLEInterface {
    private static BLEInterface _shared;
    public static BLEInterface shared(Context context) {
        if (_shared == null) {
            _shared = new BLEDefault(
                    new BLEConnectionFactoryDefault(),
                    new BLEConnectionsControlFactoryDefault(),
                    new BLEConnectionsFactoryDefault(),
                    new BLECentralManagerDefault(context),
                    new BLEFindMeDefault(),
                    new BLEScannerFactoryDefault(),
                    new BLEAlertFactoryDefault(),
                    new BLEFindMeControlFactoryDefault(),
                    new BLEConnectionsStoreFactoryDefault()
            );
        }
        return _shared;
    }
    static final int TIMEOUT = 60;
    private final BLECentralManagerInterface manager;
    private final BLEConnectionsInterface connections;
    private final BLEAlertInterface alert;
    private final BLEFindMeInterface findMe;
    private final BLEScannerInterface scanner;
    private final Channel<BLEState> channelState = new Channel<>();

    private BLEDefault(
            BLEConnectionFactoryInterface connectionFactory,
            BLEConnectionsControlFactoryInterface connectionsControlFactory,
            BLEConnectionsFactoryInterface connectionsFactory,
            BLECentralManagerInterface manager,
            BLEFindMeInterface findMe,
            BLEScannerFactoryInterface scannerFactory,
            BLEAlertFactoryInterface alertFactory,
            BLEFindMeControlFactoryInterface findMeControlFactory,
            BLEConnectionsStoreFactoryInterface storeFactory
    ){
        this.manager = manager;
        BLEFindMeControlInterface findMeControl = findMeControlFactory.findMeControll(findMe);
        final BLEConnectionsStoreInterface store = storeFactory.store(connectionFactory, findMeControl, manager);
        this.connections = connectionsFactory.connections(store);
        // this is cycle dependency ugly resolving
        // connections <- store <- connectionsControl <- connections
        // as a result store.setConnections is must
        store.setConnectionsControl(connectionsControlFactory.connectionsControl(connections));
        this.alert = alertFactory.alert(store);
        this.findMe = findMe;
        this.scanner = scannerFactory.scanner(connections, manager);
    }

    @Override
    public BLEAlertInterface alert() {
        return alert;
    }

    @Override
    public BLEConnectionsInterface connections() {
        return connections;
    }

    @Override
    public BLEFindMeInterface findMe() {
        return findMe;
    }

    @Override
    public BLEScannerInterface scanner() {
        return scanner;
    }

    @NonNull
    @Override
    public BLEState state() {
        return manager.state();
    }

    @Override
    public Channel<BLEState> observableState() {
        return channelState;
    }
}
