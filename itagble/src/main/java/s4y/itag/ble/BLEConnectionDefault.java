package s4y.itag.ble;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

import s4y.rasat.Channel;
import s4y.rasat.DisposableBag;
import s4y.rasat.Observable;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

class BLEConnectionDefault implements BLEConnectionInterface {
    private static final UUID IMMEDIATE_ALERT_SERVICE = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    private static final UUID FINDME_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID ALERT_LEVEL_CHARACTERISTIC = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    private static final UUID FINDME_CHARACTERISTIC = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    @NonNull
    final private BLEConnectionsControlInterface connectionsControl;
    private BLEPeripheralInterace peripheral;
    @NonNull
    final private BLECentralManagerInterface manager;
    @NonNull
    private String id;
    private int lastStatus;
    private final DisposableBag disposables = new DisposableBag();
    private final Channel<AlertVolume> immediateAlertUpdateNotificationChannel = new Channel<>(AlertVolume.NO_ALERT);
    private final Channel<Boolean> findMeChannel = new Channel<>(false);
    private final Channel<Boolean> lostChannel = new Channel<>(true);
    @NonNull
    final private BLEFindMeControlInterface findMeControl;

    BLEConnectionDefault(@NonNull BLEConnectionsControlInterface connectionsControl,
                         @NonNull BLEFindMeControlInterface findMeControl,
                         @NonNull BLECentralManagerInterface manager,
                         @NonNull String id) {
        this.connectionsControl = connectionsControl;
        this.findMeControl = findMeControl;
        this.id = id;
        this.manager = manager;
    }

    BLEConnectionDefault(@NonNull BLEConnectionsControlInterface connectionsControl,
                         @NonNull BLEFindMeControlInterface findMeControl,
                         @NonNull BLECentralManagerInterface manager,
                         @NonNull BLEPeripheralInterace peripheral) {
        this(connectionsControl,
                findMeControl,
                manager,
                peripheral.identifier()
        );
        setPeripheral(peripheral);
    }

    private void setPeripheral(@Nullable BLEPeripheralInterace peripheral) {
        this.peripheral = peripheral;
        disposables.dispose();
        if (this.peripheral != null) {
            disposables.add(this.peripheral
                    .observables()
                    .observableNotification()
                    .subscribe(event -> {
                        if (FINDME_CHARACTERISTIC.equals(event.characteristic.uuid())) {
                            findMeControl.onClick(id);
                        }
                    }));
            disposables.add(this.peripheral
                    .observables()
                    .observableWrite()
                    .subscribe(event -> {
                        if (ALERT_LEVEL_CHARACTERISTIC.equals(event.characteristic.uuid())) {
                            immediateAlertUpdateNotificationChannel.broadcast(
                                    AlertVolume.fromCharacteristic(event.characteristic));
                        }
                    }));
            disposables.add(this.peripheral
                    .observables()
                    .observableConnected()
                    .subscribe(event -> {
                        if (findMeCharacteristic() != null && peripheral != null) {
                           peripheral.setNotify(findMeCharacteristic(), true);
                        }
                        if (!lostChannel.observable.value()) {
                            lostChannel.broadcast(true);
                        }
                    }));
            disposables.add(this.peripheral
                    .observables()
                    .observableDisconnected()
                    .subscribe(event -> {
                        if (event.status != GATT_SUCCESS && lostChannel.observable.value()) {
                            lostChannel.broadcast(false);
                        }
                    }));
        }
    }


    @Override
    public boolean isConnected() {
        return peripheral != null && peripheral.state() == BLEPeripheralState.connected;
    }

    private BLEService immediateAlertService() {
        for (BLEService service : peripheral.services()) {
            if (IMMEDIATE_ALERT_SERVICE.equals(service.uuid())) {
                return service;
            }
        }
        return null;
    }

