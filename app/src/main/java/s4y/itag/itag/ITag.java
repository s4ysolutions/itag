package s4y.itag.itag;

import android.content.Context;
import android.os.Build;
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
    private static final DisposableBag disposablePassiveScanner = new DisposableBag();
    private static final DisposableBag disposablesConnections = new DisposableBag();

    private static final Map<String, Thread> asyncConnections = new HashMap<>();
    private static final Map<String, DisposableBag> connectionBags = new HashMap<>();
    private static final int PASSIVE_DISCONNECT_TIMEOUT = 2000;
    private static final android.os.Handler passiveDisconnectTimeoutHandler = new Handler(Looper.getMainLooper());
    private static final Map<String, Runnable> passiveDisconnectRunnables = new HashMap<>();
    private static void iTagPassivelyDisconnected(ITagInterface itag) {
        if(!itag.isShaking() && itag.connectionMode() == TagConnectionMode.passive) {
            ITag.store.setPassivelyDisconnected(itag.id(), true);
            alertUser(itag, true);
        }
    };

    public static void initITag(Context context) {
        store = new ITagsStoreDefault(ITagApplication.context);
        ble = BLEDefault.shared(context, store.getIds());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("ingo", "dadarata");
        }
        subscribeDisconnectionsAndConnections();
        disposables.add(store.observable().subscribe(event -> {
            Log.d("ingo", "disposables.add(store.observable().subscribe(event -> { " + event.op);
            subscribeDisconnectionsAndConnections();
        }));
        subscribePassiveScanner();
    }

    public static void unsubscribePassiveScanner(){
        disposablePassiveScanner.dispose();
    }

    public static void subscribePassiveScanner() {
        // TODO: pass bluetooth manager only devices with passive mode ON.
        disposablePassiveScanner.dispose();
        disposablePassiveScanner.add(
                ITag.ble.scanner().observableScan().subscribe((result) -> {
                    ITagInterface itag = ITag.store.byId(result.id);
                    if(itag == null) return;
                    if(itag.connectionMode() == TagConnectionMode.passive) {
                        BLEConnectionInterface connection = ITag.ble.connectionById(result.id);
                        connection.broadcastRSSI(result.rssi);
                        // to handle disconnects
                        if(itag.alertMode() == TagAlertMode.alertOnDisconnect || itag.alertMode() == TagAlertMode.alertOnBoth) {
                            Runnable runnable;
                            if (passiveDisconnectRunnables.containsKey(itag.id())) {
                                runnable = passiveDisconnectRunnables.get(itag.id());
                            } else {
                                runnable = () -> iTagPassivelyDisconnected(itag);
                                passiveDisconnectRunnables.put(itag.id(), runnable);
                            }
                            passiveDisconnectTimeoutHandler.removeCallbacks(runnable);
                            passiveDisconnectTimeoutHandler.postDelayed(runnable, PASSIVE_DISCONNECT_TIMEOUT);
                        }
                        // to handle connects
                        if(itag.alertMode() == TagAlertMode.alertOnDisconnect || itag.alertMode() == TagAlertMode.alertOnBoth) {
                            if(itag.hasPassivelyDisconnected()){
                                alertUser(itag, false);
                                ITag.store.setPassivelyDisconnected(itag.id(), false);
                            }
                        }
                    }
                    // handle reconnects for devices in active mode
                    if(itag.connectionMode() == TagConnectionMode.active && itag.reconnectMode() && ITag.ble.connectionById(itag.id()).state() == BLEConnectionState.disconnected){
                        ITag.ble.connectionById(itag.id()).connect();
                    }
                })
        );
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
            if (itag.isConnectModeEnabled()){
                disposablesConnections.add(connection.observableState().subscribe(event -> {
                            if (BuildConfig.DEBUG)
                                Log.d(LT, "connection " + connection.id() + " state " + connection.state());
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
                            if ((itag.alertMode() == TagAlertMode.alertOnDisconnect || itag.alertMode() == TagAlertMode.alertOnBoth) && BLEConnectionState.disconnected.equals(connection.state())) {
                                if (itag.alertDelay() == 0) {
                                    if (BuildConfig.DEBUG)
                                        Log.d(LT, "connection " + connection.id() + " lost");
                                    alertUser(itag, true);
                                    connection.broadcastRSSI(-999);
                                } else {
                                    if (BuildConfig.DEBUG) Log.d(LT, "connection " +
                                            connection.id() + " lost will be delayed by " +
                                            (itag.alertDelay() * 1000) + "ms");
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        if (BuildConfig.DEBUG) Log.d(LT, "connection " +
                                                connection.id() + " lost posted, state=" +
                                                connection.state());
                                        if (itag.isConnectModeEnabled() && !connection.isConnected()) {
                                            if (BuildConfig.DEBUG)
                                                Log.d(LT, "connection " + connection.id() + " lost");
                                            alertUser(itag, true);
                                        }
                                    }, itag.alertDelay() * 1000L);
                                }
                            } else if (BLEConnectionState.connected.equals(connection.state())) {
                                if (BuildConfig.DEBUG)
                                    Log.d(LT, "connection " + connection.id() + " restored");
                                if((itag.alertMode() == TagAlertMode.alertOnConnect || itag.alertMode() == TagAlertMode.alertOnBoth)){
                                    alertUser(itag, false);
                                }
                            }
                        }
                ));
            }
            disposablesConnections.add(connection.observableClick().subscribe(click -> {
                Log.d("ingo", "Clicks: " + click);
                if (click != 0 && connection.isAlerting()) {
                    Log.d("ingo", "click first case");
                    new Thread(() -> connection.writeImmediateAlert(AlertVolume.NO_ALERT, ITag.BLE_TIMEOUT)).start();
                } else {
                    Log.d("ingo", "click second case");
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
        //itag.setShaking(true);
        ITag.store.setShakingOnConnectDisconnect(itag.id(), true);
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
        // TODO: this should be completely removed since we want to use bluetooth scanner to scan the device and then connect to it
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
                    connection.connect();
                } while (!isInterrupted() && itag.isConnectModeEnabled() && infinity && !connection.isConnected());
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
                            //connectAsync(connection);
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
