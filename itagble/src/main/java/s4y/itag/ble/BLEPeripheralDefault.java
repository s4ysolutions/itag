package s4y.itag.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

class BLEPeripheralDefault implements BLEPeripheralInterace, AutoCloseable {
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    @NonNull
    private final BluetoothDevice device;
    @NonNull
    private final BLECentralManagerInterface manager;
    private BluetoothGatt gatt = null;
    private BLEPeripheralObservables observables = new BLEPeripheralObservables();
    private BLEService[] services = new BLEService[]{};

    private BLEPeripheralState state = BLEPeripheralState.disconnected;

    private synchronized void setState(BLEPeripheralState state) {
        this.state = state;
    }

    private synchronized BLEPeripheralState getState() {
        return this.state;
    }

    private int connectingCount;

    private synchronized void resetConnectingCount() {
        this.connectingCount = 0;
    }

    private synchronized int incConnectingCount() {
        this.connectingCount++;
        return this.connectingCount;
    }

    private final Context context;
    private final Handler stateHandler;

    private final BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(@NonNull BluetoothGatt gatt, int status, int newState) {
            if (status == GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    int bondState = device.getBondState();
                    int delayWhenBonded = 0;
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                        delayWhenBonded = 1000;
                    }
                    final int delay = bondState == BOND_BONDED ? delayWhenBonded : 0;
                    stateHandler.postDelayed(() -> {
                                observables
                                        .channelConnected
                                        .broadcast(new BLEPeripheralObservablesInterface.ConnectedEvent());
                                // TODO: dicover in the calling thread
                                //      gatt.discoverServices();
                            },
                            delay);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    close();
                    if (getState() == BLEPeripheralState.connecting) {
                        stateHandler.post(
                                () -> observables.channelConnectionFailed.broadcast(new BLEPeripheralObservablesInterface.ConnectionFailedEvent(status))
                        );
                    } else {
                        stateHandler.post(
                                () -> observables.channelDisconnected.broadcast(new BLEPeripheralObservablesInterface.DisconnectedEvent(status))
                        );
                    }
                }
            } else {
                close();
                if (getState() == BLEPeripheralState.connecting) {
                    stateHandler.post(
                            () -> observables.channelConnectionFailed.broadcast(new BLEPeripheralObservablesInterface.ConnectionFailedEvent(status))
                    );
                    /*
                    if (status == 133) {
                        int count = incConnectingCount();
                        if (count <= 3) {
                            gatt.close();
                            handler.postOperation(() -> connectInternal(),
                                    500);
                        } else {
                            close();
                            handler.postOperation(
                                    () -> observables.channelConnectionFailed.broadcast(new BLEPeripheralObservablesInterface.ConnectionFailedEvent(status))
                            );
                        }
                    } else {
                        close();
                        handler.postOperation(
                                () -> observables.channelConnectionFailed.broadcast(new BLEPeripheralObservablesInterface.ConnectionFailedEvent(status))
                        );
                    }
                     */
                } else {
                    close();
                    stateHandler.post(
                            () -> observables.channelDisconnected.broadcast(new BLEPeripheralObservablesInterface.DisconnectedEvent(status))
                    );
                }
            }
        }

        @Override
        public void onServicesDiscovered(@NonNull final BluetoothGatt gatt, int status) {
            List<BLEService> serviceList = new ArrayList<>();
            for (BluetoothGattService service : gatt.getServices()) {
                serviceList.add(new BLEService(service));
            }
            services = new BLEService[serviceList.size()];
            serviceList.toArray(services);
            setState(BLEPeripheralState.connected);
            stateHandler.post(() -> {
                        observables.channelDiscoveredServices.broadcast(new BLEPeripheralObservablesInterface.DiscoveredServicesEvent(services, status));
                    }
            );
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            stateHandler.post(
                    () -> observables.channelRSSI.broadcast(
                            new BLEPeripheralObservablesInterface.RSSIEvent(rssi, status)
                    )
            );
        }
    };

    BLEPeripheralDefault(@NonNull Context context, @NonNull BLECentralManagerInterface manager, @NonNull BluetoothDevice device) {
        this.device = device;
        this.manager = manager;
        this.context = context;
        this.stateHandler = new Handler(Looper.getMainLooper());
    }

    @NonNull
    public String identifier() {
        return device.getAddress();
    }

    private void connectInternal() {
        BluetoothGatt g;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            g = device.connectGatt(context, true, callback, TRANSPORT_LE);
        } else {
            g = device.connectGatt(context, true, callback);
        }
        synchronized (this) {
            gatt = g;
        }
    }

    @Override
    public void connect() {
        setState(BLEPeripheralState.connecting);
        resetConnectingCount();
        connectInternal();
    }

    @Override
    public void disconnect() {
        if (state() == BLEPeripheralState.disconnected) {
            setState(BLEPeripheralState.disconnected);
        } else {
            setState(BLEPeripheralState.disconnecting);
            gatt.disconnect();
        }
    }

    @Override
    public BLEError writeInt8(BLECharacteristic characteristic, int value) {
        BluetoothGatt g;
        synchronized (this) {
            g = gatt;
        }
        if (g == null) {
            return BLEError.noGatt;
        }
        if (!manager.connected(device)) {
            return BLEError.notConnected;
        }
        setState(BLEPeripheralState.writting);
        characteristic.setInt8(value);
        g.writeCharacteristic(characteristic.gattCharacteristic);
        return BLEError.ok;
    }

    @Override
    public BLEError setNotify(BLECharacteristic characteristic, boolean enable) {
        BluetoothGatt g;
        synchronized (this) {
            g = gatt;
        }
        if (g == null) {
            return BLEError.noGatt;
        }
        if (!manager.connected(device)) {
            return BLEError.notConnected;
        }
        g.setCharacteristicNotification(characteristic.gattCharacteristic, enable);
        BluetoothGattDescriptor descriptor = characteristic.gattCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            g.writeDescriptor(descriptor);
        }

        return BLEError.ok;
    }

    @Override
    public BLEPeripheralState state() {
        BluetoothGatt g;
        synchronized (this) {
            g = gatt;
        }
        if (g == null || !manager.connected(device)) {
            return BLEPeripheralState.disconnected;
        }
        return getState();
    }

    @Override
    public BLEPeripheralObservablesInterface observables() {
        return observables;
    }

    @Override
    synchronized public void close() {
        setState(BLEPeripheralState.disconnected);
        BluetoothGatt g;
        synchronized (this) {
            g = gatt;
            gatt = null;
        }
        g.close();
    }

    private static final int RSSI_INTERVAL_MS = 1000;

    @NonNull
    private Runnable mRssiRunable = new Runnable() {
        @Override
        public void run() {
            if (state() == BLEPeripheralState.connected) { // https://github.com/s4ysolutions/itag/issues/14
                gatt.readRemoteRssi();
            }
            manager.postOperation(this, RSSI_INTERVAL_MS);
        }
    };

    private int rssiCount = 0;

    @Override
    public void enableRSSI() {
        rssiCount++;
        if (rssiCount == 1) {
            manager.postOperation(mRssiRunable);
        }
    }

    @Override
    public void disableRSSI() {
        if (rssiCount > 0) {
            rssiCount--;
            manager.cancelOperation(mRssiRunable);
        }
    }

    public BLEService[] services() {
        return services;
    }

    public String name() {
        return device.getName();
    }

    public String address() {
        return device.getAddress();
    }

    public boolean rssi() {
        return rssiCount > 0;
    }

    ;
}
