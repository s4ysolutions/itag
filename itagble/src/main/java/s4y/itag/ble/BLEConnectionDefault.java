package s4y.itag.ble;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

import s4y.rasat.DisposableBag;
import s4y.rasat.Observable;
import s4y.rasat.android.ChannelDistinct;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

class BLEConnectionDefault implements BLEConnectionInterface {
    @SuppressWarnings("unused")
    private static final String LT = BLEConnectionDefault.class.getName();
    private static final UUID IMMEDIATE_ALERT_SERVICE = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    private static final UUID FINDME_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID ALERT_LEVEL_CHARACTERISTIC = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    private static final UUID FINDME_CHARACTERISTIC = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    @Nullable
    private BLEPeripheralInterace peripheral;
    @NonNull
    final private BLECentralManagerInterface manager;
    @NonNull
    private String id;
    private int lastStatus;
    private final DisposableBag disposables = new DisposableBag();
    private final ChannelDistinct<AlertVolume> alertChannel = new ChannelDistinct<>(AlertVolume.NO_ALERT);
    private final ChannelDistinct<Boolean> findMeChannel = new ChannelDistinct<>(false);
    private final ChannelDistinct<Boolean> lostChannel = new ChannelDistinct<>(true);
    private final ChannelDistinct<BLEConnectionState> stateChannel = new ChannelDistinct<>(BLEConnectionState.disconnected);
    private final ChannelDistinct<Integer> rssiChannel = new ChannelDistinct<>(0);

    private class Click {
        private static final int CLICK_INTERVAL = 600;
        private static final int CLICK_COUNT = 2;

        private Handler clickHandler = new Handler(Looper.getMainLooper());
        private int count = 0;
        private final Runnable waitNext = () -> {
            if (done()) {
                findMeChannel.broadcast(true);
            } else {
                clear();
            }
        };

        private synchronized boolean done() {
            return count >= CLICK_COUNT;
        }

        private synchronized void inc() {
            count++;
        }

        private synchronized void clear() {
            count = 0;
        }

        void onClick() {
            clickHandler.removeCallbacks(click.waitNext);
            if (isFindMe()) {
                findMeChannel.broadcast(false);
            } else {
                click.inc();
                clickHandler.postDelayed(click.waitNext, CLICK_INTERVAL);
            }
        }
    }

    private final Click click = new Click();

    BLEConnectionDefault(@NonNull BLECentralManagerInterface manager,
                         @NonNull String id) {
        this.id = id;
        this.manager = manager;
    }

    BLEConnectionDefault(@NonNull BLECentralManagerInterface manager,
                         @NonNull BLEPeripheralInterace peripheral) {
        this(manager, peripheral.identifier());
        setPeripheral(peripheral);
    }

    private void setPeripheral(@Nullable BLEPeripheralInterace peripheral) {
        this.peripheral = peripheral;
        disposables.dispose();
        // TODO: memory leak on destruct?
        if (this.peripheral != null) {
            disposables.add(this.peripheral
                    .observables()
                    .observableDiscoveredServices()
                    .subscribe(event -> {
                        if (findMeCharacteristic() != null && peripheral != null) {
                            peripheral.setNotify(findMeCharacteristic(), true);
                        }
                        stateChannel.broadcast(BLEConnectionState.connected);
                        lostChannel.broadcast(false);
                    }));
            disposables.add(this.peripheral
                    .observables()
                    .observableDisconnected()
                    .subscribe(event -> {
                        if (event.status != GATT_SUCCESS && lostChannel.observable.value()) {
                            stateChannel.broadcast(BLEConnectionState.disconnected);
                            lostChannel.broadcast(false);
                        }
                    }));
            disposables.add(this.peripheral
                    .observables()
                    .observableNotification()
                    .subscribe(event -> {
                        if (FINDME_CHARACTERISTIC.equals(event.characteristic.uuid())) {
                            click.onClick();
                        }
                    }));
            disposables.add(this.peripheral
                    .observables()
                    .observableWrite()
                    .subscribe(event -> {
                        if (ALERT_LEVEL_CHARACTERISTIC.equals(event.characteristic.uuid())) {
                            alertChannel.broadcast(
                                    AlertVolume.fromCharacteristic(event.characteristic));
                        }
                    }));
            disposables.add(this.peripheral
                    .observables()
                    .observableRSSI()
                    .subscribe(event -> {
                        if (event.status == GATT_SUCCESS) {
                            rssiChannel.broadcast(event.rssi);
                        }
                    }));
        }
    }


