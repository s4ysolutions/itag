package s4y.itag.itag;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import s4y.itag.BuildConfig;
import s4y.itag.ITagApplication;
import s4y.itag.MediaPlayerUtils;
import s4y.itag.ble.AlertVolume;
import s4y.itag.ble.BLEConnectionInterface;
import s4y.itag.ble.BLEConnectionState;
import s4y.itag.ble.BLEDefault;
import s4y.itag.ble.BLEInterface;
import s4y.itag.history.HistoryRecord;
import s4y.rasat.DisposableBag;

import static s4y.itag.Notifications.cancelDisconnectNotification;
import static s4y.itag.Notifications.sendDisconnectNotification;

public class ITag {
    private static final String LT = ITag.class.getName();
    public static final int BLE_TIMEOUT = 30;
    public static final int SCAN_TIMEOUT = 25;
    public static BLEInterface ble;
    public static ITagsStoreInterface store;

    private static final Map<String, AutoCloseable> reconnectListeners = new HashMap<>();
    private static final DisposableBag disposables = new DisposableBag();
    private static final DisposableBag disposablesConnections = new DisposableBag();

    private static final Map<String, Thread> asyncConnections = new HashMap<>();
    private static final Map<String, DisposableBag> connectionBags = new HashMap<>();

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
        subscribeDisconnections();
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
            subscribeDisconnections();
        }));

    }

    public static void close() {
        synchronized (reconnectListeners) {
            for (String id : reconnectListeners.keySet()) {
                disableReconnect(id);
            }
        }
        synchronized (asyncConnections) {
            for (Thread thread : asyncConnections.values()) {
                thread.interrupt();
            }
        }
        try {
            ble.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        synchronized (connectionBags) {
            for (DisposableBag bag : connectionBags.values()) {
                bag.dispose();
            }
        }
        disposables.dispose();
        disposablesConnections.dispose();
    }

    private static void subscribeDisconnections() {
        disposablesConnections.dispose();
        for (int i = 0; i < store.count(); i++) {
            final ITagInterface itag = store.byPos(i);
            if (itag != null && itag.isAlertDisconnected()) {
                BLEConnectionInterface connection = ble.connectionById(itag.id());
                disposablesConnections.add(connection.observableState().subscribe(event -> {
                            if (BuildConfig.DEBUG) Log.d(LT, "connection " + connection.id() + " state " + connection.state());
                            if (itag.isAlertDisconnected() && BLEConnectionState.disconnected.equals(connection.state())) {
                                if (itag.alertDelay() == 0) {
                                    if (BuildConfig.DEBUG) Log.d(LT, "connection " + connection.id() + " lost");
                                    MediaPlayerUtils.getInstance().startSoundDisconnected(ITagApplication.context);
                                    sendDisconnectNotification(ITagApplication.context, itag.name());
                                    HistoryRecord.add(ITagApplication.context, itag.id());
                                } else {
                                    if (BuildConfig.DEBUG) Log.d(LT, "connection " +
                                            connection.id() + " lost will be delayed by "+
                                            (itag.alertDelay()*1000)+"ms");
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        if (BuildConfig.DEBUG) Log.d(LT, "connection " +
                                                connection.id() + " lost posted, state="+
                                                connection.state());
                                        if (itag.isAlertDisconnected() && !connection.isConnected()) {
                                            if (BuildConfig.DEBUG) Log.d(LT, "connection " + connection.id() + " lost");
                                            MediaPlayerUtils.getInstance().startSoundDisconnected(ITagApplication.context);
                                            sendDisconnectNotification(ITagApplication.context, itag.name());
                                            HistoryRecord.add(ITagApplication.context, itag.id());
                                        }
                                    }, itag.alertDelay() * 1000);
                                }
                            } else if (BLEConnectionState.connected.equals(connection.state())) {
                                if (BuildConfig.DEBUG) Log.d(LT, "connection " + connection.id() + " restored");
                                MediaPlayerUtils.getInstance().stopSound(ITagApplication.context);
                                cancelDisconnectNotification(ITagApplication.context);
                                HistoryRecord.clear(ITagApplication.context, itag.id());
                            }
                        }
                ));
                disposablesConnections.add(connection.observableClick().subscribe(click -> {
                    if (click != 0 && connection.isAlerting()) {
                        new Thread(() -> connection.writeImmediateAlert(AlertVolume.NO_ALERT, ITag.BLE_TIMEOUT)).start();
                    } else {
                        if (connection.isFindMe() && !MediaPlayerUtils.getInstance().isSound()) {
                            MediaPlayerUtils.getInstance().startFindPhone(ITagApplication.context);
                        } else {
                            if (connection.isConnected()) {
                                MediaPlayerUtils.getInstance().stopSound(ITagApplication.context);
                            }
                        }
                    }
                }));
            }
        }
    }

    private static int connectThreadsCount = 0;

    public static void connectAsync(final BLEConnectionInterface connection) {
        connectAsync(connection, true);
    }

    @SuppressWarnings("SameParameterValue")
    private static void connectAsync(final BLEConnectionInterface connection, boolean infinity) {
        connectAsync(connection, infinity, null);
    }

    @SuppressWarnings("unused")
    static void connectAsync(final BLEConnectionInterface connection, Runnable onComplete) {
        connectAsync(connection, true, onComplete);
    }

    @SuppressWarnings("SameParameterValue")
    public static void connectAsync(final BLEConnectionInterface connection, boolean infinity, Runnable onComplete) {
        synchronized (asyncConnections) {
            if (asyncConnections.containsKey(connection.id())) {
                return;
            }
        }
        DisposableBag disposableBagTmp;
        synchronized (connectionBags) {
            disposableBagTmp = connectionBags.get(connection.id());
            if (disposableBagTmp == null) {
                disposableBagTmp = new DisposableBag();
                connectionBags.put(connection.id(), disposableBagTmp);
            } else {
                disposableBagTmp.dispose();
            }
        }

        final ITagInterface itag = store.byId(connection.id());
        if (itag == null) {
            return;
        }
        connectThreadsCount++;
        if (BuildConfig.DEBUG) {
            Log.d(LT, "BLE Connect thread started, count = " + connectThreadsCount);
        }
        Thread thread = new Thread("BLE Connect " + connection.id() + " " + System.currentTimeMillis()) {
            @Override
            public void run() {
                do {
                    if (BuildConfig.DEBUG) {
                        Log.d(LT, "BLE Connect thread connect " + connection.id() + "/" + itag.name() + " " + Thread.currentThread().getName());
                    }
                    connection.connect(infinity);
                } while (!isInterrupted() && itag.isAlertDisconnected() && infinity && !connection.isConnected());
                // stop sound on connection in any case
                MediaPlayerUtils.getInstance().stopSound(ITagApplication.context);
                if (!isInterrupted()) {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
                synchronized (asyncConnections) {
                    asyncConnections.remove(connection.id());
                }
                connectThreadsCount--;
                if (BuildConfig.DEBUG) {
                    Log.d(LT, "BLE Connect thread finished, count = " + connectThreadsCount);
                }
            }
        };
        synchronized (asyncConnections) {
            asyncConnections.put(connection.id(), thread);
        }
        thread.start();
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
