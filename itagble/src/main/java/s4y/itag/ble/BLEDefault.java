package s4y.itag.ble;

import s4y.observables.Observable;
import s4y.observables.Observer;

public class BLEDefault implements BLEInterface {
    static final int TIMEOUT = 60;
    private final CBCentralManagerInterface manager;
    private final BLEConnectionsInterface connections;
    private final BLEAlertInterface alert;
    private final BLEFindMeInterface findMe;
    private final BLEScannerInterface scanner;
    private final Observable<BLEState> observableState = new Observable<>();

    BLEDefault (
            CBCentralManagerFactoryInterface managerFactory,
            BLEConnectionFactoryInterface connectionFactory,
            BLEConnectionsControlFactoryInterface connectionsControlFactory,
            BLEConnectionsFactoryInterface connectionsFactory,
            BLEAlertFactoryInterface alertFactory,
            BLEFindMeInterface findMe,
            BLEFindMeControl findMeControl,
            CBCentralManagerDelegate managerDelegate,
            BLEManagerObservablesInterface managerObservables,
            BLEPeripheralObservablesFactoryInterface peripheralObservablesFactory,
            BLEScannerFactoryInterface scannerFactory,
            BLEConnectionsStoreFactoryInterface storeFactory
    ){
        this.manager = managerFactory.manager(managerDelegate);
        final BLEConnectionsStoreInterface store = storeFactory.store(connectionFactory, findMeControl, manager, peripheralObservablesFactory);
        this.connections = connectionsFactory.connections(store, managerObservables);
        // this is cycle dependency ugly resolving
        // connections <- store <- connectionsControl <- connections
        // as a result store.setConnections is must
        store.setConnectionsControl(connectionsControlFactory.connectionsControl(connections));
        this.alert = alertFactory.alert(store);
        this.findMe = findMe;
        this.scanner = scannerFactory.scanner(connections, manager);
        managerObservables.getWillRestoreState().subscribe(new Observer<CBPeripheralInterace[]>() {
            @Override
            public void onNext(CBPeripheralInterace[] peripherals) {
                store.restorePeripherals(peripherals);
            }
        });
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
        return manager.state() == CBManagerState.poweredOn ? BLEState.ON : BLEState.OFF;
    }

    @Override
    public Observable<BLEState> observableState() {
        return observableState;
    }
}
