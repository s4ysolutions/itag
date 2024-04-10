package s4y.itag.itag;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import s4y.itag.preference.VolumePreference;
import solutions.s4y.rasat.DisposableBag;

import static s4y.itag.Notifications.sendConnectNotification;
import static s4y.itag.Notifications.sendDisconnectNotification;

public class ITag {
    private static final String LT = ITag.class.getName();
    public static final int BLE_TIMEOUT = 30;
    public static final int SCAN_TIMEOUT = 60;
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
            // TODO: modify so that it doesn't connect in passive mode
            if (itag == null || !itag.isAlertEnabled()) continue;
            BLEConnectionInterface connection = ITag.ble.connectionById(itag.id());
            connectAsync(connection);
            enableReconnect(itag.id());
        }
        subscribeDisconnectionsAndConnections();
        disposables.add(store.observable().subscribe(event -> {
            Log.d("ingo", "disposables.add(store.observable().subscribe(event -> { " + event.op);
            /*ITagInterface itag = event.tag;
            boolean reconnect = store.remembered(itag.id()) && itag.isAlertOnDisconnectEnabled();
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
            }*/
            subscribeDisconnectionsAndConnections();
        }));

    }

    public static void closeApplication() {
        List<String> ids;
        synchronized (reconnectListeners) {
            ids = new ArrayList<>(reconnectListeners.keySet());
        }
        for (String id : ids) {
            disableReconnect(id);
        }

        List<Thread> threads;
        synchronized (asyncConnections) {
            threads = new ArrayList<>(asyncConnections.values());
        }
        for (Thread thread : threads) {
            thread.interrupt();
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

    private static void subscribeDisconnectionsAndConnections() {
        disposablesConnections.dispose();
        for (int i = 0; i < store.count(); i++) {
            final ITagInterface itag = store.byPos(i);
            if (itag == null) continue;
            BLEConnectionInterface connection = ble.connectionById(itag.id());
            if (itag.isAlertEnabled()){
                disposablesConnections.add(connection.observableState().subscribe(event -> {
                            if (BuildConfig.DEBUG)
                                Log.d(LT, "connection " + connection.id() + " state " + connection.state());
                            Log.e("ingo", "state = " + connection.state() + ", old state = " + connection.oldState());
                            if(connection.state() == connection.oldState()){
                                return;
                            }
                            if(
                                (BLEConnectionState.connected.equals(connection.state()) && BLEConnectionState.writting.equals(connection.oldState())) ||
                                (BLEConnectionState.writting.equals(connection.state()))
                            ){
                                connection.setOldState(connection.state());
                                return;
                            }
                            connection.setOldState(connection.state());
                            Log.d("ingo", "alert mode " + itag.alertMode());
                            if ((itag.alertMode() == TagAlertMode.alertOnDisconnect || itag.alertMode() == TagAlertMode.alertOnBoth) && BLEConnectionState.disconnected.equals(connection.state())) {
                                if (itag.alertDelay() == 0) {
                                    if (BuildConfig.DEBUG)
                                        Log.d(LT, "connection " + connection.id() + " lost");
                                    alertUser(itag, true);
                                } else {
                                    if (BuildConfig.DEBUG) Log.d(LT, "connection " +
                                            connection.id() + " lost will be delayed by " +
                                            (itag.alertDelay() * 1000) + "ms");
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        if (BuildConfig.DEBUG) Log.d(LT, "connection " +
                                                connection.id() + " lost posted, state=" +
                                                connection.state());
                                        if (itag.isAlertEnabled() && !connection.isConnected()) {
                                            if (BuildConfig.DEBUG)
                                                Log.d(LT, "connection " + connection.id() + " lost");
                                            alertUser(itag, true);
                                        }
                                    }, itag.alertDelay() * 1000L);
                                }
                            } else if (BLEConnectionState.connected.equals(connection.state())) {
                                if (BuildConfig.DEBUG)
                                    Log.d(LT, "connection " + connection.id() + " restored");
                                // TODO: play sound if connection restore alert is enabled
                                if((itag.alertMode() == TagAlertMode.alertOnConnect || itag.alertMode() == TagAlertMode.alertOnBoth)){
                                    //if(MediaPlayerUtils.getInstance().isSound()){
                                        alertUser(itag, false);
                                    //}
                                }/* else {
                                    stopSound();
                                    cancelDisconnectNotification(ITagApplication.context);
                                    HistoryRecord.clear(ITagApplication.context, itag.id());
                                }*/
                            }
                        }
                ));
            }
            disposablesConnections.add(connection.observableClick().subscribe(click -> {
                if (click != 0 && connection.isAlerting()) {
                    new Thread(() -> connection.writeImmediateAlert(AlertVolume.NO_ALERT, ITag.BLE_TIMEOUT)).start();
                } else {
                    if (connection.isFindMe() && !MediaPlayerUtils.getInstance().isSound()) {
                        MediaPlayerUtils.getInstance().startFindPhone(ITagApplication.context);
                    } else {
                        if (connection.isConnected()) {
                            stopSound();
                        }
                    }
                }
            }));
        }
    }

    static void stopSound(){
        MediaPlayerUtils.getInstance().stopSound(ITagApplication.context);
    }

    private static void alertUser(ITagInterface itag, Boolean disconnected) {
        Log.d("ingo", "setShakingOnConnectDisconnect(true)");
        itag.setShakingOnConnectDisconnect(true);
        int volume = new VolumePreference(ITagApplication.context).get();
        if (volume == VolumePreference.LOUD) {
            MediaPlayerUtils.getInstance().startSoundConnectedDisconnected(ITagApplication.context, disconnected);
        } else if (volume == VolumePreference.VIBRATION) {
            MediaPlayerUtils.getInstance().startVibrate();
        }
        if(disconnected) {
            sendDisconnectNotification(ITagApplication.context, itag.name());
        } else {
            sendConnectNotification(ITagApplication.context, itag.name());
        }
        HistoryRecord.add(ITagApplication.context, itag.id());
    }

    private static int connectThreadsCount = 0;

    public static void connectAsync(final BLEConnectionInterface connection) {
        connectAsync(connection, true);
    }

    @SuppressWarnings("SameParameterValue")
    private static void connectAsync(final BLEConnectionInterface connection, boolean infinity) {
        connectAsync(connection, infinity, null);
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
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
                } while (!isInterrupted() && itag.isAlertEnabled() && infinity && !connection.isConnected());
                // stop sound on connection in any case
                stopSound();
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

    public static void enableReconnect(String id) {
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

    public static void disableReconnect(String id) {
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
