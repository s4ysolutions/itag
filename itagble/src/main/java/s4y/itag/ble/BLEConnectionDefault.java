package s4y.itag.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.annotation.NonNull;

import java.util.UUID;

import s4y.observables.Subscription;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;

public class BLEConnectionDefault implements BLEConnectionInterface {
    private static final UUID IMMEDIATE_ALERT_SERVICE = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    private static final UUID FINDME_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID ALERT_LEVEL_CHARACTERISTIC = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    private static final UUID FINDME_CHARACTERISTIC = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    final private Context context;
    final private BLEConnectionsControlInterface connectionsControl;
    final private BLEFindMeControl findMeControl;
    final private CBCentralManagerInterface manager;
    final private String id;

    private BluetoothDevice peripheral = null;
    private BluetoothGatt gatt = null;
    private BluetoothGattCharacteristic characteristicImmediateAlert = null;
    private BluetoothGattCharacteristic characteristicFindMe = null;

    private int lastStatus = GATT_SUCCESS;
/*
    private AlertVolume alertVolume;
    private final Observable<AlertVolume> observableAlertVolume = new Observable<>(AlertVolume.NO_ALERT);
*/
    private static class ConnectionStateChange {
        final BluetoothGatt gatt;
        final int status;
        final int newState;

        ConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            this.gatt = gatt;
            this.status = status;
            this.newState = newState;
        }
    }

    private static class ServicesDiscovered {
        final BluetoothGatt gatt;
        final int status;

        ServicesDiscovered(BluetoothGatt gatt, int status) {
            this.gatt = gatt;
            this.status = status;
        }
    }

    private static class CharacteristicWrite {
        final BluetoothGatt gatt;
        final BluetoothGattCharacteristic characteristic;
        final int status;

        CharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            this.gatt = gatt;
            this.characteristic = characteristic;
            this.status = status;
        }
    }

    private static class Monitor<T> {
        private T state;

        synchronized void setPayload(T state) {
            this.state = state;
            notifyAll();
        }

        synchronized void waitFor(Runnable runnable, long timeoutSec) {
            state = null;
            runnable.run();
            try {
                if (timeoutSec > 0) {
                    wait(timeoutSec * 1000);
                } else {
                    wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        boolean isTimedOut() {
            return state == null;
        }

        T payload() {
            return state;
        }
    }

    private final Monitor<ConnectionStateChange> monitorConnect = new Monitor<>();
    private final Monitor<ConnectionStateChange> monitorDisconnect = new Monitor<>();
    private final Monitor<ServicesDiscovered> monitorServicesDiscovered = new Monitor<>();
    private final Monitor<CharacteristicWrite> monitorCharacteristicWrite = new Monitor<>();

    private BluetoothGattCharacteristic characteristicToWrite = null;

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            monitorConnect.setPayload(new ConnectionStateChange(gatt, status, newState));
            if (status == GATT_SUCCESS) {
                if (newState == STATE_CONNECTED) {
                    completeConnection(60);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    markDisconnected();
                }
            } else if (status != 133) {
                markDisconnected();
            }
        }

        @Override
        public void onServicesDiscovered(@NonNull final BluetoothGatt gatt, int status) {
            if (id.equals(gatt.getDevice().getAddress())) {
                monitorServicesDiscovered.setPayload(new ServicesDiscovered(gatt, status));
            }
        }

        @Override
        public void onCharacteristicWrite(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, int status) {
            if (id.equals(gatt.getDevice().getAddress())) {
                connectionsControl.setState(id, BLEConnectionState.connected);
                // TODO: parallel writes?
                /*
                if (alertVolume!=null && ALERT_LEVEL_CHARACTERISTIC.equals(characteristic.getUuid())) {
                    observableAlertVolume.onNext(alertVolume);
                }
                 */
                if (characteristicToWrite == null || characteristicToWrite.getUuid().equals(characteristic.getUuid())) {
                    monitorCharacteristicWrite.setPayload(new CharacteristicWrite(gatt, characteristic, status));
                }
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
            if (id.equals(gatt.getDevice().getAddress()) && FINDME_CHARACTERISTIC.equals(characteristic.getUuid())) {
                findMeControl.onClick(id);
            }
        }
    };

    BLEConnectionDefault(Context context,
                         BLEConnectionsControlInterface connectionsControl,
                         BLEFindMeControl findMeControl,
                         CBCentralManagerInterface manager,
                         String id) {
        this.context = context;
        this.connectionsControl = connectionsControl;
        this.findMeControl = findMeControl;
        this.id = id;
        this.manager = manager;
    }

    private BLEError assertPeripheral() {
        if (peripheral == null) {
            peripheral = manager.retrievePeripheral(id);
        }
        return BLEError.ok;
    }

    private void markDisconnected() {
        if (gatt != null) {
            gatt.close();
        }
        gatt = null;
        // observableAlertVolume.onNext(AlertVolume.NO_ALERT);
        connectionsControl.setState(id, BLEConnectionState.disconnected);
    }

    private final Object lockWaitForDiscover = new Object();
    private boolean lockWaitForDiscoverTimeout;

    private BLEError waitForDiscover(int timeoutSec) {
        // TODO: another scan
        connectionsControl.setState(id, BLEConnectionState.discovering);

        Subscription<BLEDiscoveryResult> subscription =
                manager.observableDidDiscoverPeripheral().subscribe(bleDiscoveryResult -> {
                    if (id.equals(bleDiscoveryResult.peripheral.getAddress())) {
                        peripheral = bleDiscoveryResult.peripheral;
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
        subscription.dispose();

        manager.stopScan();
        return lockWaitForDiscoverTimeout ? BLEError.timeout : BLEError.ok;
    }


    private BLEError waitForConnect(int timeoutSec) throws BLEException {
        if (peripheral == null)
            return BLEError.noPeripheral;

        if (isConnected()) {
            return completeConnection(60);
        }

        connectionsControl.setState(id, BLEConnectionState.connecting);

        monitorConnect.waitFor(
                () -> gatt = peripheral.connectGatt(context, true, gattCallback),
                timeoutSec);
        if (monitorConnect.isTimedOut()){
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
            if (monitorConnect.isTimedOut()){
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

        if (characteristicFindMe != null ) {
            setCharacteristicNotification(gatt, characteristicFindMe);
        }

        connectionsControl.setState(id, BLEConnectionState.connected);
        return BLEError.ok;
    }

    @Override
    public boolean isConnected() {
        return peripheral != null & gatt != null && gatt.getConnectionState(peripheral) == STATE_CONNECTED;
    }

    @Override
    public BLEError connect() throws InterruptedException, BLEException {
        connectionsControl.setState(id, BLEConnectionState.connecting);
        manager.stopScan();
        assertPeripheral();

        if (peripheral == null) {
            Subscription<BLEDiscoveryResult> subscription =
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
                subscription.dispose();
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
    public Observable<AlertVolume> observableImmediateAlert() {
        return observableAlertVolume;
    }
*/
    @Override
    public int getLastStatus() {
        return lastStatus;
    }
}
