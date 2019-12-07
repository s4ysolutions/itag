package s4y.itag.ble;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;

import s4y.rasat.Channel;
import s4y.rasat.ChannelDistinct;
import s4y.rasat.Observable;

public class BLEDefault implements BLEInterface {
    private static BLEInterface _shared;

    public static BLEInterface shared(Context context) {
        if (_shared == null) {
            _shared = new BLEDefault(
                    context,
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
    private final ChannelDistinct<BLEState> channelState;
    private final Context context;
    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
//                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
//               if (state == BluetoothAdapter.STATE_ON || state == BluetoothAdapter.STATE_OFF) {
                channelState.broadcast(manager.state());
            }
        }
    };

    private BLEDefault(
            Context context,
            BLEConnectionFactoryInterface connectionFactory,
            BLEConnectionsControlFactoryInterface connectionsControlFactory,
            BLEConnectionsFactoryInterface connectionsFactory,
            BLECentralManagerInterface manager,
            BLEFindMeInterface findMe,
            BLEScannerFactoryInterface scannerFactory,
            BLEAlertFactoryInterface alertFactory,
            BLEFindMeControlFactoryInterface findMeControlFactory,
            BLEConnectionsStoreFactoryInterface storeFactory
    ) {
        this.manager = manager;
        this.channelState = new ChannelDistinct<>(manager.state());
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
        this.context = context;
        context.registerReceiver(
                stateReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        );
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
    public BLEError enable() {
        return manager.enable();
    }

    @Override
    public Observable<BLEState> observableState() {
        return channelState.observable;
    }

    @Override
    public void close() {
        context.unregisterReceiver(stateReceiver);
    }
}
