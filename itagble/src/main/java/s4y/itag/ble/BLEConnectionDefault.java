package s4y.itag.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import androidx.annotation.NonNull;

import java.util.UUID;

import s4y.rasat.Channel;
import s4y.rasat.Handler;
import s4y.rasat.Disposable;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;

public class BLEConnectionDefault implements BLEConnectionInterface {
    private static final UUID IMMEDIATE_ALERT_SERVICE = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    private static final UUID FINDME_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID ALERT_LEVEL_CHARACTERISTIC = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    private static final UUID FINDME_CHARACTERISTIC = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    @NonNull
    final private BLEConnectionsControlInterface connectionsControl;
    private CBPeripheralInterace peripheral;
    @NonNull
    final private CBCentralManagerInterface manager;
    final private BLEManagerObservablesInterface managerObservables;
    final private BLEPeripheralObservablesInterface peripheralObservables;

    private String id;
    private Disposable<BLEPeripheralObservablesInterface.CharacteristicEvent> disposableImmediateAlert;

    private BluetoothGattCharacteristic characteristicImmediateAlert = null;
    private BluetoothGattCharacteristic characteristicFindMe = null;

    private final Channel<AlertUpdateNotificationEvent> immediateAlertUpdateNotificationChannel;

    @NonNull
    final private Context context;
    @NonNull
    final private BLEFindMeControl findMeControl;

    BLEConnectionDefault(@NonNull Context context,
                         @NonNull BLEConnectionsControlInterface connectionsControl,
                         @NonNull BLEFindMeControl findMeControl,
                         @NonNull CBCentralManagerInterface manager,
                         @NonNull BLEPeripheralObservablesFactoryInterface peripheralObservablesFactory,
                         @NonNull BLEManagerObservablesFactoryInterface managerObservablesFactory,
                         @NonNull String id) {
        this.context = context;
        this.connectionsControl = connectionsControl;
        this.findMeControl = findMeControl;
        this.id = id;
        this.manager = manager;
        this.peripheralObservables = peripheralObservablesFactory.observables();
        this.managerObservables = managerObservablesFactory.observables();
        peripheralObservables.didUpdateValueForCharacteristic().subscribe(new Handler<BLEPeripheralObservablesInterface.CharacteristicEvent>() {
            @Override
            public void handle(BLEPeripheralObservablesInterface.CharacteristicEvent event) {
                if (peripheral.identifier().equals(event.peripheral.identifier()) &&
                        FINDME_CHARACTERISTIC.equals(event.characteristic.getUuid())
                ) {
                    findMeControl.onClick(id);
                }
            }
        });
        immediateAlertUpdateNotificationChannel = new Channel<>(new AlertUpdateNotificationEvent(id, AlertVolume.NO_ALERT));

        disposableImmediateAlert = peripheralObservables
                .didWriteValueForCharacteristic()
                .subscribe(event -> {
                    if (id.equals(event.peripheral.identifier()) &&
                            ALERT_LEVEL_CHARACTERISTIC.equals(event.characteristic.getUuid())) {
                        immediateAlertUpdateNotificationChannel.broadcast(
                                new AlertUpdateNotificationEvent(
                                        id,
                                        AlertVolume.fromCharacteristic(event.characteristic))
                        );
                    }
                });
    }

    BLEConnectionDefault(@NonNull Context context,
                         @NonNull BLEConnectionsControlInterface connectionsControl,
                         @NonNull BLEFindMeControl findMeControl,
                         @NonNull CBCentralManagerInterface manager,
                         @NonNull BLEPeripheralObservablesFactoryInterface peripheralObservablesFactory,
                         @NonNull BLEManagerObservablesFactoryInterface managerObservablesFactory,
                         @NonNull CBPeripheralInterace peripheral) {
        this(context,
                connectionsControl,
                findMeControl,
                manager,
                peripheralObservablesFactory,
                managerObservablesFactory,
                peripheral.identifier().toString()
        );
        setPeripheral(peripheral);
    }