    @Override
    public boolean isConnected() {
        return peripheral != null && !BLEPeripheralState.disconnected.equals(peripheral.state());
    }

    private BLEService immediateAlertService() {
        if (peripheral != null) {
            for (BLEService service : peripheral.services()) {
                if (IMMEDIATE_ALERT_SERVICE.equals(service.uuid())) {
                    return service;
                }
            }
        }
        return null;
    }

    private BLEService findMeService() {
        if (peripheral != null) {
            for (BLEService service : peripheral.services()) {
                if (FINDME_SERVICE.equals(service.uuid())) {
                    return service;
                }
            }
        }
        return null;
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
        if (peripheral == null) {
            setPeripheral(manager.retrievePeripheral(id));
        }
    }

    private final ThreadWait<Integer> monitorDisconnect = new ThreadWait<>();
    private final ThreadWait<Integer> monitorDiscovery = new ThreadWait<>();
    private final ThreadWait<Integer> monitorCharacteristicWrite = new ThreadWait<>();
    private final ThreadWait<Integer> monitorConnect = new ThreadWait<>();

    @SuppressWarnings("UnusedReturnValue")
    private BLEError waitForConnect(int timeoutSec) {
        if (peripheral == null)
            return BLEError.noPeripheral;

        if (isConnected()) {
            return BLEError.ok;
        }

        try (DisposableBag disposables = new DisposableBag()) {
            int connectingCount = 0;
            boolean completed = false;
            do {
                disposables.add(
                        peripheral.observables()
                                .observableConnected() // SUCCESS (if status ok)
                                .subscribe((event) -> monitorConnect.setPayload(GATT_SUCCESS))
                );
                disposables.add(
                        peripheral.observables()
                                .observableConnectionFailed() //FAILED during connection
                                .subscribe((event) -> monitorConnect.setPayload(event.status))
                );
                monitorConnect.waitFor(() -> peripheral.connect(), timeoutSec);
                disposables.dispose();
                if (monitorConnect.isTimedOut()) {
                    return BLEError.timeout;
                }
                if (isConnected()) {
                    completed = true;
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
            } while (!completed);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                try {
                    // wait before discovery a bit
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            disposables.add(
                    peripheral.observables()
                            .observableDiscoveredServices()
                            .subscribe(event -> monitorDiscovery.setPayload(event.status)
                            )
            );

            monitorDiscovery.waitFor(() -> peripheral.discoveryServices(), 15);
            if (monitorDiscovery.isTimedOut()) {
                return BLEError.timeout;
            }
            lastStatus = monitorDiscovery.payload();
            return lastStatus == GATT_SUCCESS ? BLEError.ok : BLEError.badStatus;
        }
    }

    private final ThreadWait<BLEPeripheralInterace> monitorScan = new ThreadWait<>();

    @SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
    private BLEError waitForScan(int timeoutSec) {
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
                                        monitorScan.setPayload(event.peripheral);
                                    }
                                }
                            })
            );
            monitorScan.waitFor(manager::scanForPeripherals, timeoutSec);
            if (monitorScan.isTimedOut()) {
                return BLEError.timeout;
            }
            setPeripheral(monitorScan.payload());
            return BLEError.ok;
        }
    }


    @Override
    public BLEError connect(int timeout) {
        stateChannel.broadcast(BLEConnectionState.connecting);
        if (manager.isScanning()) {
            manager.stopScan();
        }
        assertPeripheral();

        // try fast connect to known peripheral
        if (peripheral != null && !isConnected()) {
            waitForConnect(Math.max(15, timeout == 0 ? 15 : timeout));
            if (!isConnected()) {
                // if connection failed will try to re-scan
                setPeripheral(null);
            }
        }

        if (peripheral == null) {
            // scan for not cached/not known peripheral
            // endlessly if timeout = 0
            do {
                BLEError error = waitForScan(25);
                if (peripheral == null && timeout == 0) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    return BLEError.ok.equals(error) ? BLEError.timeout : error;
                }
            } while (peripheral == null);
        }

        // connect as soon as a peripheral scanned
        BLEError error = waitForConnect(timeout);
        if (!BLEError.ok.equals(error)) {
            return error;
        }

        return BLEError.ok;
    }

    @Override
    public BLEError connect() {
        return connect(0);
    }

    @Override
    public BLEError disconnect(int timeoutSec) {
        manager.stopScan();

        if (!isConnected()) {
            return BLEError.ok;
        }

        if (peripheral == null) {
            return BLEError.noPeripheral;
        }

        stateChannel.broadcast(BLEConnectionState.disconnecting);

        if (timeoutSec <= 0) {
            peripheral.disconnect();
            return BLEError.ok;
        }
        try (DisposableBag disposables = new DisposableBag()) {
            disposables.add(
                    peripheral
                            .observables()
                            .observableDisconnected()
                            .subscribe(event -> monitorDisconnect.setPayload(event.status))
            );
            monitorDisconnect.waitFor(() -> peripheral.disconnect(), timeoutSec);
            lastStatus = monitorDisconnect.payload();
            return monitorDisconnect.isTimedOut()
                    ? BLEError.timeout
                    : lastStatus == GATT_SUCCESS
                    ? BLEError.ok
                    : BLEError.badStatus;
        }
    }

    @Override
    public BLEError disconnect() {
        return disconnect(0);
    }

    private BLEError writeInt8(@NonNull BLECharacteristic characteristic, int value, int timeoutSec) {
        if (peripheral == null) {
            return BLEError.noPeripheral;
        }
        if (timeoutSec <= 0) {
            return peripheral.writeInt8(characteristic, value);
        }

        try (DisposableBag disposables = new DisposableBag()) {
            disposables.add(peripheral
                    .observables()
                    .observableWrite()
                    .subscribe(event -> {
                        if (characteristic.uuid().equals(event.characteristic.uuid())) {
                            monitorCharacteristicWrite.setPayload(event.status);
                        }
                    }));
            monitorCharacteristicWrite.waitFor(() -> peripheral.writeInt8(characteristic, value), timeoutSec);
            lastStatus = monitorCharacteristicWrite.payload();
            return monitorCharacteristicWrite.isTimedOut()
                    ? BLEError.timeout
                    : lastStatus == GATT_SUCCESS
                    ? BLEError.ok
                    : BLEError.badStatus;
        }
    }

    @Override
    public BLEError writeImmediateAlert(AlertVolume volume, int timeout) {
        BLECharacteristic characteristic = immediateAlertCharacteristic();
        if (characteristic == null) {
            return BLEError.noImmediateAlertCharacteristic;
        }
        return writeInt8(characteristic, volume.value, timeout);
    }

    @Override
    public BLEError writeImmediateAlert(AlertVolume volume) {
        return writeImmediateAlert(volume, 0);
    }

    @Override
    public void enableRSSI() {
        if (peripheral != null) {
            peripheral.enableRSSI();
        }
    }

    @Override
    public void disableRSSI() {
        if (peripheral != null) {
            peripheral.disableRSSI();
        }
    }

    @Override
    public boolean rssi() {
        return peripheral != null && peripheral.rssi();
    }

    @Override
    public Observable<AlertVolume> observableImmediateAlert() {
        return alertChannel.observable;
    }

    @Override
    public Observable<Boolean> observableFindeMe() {
        return findMeChannel.observable;
    }

    @Override
    public Observable<Integer> observableRSSI() {
        return rssiChannel.observable;
    }

    @Override
    public Observable<BLEConnectionState> observableState() {
        return stateChannel.observable;
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
    public boolean isAlerting() {
        return alertChannel.observable.value() != AlertVolume.NO_ALERT;
    }

    @Override
    public boolean isFindMe() {
        return findMeChannel.observable.value();
    }

    @Override
    public void close() throws Exception {
        if (peripheral != null) {
            peripheral.close();
        }
        disposables.dispose();
    }
}
