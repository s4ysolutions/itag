package s4y.itag.ble;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

import solutions.s4y.rasat.Channel;
import solutions.s4y.rasat.ChannelDistinct;
import solutions.s4y.rasat.DisposableBag;
import solutions.s4y.rasat.Observable;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

class BLEConnectionDefault implements BLEConnectionInterface {
    private static final String LT = BLEConnectionDefault.class.getName();
    private static final UUID IMMEDIATE_ALERT_SERVICE = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    private static final UUID FINDME_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID ALERT_LEVEL_CHARACTERISTIC = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    private static final UUID FINDME_CHARACTERISTIC = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private static final int CLICK_COUNT = 2;

    @NonNull
    private final BLEPeripheralInterace[] peripheral = new BLEPeripheralInterace[]{null};
    @NonNull
    private final BLECentralManagerInterface manager;
    @NonNull
    private final String id;
    private int lastStatus;
    private final DisposableBag disposables = new DisposableBag();
    private final ChannelDistinct<AlertVolume> alertChannel = new ChannelDistinct<>(AlertVolume.NO_ALERT);
    private final Channel<Integer> clickChannel = new Channel<>(0);
    private final ChannelDistinct<BLEConnectionState> stateChannel = new ChannelDistinct<>(BLEConnectionState.disconnected);
    private final ChannelDistinct<Integer> rssiChannel = new ChannelDistinct<>(-999);
    BLEConnectionState oldState = null;

    private final Boolean debug;

    @Override
    public boolean isConnected() {
        synchronized (peripheral) {
            if (debug) {
                if (peripheral[0] == null) {
                    Log.d(LT, "peripheral[0] is null");
                } else {
                    Log.d(LT, "peripheral[0] " + peripheral[0].address() + " isConnected=" + peripheral[0].isConnected());
                }
            }
            return peripheral[0] != null && peripheral[0].isConnected();
        }
    }

    private final ClickHandler clickHandler = new ClickHandler();

    BLEConnectionDefault(@NonNull BLECentralManagerInterface manager,
                         @NonNull String id, Boolean debug) {
        this.id = id;
        this.manager = manager;
        this.debug = debug;
    }

    BLEConnectionDefault(@NonNull BLECentralManagerInterface manager,
                         @NonNull BLEPeripheralInterace peripheral, Boolean debug) {
        this(manager, peripheral.identifier(), debug);
        setPeripheral(peripheral);
    }

    private void startObserve() {
        synchronized (this.peripheral) {
            // TODO: memory leak on destruct?

            if (peripheral[0] != null) {
                disposables.add(peripheral[0]
                        .observables()
                        .observableDiscoveredServices()
                        .subscribe(event -> stateChannel.broadcast(BLEConnectionState.connected)));
                disposables.add(peripheral[0]
                        .observables()
                        .observableDisconnected()
                        .subscribe(event -> stateChannel.broadcast(BLEConnectionState.disconnected)));
                disposables.add(peripheral[0]
                        .observables()
                        .observableNotification()
                        .subscribe(event -> {
                            if (FINDME_CHARACTERISTIC.equals(event.uuid())) {
                                clickHandler.handleClick();
                            }
                        }));
                disposables.add(peripheral[0]
                        .observables()
                        .observableWrite()
                        .subscribe(event -> {
                            stateChannel.broadcast(BLEConnectionState.connected);
                            if (ALERT_LEVEL_CHARACTERISTIC.equals(event.characteristic.uuid())) {
                                alertChannel.broadcast(
                                        AlertVolume.fromCharacteristic(event.characteristic));
                            }
                        }));
                disposables.add(peripheral[0]
                        .observables()
                        .observableRSSI()
                        .subscribe(event -> {
                            if (event.status == GATT_SUCCESS) {
                                rssiChannel.broadcast(event.rssi);
                            }
                        }));
            }
        }
    }