    private void setPeripheral(CBPeripheralInterace peripheral) {
        this.peripheral = peripheral;
        if (disposableImmediateAlert != null) {
            disposableImmediateAlert.dispose();
        }
        if (peripheral == null) {
            disposableImmediateAlert = null;
        } else {
            disposableImmediateAlert = peripheralObservables
                    .didWriteValueForCharacteristic()
                    .subscribe(event -> {
                        if (id.equals(event.peripheral.identifier()) &&
                                ALERT_LEVEL_CHARACTERISTIC.equals(event.characteristic.getUuid())) {
                            immediateAlertUpdateNotificationChannel.broadcast(
                                    new AlertUpdateNotificationEvent(
                                            id,
                                            AlertVolume.fromCharacteristic(event.characteristic))
                            );
                        }
                    });
        }
    }


    @Override
    public boolean isConnected() {
        return peripheral.state() == CBPeripheralState.connected;
    }

    private CBService immediateAlertService() {
        for (CBService service : peripheral.services()) {
            if (IMMEDIATE_ALERT_SERVICE.equals(service.uuid())) {
                return service;
            }
        }
        return null;
    }

    private CBService findMeService() {
        for (CBService service : peripheral.services()) {
            if (FINDME_SERVICE.equals(service.uuid())) {
                return service;
            }
        }
        return null;
    }

    private CBCharacteristic immediateAlertCharacteristic() {
        CBService service = immediateAlertService();
        if (service != null) {
            for (CBCharacteristic characteristic : service.characteristics) {
                if (ALERT_LEVEL_CHARACTERISTIC.equals(characteristic.uuid())) {
                    return characteristic;
                }
            }
        }
        return null;
    }

    private CBCharacteristic findMeCharacteristic() {
        CBService service = findMeService();
        if (service != null) {
            for (CBCharacteristic characteristic : service.characteristics) {
                if (FINDME_CHARACTERISTIC.equals(characteristic.uuid())) {
                    return characteristic;
                }
            }
        }
        return null;
    }

    private BLEError assertPeripheral() {
        return BLEError.ok;
    }

    private void markDisconnected() {
        if (gatt != null) {
            gatt.close();
        }
        gatt = null;
        // observableAlertVolume.broadcast(AlertVolume.NO_ALERT);
        connectionsControl.setState(id, BLEConnectionState.disconnected);
    }

    private final Object lockWaitForDiscover = new Object();
    private boolean lockWaitForDiscoverTimeout;

