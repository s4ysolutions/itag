package s4y.itag.itag;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import s4y.itag.ITagApplication;
import s4y.itag.ble.BLEConnectionInterface;
import s4y.itag.ble.BLEConnectionState;
import s4y.itag.ble.BLEDefault;
import s4y.itag.ble.BLEInterface;
import s4y.rasat.DisposableBag;

public class ITag {
    private static final String LT = ITag.class.getName();
    public static final int BLE_TIMEOUT = 30;
    public static final int SCAN_TIMEOUT = 25;
    public static BLEInterface ble;
    public static ITagsStoreInterface store;

    private static final Map<String, AutoCloseable> reconnectListeners = new HashMap<>();
    private static final DisposableBag disposables = new DisposableBag();

    public static void initITag(Context context) {
        ble = BLEDefault.shared(context);
        store = new ITagsStoreDefault(ITagApplication.context);
        for (int i = 0; i < store.count(); i++) {
            ITagInterface itag = store.byPos(i);
            if (itag != null) {
                if (itag.isAlertDisconnected()) {
                    BLEConnectionInterface connection = ITag.ble.connectionById(itag.id());
                    connectAsync(connection);
                    enableReconnect(itag.id());
                }
            }
        }
        disposables.add(store.observable().subscribe(event -> {
            ITagInterface itag = event.tag;
            boolean reconnect = store.remembered(itag.id()) && itag.isAlertDisconnected();
            BLEConnectionInterface connection = ble.connectionById(itag.id());
            if (reconnect) {
                enableReconnect(itag.id());
                if (!connection.isConnected()) {
                    connectAsync(connection);
                }
            } else {
                disableReconnect(itag.id());
                if (connection.isConnected()) {
                    new Thread(() -> connection.disconnect(BLE_TIMEOUT)).start();
                }
            }
        }));
    }

    public static void close() throws Exception {
        ble.close();
        for (String id : reconnectListeners.keySet()) {
            disableReconnect(id);
        }
        disposables.dispose();
    }

    private static int connectThreadsCount = 0;

    private static void connectAsync(final BLEConnectionInterface connection) {
        connectAsync(connection, true);
    }

    @SuppressWarnings("SameParameterValue")
    private static void connectAsync(final BLEConnectionInterface connection, boolean infinity) {
        connectThreadsCount++;
        Log.d(LT, "BLE Connect thread started, count = " + connectThreadsCount);
        new Thread("BLE Connect " + connection.id() + " " + System.currentTimeMillis()) {
            @Override
            public void run() {
                do {
                    ITagInterface itag = store.byId(connection.id());
                    Log.d(LT, "Attempt to connect " + connection.id() + "/" + (itag == null ? "null" : itag.name())+" "+Thread.currentThread().getName());
                    connection.connect(infinity);
                } while (infinity && !connection.isConnected());
                connectThreadsCount--;
                Log.d(LT, "BLE Connect thread finished, count = " + connectThreadsCount);
            }
        }.start();
    }

    private static void enableReconnect(String id) {
        disableReconnect(id);
        synchronized (reconnectListeners) {
            final BLEConnectionInterface connection = ITag.ble.connectionById(id);
            reconnectListeners.put(id, connection.observableState()
                    .subscribe(state -> {
                        if (BLEConnectionState.disconnected.equals(state)) {
                            connectAsync(connection);
                        }
                    }));
        }
    }

    private static void disableReconnect(String id) {
        synchronized (reconnectListeners) {
            AutoCloseable existing = reconnectListeners.get(id);
            if (existing != null) {
                try {
                    existing.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                reconnectListeners.remove(id);
            }
        }
    }
}