    private BLEService findMeService() {
        for (BLEService service : peripheral.services()) {
            if (FINDME_SERVICE.equals(service.uuid())) {
                return service;
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
            peripheral = manager.retrievePeripheral(id);
        }
    }

    private final ThreadWait<Integer> monitorDisconnect = new ThreadWait<>();
    private final ThreadWait<BLEPeripheralInterace> monitorScan = new ThreadWait<>();
    private final ThreadWait<Integer> monitorCharacteristicWrite = new ThreadWait<>();

    private final ThreadWait<Integer> monitorConnect = new ThreadWait<>();

    private BLEError waitForConnect(int timeoutSec) {
        if (peripheral == null)
            return BLEError.noPeripheral;

        if (isConnected()) {
            return BLEError.ok;
        }

        try (DisposableBag disposables = new DisposableBag()) {
            disposables.add(
                    peripheral.observables()
                            .observableDiscoveredServices() // SUCCESS (if status ok)
                            .subscribe((event) -> monitorConnect.setPayload(event.status))
            );
            disposables.add(
                    peripheral.observables()
                            .observableConnectionFailed() //FAILED during connection
                            .subscribe((event) -> monitorConnect.setPayload(event.status))
            );
            monitorConnect.waitFor(() -> peripheral.connect(), timeoutSec);
            if (monitorConnect.isTimedOut()) {
                return BLEError.timeout;
            }
            if (isConnected()) {
                lastStatus = GATT_SUCCESS;
                return BLEError.ok;
            } else {
                lastStatus = monitorConnect.payload();
                return BLEError.badStatus;
            }
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private BLEError waitForScan(int timeoutSec) {
        if (isConnected()) {
            return BLEError.ok;
        }

        try (DisposableBag disposables = new DisposableBag()) {
            disposables.add(
                    manager.observables()
                            .observablePeripheralDiscovered()
                            .subscribe((event) -> {
                                if (peripheral == null) {
                                    monitorScan.setPayload(null);
                                }
                                if (id.equals(event.device.identifier())) {
                                    monitorScan.setPayload(event.device);
                                }
                            })
            );
            monitorScan.waitFor(manager::scanForPeripherals, timeoutSec);
            if (monitorConnect.isTimedOut()) {
                return BLEError.timeout;
            }
            peripheral = monitorScan.payload();
            return BLEError.ok;
        }
    }

    @Override
    public BLEError connect(int timeout) throws InterruptedException {
        connectionsControl.setState(id, BLEConnectionState.connecting);
        manager.stopScan();
        assertPeripheral();

        if (peripheral != null) {
            // already connected
            if (isConnected()) {
                connectionsControl.setState(id, BLEConnectionState.connecting);
                return BLEError.ok;
            }
            // try fast connect to known device
            waitForConnect(Math.max(15, timeout));
            if (isConnected()) {
                connectionsControl.setState(id, BLEConnectionState.connecting);
                return BLEError.ok;
            }
        }

        do {
            // scan for not cached/not known device
            waitForScan(timeout == 0 ? 25 : 15);
            if (peripheral == null) {
                Thread.sleep(5000);
            }
        } while (peripheral == null && timeout == 0);

        // failed to scan for the device
        if (peripheral == null) {
            connectionsControl.setState(id, BLEConnectionState.disconnected);
            return BLEError.noPeripheral;
        }

        // connect as soon as a device scanned
        BLEError error = waitForConnect(timeout);

        connectionsControl.setState(id,
                isConnected()
                        ? BLEConnectionState.connected
                        : BLEConnectionState.disconnected);
        return error;
    }

    @Override
    public BLEError connect() throws InterruptedException {
        return connect(0);
    }

    @Override
    public BLEError disconnect(int timeoutSec) {
        manager.stopScan();
        if (!isConnected()) {
            connectionsControl.setState(id, BLEConnectionState.disconnected);
            return BLEError.ok;
        }

        connectionsControl.setState(id, BLEConnectionState.disconnecting);


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
            connectionsControl.setState(id, isConnected() ? BLEConnectionState.connected : BLEConnectionState.disconnected);
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
    public Observable<AlertVolume> observableImmediateAlert() {
        return immediateAlertUpdateNotificationChannel.observable;
    }

    @Override
    public Observable<Boolean> observableFindeMe() {
        return findMeChannel.observable;
    }

    @Override
    public Observable<Boolean> observableLost() {
        return lostChannel.observable;
    }

    @Override
    public int getLastStatus() {
        return lastStatus;
    }
}