    private BLEError waitForDiscover(int timeoutSec) {
        // TODO: another scan
        connectionsControl.setState(id, BLEConnectionState.discovering);

        Disposable<BLEManagerObservablesInterface.CBPeripheralDiscoveredEvent> disposable =
                managerObservables.didDiscoverPeripheral().subscribe(event -> {
                    if (peripheral.identifier().equals(event.peripheral.identifier())) {
                        setPeripheral(event.peripheral);
                        lockWaitForDiscoverTimeout = false;
                        synchronized (lockWaitForDiscover) {
                            lockWaitForDiscover.notifyAll();
                        }
                    }
                });

        lockWaitForDiscoverTimeout = true;
        manager.scanForPeripherals();
        synchronized (lockWaitForDiscover) {
            try {
                lockWaitForDiscover.wait(timeoutSec * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        disposable.dispose();

        manager.stopScan();
        return lockWaitForDiscoverTimeout ? BLEError.timeout : BLEError.ok;
    }

    private final ThreadWait<CBPeripheralDefault.ConnectionStateChange> monitorConnect = new ThreadWait<>();
    private final ThreadWait<CBPeripheralDefault.ConnectionStateChange> monitorDisconnect = new ThreadWait<>();
    private final ThreadWait<CBPeripheralDefault.ServicesDiscovered> monitorServicesDiscovered = new ThreadWait<>();
    private final ThreadWait<CBPeripheralDefault.CharacteristicWrite> monitorCharacteristicWrite = new ThreadWait<>();

    private BLEError waitForConnect(int timeoutSec) throws BLEException {
        if (peripheral == null)
            return BLEError.noPeripheral;

        if (peripheral.state() == CBPeripheralState.connected) {
            return completeConnection(60);
        }
        connectionsControl.setState(id, BLEConnectionState.connecting);

        try (Disposable<CBPeripheralInterace> disposable = managerObservables.didConnectPeripheral().subscribe(
                new Handler<CBPeripheralInterace>() {
                    @Override
                    public void handle(CBPeripheralInterace event) {

                    }
                }
        )){
int a;
a=1;
        }
        monitorConnect.waitFor(
                () -> gatt = peripheral.connectGatt(context, true, gattCallback),
                timeoutSec);
        if (monitorConnect.isTimedOut()) {
            return BLEError.timeout;
        }

        // Handle error during connection
        int count = 0;
        while (monitorConnect.payload().status == 133 && count < 5) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (gatt != null) {
                gatt.close();
                gatt = null;
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            monitorConnect.waitFor(
                    () -> gatt = peripheral.connectGatt(context, true, gattCallback),
                    timeoutSec);
            if (monitorConnect.isTimedOut()) {
                return BLEError.timeout;
            }
            count++;
        }

        if (monitorConnect.payload().status != GATT_SUCCESS || monitorConnect.payload().newState != STATE_CONNECTED) {
            throw new BLEException(monitorConnect.payload().status);
        }
        return BLEError.ok;
    }

    private BLEError waitForConnect() throws BLEException {
        return waitForConnect(0);
    }

    @SuppressWarnings("UnusedReturnValue")
    private BLEError waitForDiscoverCharacteristics(int timeoutSec) {
        if (peripheral == null) {
            return BLEError.noPeripheral;
        }
        if (gatt == null) {
            return BLEError.noGatt;
        }
        connectionsControl.setState(id, BLEConnectionState.discoveringServices);

        monitorServicesDiscovered.waitFor(
                () -> gatt.discoverServices(),
                timeoutSec
        );

        if (monitorServicesDiscovered.isTimedOut()) {
            return BLEError.timeout;
        }

        if (monitorServicesDiscovered.payload().status != GATT_SUCCESS) {
            lastStatus = monitorServicesDiscovered.payload().status;
            return BLEError.badStatus;
        }

        for (BluetoothGattService service : gatt.getServices()) {
            if (IMMEDIATE_ALERT_SERVICE.equals(service.getUuid())) {
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    if (ALERT_LEVEL_CHARACTERISTIC.equals(characteristic.getUuid())) {
                        characteristicImmediateAlert = characteristic;
                    }
                }
            } else if (FINDME_SERVICE.equals(service.getUuid())) {
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    if (FINDME_CHARACTERISTIC.equals(characteristic.getUuid())) {
                        characteristicFindMe = characteristic;
                    }
                }
            }
        }
        return BLEError.ok;
    }

    private BLEError write(BluetoothGattCharacteristic characteristic, int value, int timeoutSec) {
        if (peripheral == null) {
            return BLEError.noPeripheral;
        }
        if (gatt == null) {
            return BLEError.noGatt;
        }

        connectionsControl.setState(id, BLEConnectionState.writting);
        characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        if (timeoutSec <= 0) {
            gatt.writeCharacteristic(characteristic);
            return BLEError.ok;
        }

        characteristicToWrite = characteristic;
        monitorCharacteristicWrite.waitFor(() -> gatt.writeCharacteristic(characteristic), timeoutSec);

        return monitorCharacteristicWrite.isTimedOut() ? BLEError.timeout : BLEError.ok;
    }

    private void setCharacteristicNotification(BluetoothGatt bluetoothgatt, @NonNull BluetoothGattCharacteristic bluetoothgattcharacteristic) {
        bluetoothgatt.setCharacteristicNotification(bluetoothgattcharacteristic, true);
        BluetoothGattDescriptor descriptor = bluetoothgattcharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothgatt.writeDescriptor(descriptor);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private BLEError completeConnection(int timeoutSec) {

        if (peripheral == null) {
            connectionsControl.setState(id, BLEConnectionState.disconnected);
            return BLEError.noPeripheral;
        }

        if (gatt == null) {
            connectionsControl.setState(id, BLEConnectionState.disconnected);
            return BLEError.noGatt;
        }

        if (characteristicImmediateAlert == null) {
            waitForDiscoverCharacteristics(timeoutSec);
            /*
            BLEError error = waitForDiscoverCharacteristics(timeoutSec);
            if (error != BLEError.ok) {
                gatt.disconnect();
                return error;
            }*/
        }

            /*
        if (characteristicImmediateAlert == null) {
            gatt.disconnect();
            return BLEError.noImmediateAlertCharacteristic;
        }
             */

        if (characteristicFindMe == null) {
            waitForDiscoverCharacteristics(timeoutSec);
            /*
            BLEError error = waitForDiscoverCharacteristics(timeoutSec);
            if (error != BLEError.ok) {
                gatt.disconnect();
                return error;
            }
             */
        }

            /*
        if (characteristicFindMe == null) {
            gatt.disconnect();
            return BLEError.noFindMeAlertCharacteristic;
        }
             */

        if (characteristicFindMe != null) {
            setCharacteristicNotification(gatt, characteristicFindMe);
        }

        connectionsControl.setState(id, BLEConnectionState.connected);
        return BLEError.ok;
    }

    @Override
    public BLEError connect() throws InterruptedException, BLEException {
        connectionsControl.setState(id, BLEConnectionState.connecting);
        manager.stopScan();
        assertPeripheral();

        if (peripheral == null) {
            Disposable<BLEDiscoveryResult> disposable =
                    manager.observableDidDiscoverPeripheral().subscribe(bleDiscoveryResult -> {
                        if (id.equals(bleDiscoveryResult.peripheral.getAddress())) {
                            peripheral = bleDiscoveryResult.peripheral;
                            synchronized (lockWaitForDiscover) {
                                lockWaitForDiscover.notifyAll();
                            }
                        }
                    });
            try {
                manager.scanForPeripherals();
                synchronized (lockWaitForDiscover) {
                    lockWaitForDiscover.wait();
                }
            } finally {
                disposable.dispose();
            }
        }
        if (peripheral == null) {
            connectionsControl.setState(id, BLEConnectionState.disconnected);
            return BLEError.noPeripheral;
        }

        return waitForConnect();
    }

    @Override
    public BLEError connect(int timeout) throws BLEException {
        final boolean connected = isConnected();
        connectionsControl.setState(id, connected ? BLEConnectionState.connected : BLEConnectionState.connecting);

        manager.stopScan();
        assertPeripheral();

        if (waitForConnect(timeout) != BLEError.ok) {
            peripheral = null;
        }

        if (peripheral == null) {
            BLEError error = waitForDiscover(timeout);
            if (error != BLEError.ok) {
                connectionsControl.setState(id, BLEConnectionState.disconnected);
                return error;
            }
        }
        if (peripheral == null) {
            connectionsControl.setState(id, BLEConnectionState.disconnected);
            return BLEError.noPeripheral;
        }

        return waitForConnect(timeout);
    }

    @Override
    public BLEError disconnect(int timeoutSec) throws BLEException {
        manager.stopScan();
        connectionsControl.setState(id, BLEConnectionState.disconnecting);

        BLEError error = assertPeripheral();
        if (error != BLEError.ok) {
            return error;
        }

        if (peripheral == null) {
            return BLEError.noPeripheral;
        }

        if (gatt == null) {
            return BLEError.noGatt;
        }

        if (timeoutSec <= 0) {
            if (characteristicFindMe != null) {
                setCharacteristicNotification(gatt, characteristicFindMe);
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            gatt.disconnect();
            return BLEError.ok;
        }

        if (characteristicFindMe != null) {
            setCharacteristicNotification(gatt, characteristicFindMe);
        }
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        monitorDisconnect.waitFor(() -> gatt.disconnect(), timeoutSec);

        if (monitorDisconnect.payload().status != GATT_SUCCESS) {
            throw new BLEException(monitorDisconnect.payload().status);
        }
        return monitorDisconnect.isTimedOut() ? BLEError.timeout : BLEError.ok;
    }

    @Override
    public BLEError writeImmediateAlert(AlertVolume volume, int timeout) {
        if (characteristicImmediateAlert == null) {
            return BLEError.noImmediateAlertCharacteristic;
        }
        /*
        alertVolume = volume;
         */
        return write(characteristicImmediateAlert, volume.value, timeout);
    }

    @Override
    public BLEError writeImmediateAlert(AlertVolume volume) {
        return writeImmediateAlert(volume, 0);
    }

    /*
        @Override
        public Channel<AlertVolume> observableImmediateAlert() {
            return observableAlertVolume;
        }
    */
    @Override
    public int getLastStatus() {
        return lastStatus;
    }

    Channel<AlertUpdateNotificationEvent> immediateAlertUpdateNotificationChannel(){
        return immediateAlertUpdateNotificationChannel;
    };
}