    private void setPeripheral(@Nullable BLEPeripheralInterace peripheral) {
        if (debug) {
            Log.d("LT", "setPeripheral id=" + id + " peripheral=" + (peripheral == null ? "null" : peripheral.identifier()));
            if (peripheral == null) {
                Log.d("LT", "setPeripheral id=" + id + " peripheral=null");
            }
        }
        if (this.peripheral() != null) {
            try {
                this.peripheral().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        synchronized (this.peripheral) {
            this.peripheral[0] = peripheral;
            disposables.dispose();
        }
    }

    @Override
    public void connect(){
        new Thread(() -> {
            connectOnMainThread();
        }).start();
    }

    public BLEError connectOnMainThread() {
        clickChannel.broadcast(0);
        alertChannel.broadcast(AlertVolume.NO_ALERT);

        //manager.stopScan();
        if (isConnected()) {
            if (peripheral() == null) {
                Log.w("LT", "isConnected but peripheral is null");
            }
            return BLEError.ok;
        }
        // stop broadcast peripherial states during connect
        disposables.dispose();
        stateChannel.broadcast(BLEConnectionState.connecting);

        assertPeripheral();

        boolean scan = false;

        while (peripheral() == null || !peripheral().cached()) {
            scan = true;
            // scan for not cached/not known peripheral
            // endlessly if timeout = 0
            if (debug) Log.d(LT,
                    "Attempt to scan for device, peripheral=null:" +
                            (peripheral() == null ? "yes" : "no") + " cached=" +
                            (peripheral() != null && peripheral().cached()));
            BLEError error = waitForScan();
            // waitForScan will set the peripheral if can
            if (peripheral() == null) {
                if (debug) Log.d(LT, "Scan failed no peripheral, will abort");
                return BLEError.ok.equals(error) ? BLEError.noPeripheral : error;
            } else {
                if (debug) Log.d(LT, "Scan got peripheral, will connect");
            }
        }

        // connect as soon as a peripheral scanned
        if (debug) Log.d(LT, "Attempt to connect. Scan run: " + (scan ? "yes" : "no"));
        BLEError error = waitForConnect();
        if (!BLEError.ok.equals(error) || !isConnected()) {
            if (debug) Log.d(LT, "Attempt to connect failed");
            stateChannel.broadcast(BLEConnectionState.disconnected);
            return error;
        }

        if (findMeCharacteristic() != null) {
            peripheral().setNotify(findMeCharacteristic(), true);
        }

        startObserve();
        if (debug) Log.d(LT, "Connected");
        stateChannel.broadcast(BLEConnectionState.connected);
        return BLEError.ok;
// TODO: check for isConencted?
    }

    @Override
    public boolean isDisconnected() {
        synchronized (peripheral) {
            return peripheral[0] == null || peripheral[0].isDisconnected();
        }
    }

    private BLEService immediateAlertService() {
        if (peripheral() != null) {
            for (BLEService service : peripheral().services()) {
                if (IMMEDIATE_ALERT_SERVICE.equals(service.uuid())) {
                    return service;
                }
            }
        }
        return null;
    }

    private BLEService findMeService() {
        synchronized (peripheral) {
            if (peripheral[0] != null) {
                for (BLEService service : peripheral[0].services()) {
                    if (FINDME_SERVICE.equals(service.uuid())) {
                        return service;
                    }
                }
            }
            return null;
        }
    }

    private BLECharacteristic immediateAlertCharacteristic() {
        BLEService service = immediateAlertService();
        if (service != null) {
            for (BLECharacteristic characteristic : service.characteristics) {
                if (ALERT_LEVEL_CHARACTERISTIC.equals(characteristic.uuid())) {
                    return characteristic;
                }
            }
        }
        return null;
    }

    private BLECharacteristic findMeCharacteristic() {
        BLEService service = findMeService();
        if (service != null) {
            for (BLECharacteristic characteristic : service.characteristics) {
                if (FINDME_CHARACTERISTIC.equals(characteristic.uuid())) {
                    return characteristic;
                }
            }
        }
        return null;
    }

    private void assertPeripheral() {
        if (peripheral() == null) {
            setPeripheral(manager.retrievePeripheral(id));
        }
    }

    private final ThreadWait<Integer> monitorDisconnect = new ThreadWait<>();
    private final ThreadWait<Integer> monitorDiscovery = new ThreadWait<>();
    private final ThreadWait<Integer> monitorCharacteristicWrite = new ThreadWait<>();
    private final ThreadWait<Integer> monitorConnect = new ThreadWait<>();

    @SuppressWarnings("UnusedReturnValue")
    private BLEError waitForConnect() {
        if (peripheral() == null)
            return BLEError.noPeripheral;

        if (isConnected()) {
            return BLEError.ok;
        }

        try (DisposableBag disposables = new DisposableBag()) {
            int connectingCount = 0;
            boolean completed = false;
            do {
                disposables.add(
                        peripheral().observables()
                                .observableConnected() // SUCCESS (if status ok)
                                .subscribe((event) -> monitorConnect.setPayload(GATT_SUCCESS)
                                )
                );
                disposables.add(
                        peripheral().observables()
                                .observableConnectionFailed() //FAILED during connection
                                .subscribe((event) -> monitorConnect.setPayload(event.status)
                                )
                );
                disposables.add(
                        peripheral().observables()
                                .observableDisconnected() //disconnected during connection
                                .subscribe((event) -> monitorConnect.setPayload(event.status)
                                )
                );
                if (debug) {
                    Log.d(LT, "Start wait for connect " + Thread.currentThread().getName());
                }
                monitorConnect.waitFor(() -> peripheral().connect(), 0);
                Log.d(LT, "End wait for connect");
                disposables.dispose();
                if (isConnected()) {
                    completed = true;
                } else {
                    if (monitorConnect.isTimedOut()) {
                        peripheral().disconnect();
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        try {
                            peripheral().close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return BLEError.timeout;
                    } else {
                        lastStatus = monitorConnect.payload();
                        if (lastStatus == 133 && connectingCount++ < 3) {
                            try {
                                // wait after close and make another attempt
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        } else {
                            return BLEError.badStatus;
                        }
                    }
                }
            } while (!completed);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                try {
                    // wait before discovery a bit
                    // or it will fail on the older devices
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else {
                try {
                    // wait before discovery a bit
                    // in order to let the gatt to be set in another thread
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            disposables.add(
                    peripheral().observables()
                            .observableDiscoveredServices()
                            .subscribe(event -> monitorDiscovery.setPayload(event.status)
                            )
            );

            monitorDiscovery.waitFor(() -> peripheral().discoveryServices(), 15);
            if (monitorDiscovery.isTimedOut()) {
                if (debug) {
                    Log.d(LT, "waitForConnect timeout id=" + id + " timed out");
                }
                return BLEError.timeout;
            }
            lastStatus = monitorDiscovery.payload();
            if (debug) {
                Log.d(LT, "waitForConnect completed id=" + id + " lastStatus=" + lastStatus);
            }
            return lastStatus == GATT_SUCCESS ? BLEError.ok : BLEError.badStatus;
        }
    }

    private final ThreadWait<BLEPeripheralInterace> monitorScan = new ThreadWait<>();

    @SuppressWarnings({"UnusedReturnValue"})
    private BLEError waitForScan() {
        if (isConnected()) {
            return BLEError.ok;
        }

        try (DisposableBag disposables = new DisposableBag()) {
            disposables.add(
                    manager.observables()
                            .observablePeripheralDiscovered()
                            .subscribe((event) -> {
                                if (event.peripheral != null) {
                                    if (id.equals(event.peripheral.identifier())) {
                                        manager.stopScan(); // TODO: check if this is why the scanner stops unexpectedly
                                        monitorScan.setPayload(event.peripheral);
                                    }
                                }
                            })
            );
            monitorScan.waitFor(manager::startScanForNewDevices, 25);
            manager.stopScan();
            if (monitorScan.isTimedOut()) {
                return BLEError.timeout;
            }
            setPeripheral(monitorScan.payload());
            return BLEError.ok;
        }
    }

    private class ClickHandler {
        private static final int CLICK_INTERVAL = 600;

        private final Handler clickHandler = new Handler(Looper.getMainLooper());
        private int count = 0;
        private final Runnable waitNext = () -> {
            Log.d(LT, "ClickHandler.waitNext postDelayed");
            int c;
            synchronized (this) {
                c = count;
                count = 0;
                if(c == 1) return;
            }
            clickChannel.broadcast(c);
        };

        private synchronized void inc() {
            count++;
        }

        synchronized void handleClick() {
            clickHandler.removeCallbacks(BLEConnectionDefault.this.clickHandler.waitNext);
            inc();
            if(count == 1) clickChannel.broadcast(1);
            Log.d(LT, "ClickHandler.handleClick postDelayed");
            clickHandler.postDelayed(BLEConnectionDefault.this.clickHandler.waitNext, CLICK_INTERVAL);
        }
    }

    @Override
    public BLEError disconnect(int timeoutSec) {
        clickChannel.broadcast(0);
        alertChannel.broadcast(AlertVolume.NO_ALERT);

        if (BLEConnectionState.disconnected.equals(state())) {
            return BLEError.ok;
        }

        if (peripheral() == null) {
            return BLEError.noPeripheral;
        }

        stateChannel.broadcast(BLEConnectionState.disconnecting);
/*
        if (findMeCharacteristic() != null) {
            peripheral.setNotify(findMeCharacteristic(), false);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
*/
        if (debug) {
            Log.d(LT, "disconnect, wait: " + (timeoutSec <= 0 ? "no" : timeoutSec));
        }

        if (timeoutSec <= 0) {
            peripheral().disconnect();
            return BLEError.ok;
        }
        try (DisposableBag disposables = new DisposableBag()) {
            disposables.add(
                    peripheral()
                            .observables()
                            .observableDisconnected()
                            .subscribe(event -> monitorDisconnect.setPayload(event.status))
            );
            monitorDisconnect.waitFor(() -> peripheral().disconnect(), timeoutSec);
            if (monitorDisconnect.isTimedOut()) {
                Log.d("ingo", "ble disconnect timeout");
               return BLEError.timeout;
            }
            lastStatus = monitorDisconnect.payload();
            return  lastStatus == GATT_SUCCESS ? BLEError.ok : BLEError.badStatus;
        }
    }

    @Override
    public BLEError disconnect() {
        return disconnect(0);
    }

    private BLEError writeInt8(@NonNull BLECharacteristic characteristic, int value, int timeoutSec) {
        clickChannel.broadcast(0);

        if (peripheral() == null) {
            return BLEError.noPeripheral;
        }
        if (timeoutSec <= 0) {
            return peripheral().writeInt8(characteristic, value);
        }

        stateChannel.broadcast(BLEConnectionState.writting);
        try (DisposableBag disposables = new DisposableBag()) {
            disposables.add(peripheral()
                    .observables()
                    .observableWrite()
                    .subscribe(event -> {
                        if (characteristic.uuid().equals(event.characteristic.uuid())) {
                            monitorCharacteristicWrite.setPayload(event.status);
                        }
                    }));
            monitorCharacteristicWrite.waitFor(() -> peripheral().writeInt8(characteristic, value), timeoutSec);
            if (monitorCharacteristicWrite.isTimedOut()) {
                return BLEError.timeout;
            }
            lastStatus = monitorCharacteristicWrite.payload();
            return lastStatus == GATT_SUCCESS
                    ? BLEError.ok
                    : BLEError.badStatus;
        }
    }

    @Override
    public BLEError writeImmediateAlert(AlertVolume volume, int timeout) {
        if (volume != AlertVolume.NO_ALERT) {
            alertChannel.broadcast(volume);
        }
        BLECharacteristic characteristic = immediateAlertCharacteristic();
        if (characteristic == null) {
            return BLEError.noImmediateAlertCharacteristic;
        }
        BLEError error = writeInt8(characteristic, volume.value, timeout);
        if (debug) {
            Log.d(LT, "writeImmediateAlert error=" + error);
        }
        return error;
    }

    @Override
    public BLEError writeImmediateAlert(AlertVolume volume) {
        return writeImmediateAlert(volume, 0);
    }

    @Override
    public void enableRSSI() {
        if (peripheral() != null) {
            peripheral().enableRSSI();
        }
    }

    @Override
    public void disableRSSI() {
        if (peripheral() != null) {
            peripheral().disableRSSI();
        }
    }

    @Override
    public boolean rssiEnabled() {
        return peripheral() != null && peripheral().rssiEnabled();
    }

    @Override
    public int rssi() {
        return rssiChannel.observable.value();
    }

    @Override
    public Observable<AlertVolume> observableImmediateAlert() {
        return alertChannel.observable;
    }

    @Override
    public Observable<Integer> observableClick() {
        return clickChannel.observable;
    }

    @Override
    public Observable<Integer> observableRSSI() {
        return rssiChannel.observable;
    }

    @Override
    public void broadcastRSSI(int rssi) {
        rssiChannel.broadcast(rssi);
    }

    @Override
    public Observable<BLEConnectionState> observableState() {
        return stateChannel.observable;
    }

    @NonNull
    @Override
    public String id() {
        return id;
    }

    @Override
    public int getLastStatus() {
        return lastStatus;
    }

    @Override
    public BLEConnectionState state() {
        return stateChannel.observable.value();
    }

    @Override
    public BLEConnectionState oldState() {
        return oldState;
    }

    @Override
    public void setOldState(BLEConnectionState oldState){
        this.oldState = oldState;
    }

    @Override
    public boolean isAlerting() {
        return alertChannel.observable.value() != AlertVolume.NO_ALERT;
    }

    @Override
    public boolean isFindMe() {
        return observableClick().value() >= CLICK_COUNT;
    }

    @Override
    public void resetFindMe() {
       clickChannel.broadcast(0);
    }

    @Override
    public void close() {
        clickChannel.broadcast(0);
        BLEPeripheralInterace p = peripheral();
        if (p != null) {
            p.disconnect();
        }
        disposables.dispose();
    }

    private BLEPeripheralInterace peripheral() {
        BLEPeripheralInterace p;
        synchronized (peripheral) {
            p = this.peripheral[0];
        }
        return p;
    }
}
