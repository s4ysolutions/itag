package s4y.itag.ble;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import solutions.s4y.rasat.ChannelDistinct;
import solutions.s4y.rasat.Observable;

public class BLEDefault implements BLEInterface {
    private static final String LT = BLEDefault.class.getName();
    private static BLEInterface _shared;

    public static BLEInterface shared(Context context, List<String> devices_ids) {
        if (_shared == null) {
            _shared = new BLEDefault(
                    context,
                    new BLEConnectionFactoryDefault(),
                    new BLECentralManagerDefault(context, BuildConfig.DEBUG),
                    new BLEScannerFactoryDefault(),
                    BuildConfig.DEBUG
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
    private final Boolean debug;

    private BLEDefault(
            Context context,
            BLEConnectionFactoryInterface connectionFactory,
            BLECentralManagerInterface manager,
            BLEScannerFactoryInterface scannerFactory,
            Boolean debug
    ) {
        this.context = context;
        this.connectionFactory = connectionFactory;
        this.manager = manager;
        this.channelState = new ChannelDistinct<>(manager.state());
        this.scanner = scannerFactory.scanner(manager);
        this.debug = debug;
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
        synchronized (map) {
            BLEConnectionInterface connection = map.get(id);
            if (connection == null) {
                connection = connectionFactory.connection(manager, id, debug);
                map.put(id, connection);
                Log.d(LT, "connectionById make id=" + id);
            } else {
                if (debug) {
                    Log.d(LT, "connectionById reused id=" + id);
                }
            }
            return connection;
        }
    }

    @Override
    public void close() {
        for (BLEConnectionInterface connection: map.values()) {
            try {
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        context.unregisterReceiver(stateReceiver);
    }
}
