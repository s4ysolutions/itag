package s4y.itag.ble;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import s4y.rasat.android.ChannelDistinct;
import s4y.rasat.Observable;

public class BLEDefault implements BLEInterface {
    private static BLEInterface _shared;

    public static BLEInterface shared(Context context) {
        if (_shared == null) {
            _shared = new BLEDefault(
                    context,
                    new BLEConnectionFactoryDefault(),
                    new BLECentralManagerDefault(context),
                    new BLEScannerFactoryDefault()
            );
        }
        return _shared;
    }

    private final BLECentralManagerInterface manager;
    private final BLEScannerInterface scanner;
    private final ChannelDistinct<BLEState> channelState;
    private final Context context;
    private final BLEConnectionFactoryInterface connectionFactory;
    private final Map<String, BLEConnectionInterface> map = new HashMap<>();
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
            BLECentralManagerInterface manager,
            BLEScannerFactoryInterface scannerFactory
    ) {
        this.context = context;
        this.connectionFactory = connectionFactory;
        this.manager = manager;
        this.channelState = new ChannelDistinct<>(manager.state());
        this.scanner = scannerFactory.scanner(manager);
        context.registerReceiver(
                stateReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        );
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

    @NonNull
    @Override
    public BLEConnectionInterface connectionById(String id) {
        BLEConnectionInterface connection = map.get(id);
        if (connection == null) {
            connection = connectionFactory.connection(manager, id);
            map.put(id, connection);
        }
        return connection;
    }

    @Override
    public void close() {
        context.unregisterReceiver(stateReceiver);
    }
}
