package s4y.itag.ble;

import s4y.rasat.Channel;

class BLEDefault implements BLEInterface {
    static final int TIMEOUT = 60;
    private final BLECentralManagerInterface manager;
    private final BLEConnectionsInterface connections;
    private final BLEAlertInterface alert;
    private final BLEFindMeInterface findMe;
    private final BLEScannerInterface scanner;
    private final Channel<BLEState> channelState = new Channel<>();

    BLEDefault (
            BLECentralManagerInterface manager,
            BLEConnectionFactoryInterface connectionFactory,
            BLEConnectionsControlFactoryInterface connectionsControlFactory,
            BLEConnectionsFactoryInterface connectionsFactory,
            BLEAlertFactoryInterface alertFactory,
            BLEFindMeInterface findMe,
            BLEFindMeControlInterface findMeControl,
            BLEScannerFactoryInterface scannerFactory,
            BLEConnectionsStoreFactoryInterface storeFactory
    ){
        this.manager = manager;
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

    @Override
    public BLEState state() {
        return manager.state() == BLECentralManagerState.poweredOn ? BLEState.ON : BLEState.OFF;
    }

    @Override
    public Channel<BLEState> observableState() {
        return channelState;
    }
}
