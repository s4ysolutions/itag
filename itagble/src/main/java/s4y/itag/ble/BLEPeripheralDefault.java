package s4y.itag.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

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
    private final Context context;
    @NonNull
    private final BluetoothDevice device;
    @NonNull
    private final BLECentralManagerInterface manager;
    private BluetoothGatt gatt = null;
    private BLEPeripheralObservables observables = new BLEPeripheralObservables();
    private HandlerThread thread = new HandlerThread("BLE Callback");
    private Handler handler = new Handler(thread.getLooper());
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
                    handler.postDelayed(() -> {
                                observables
                                        .channelConnected
                                        .broadcast(new BLEPeripheralObservablesInterface.ConnectedEvent());
                                gatt.discoverServices();
                            },
                            delay);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    close();
                    if (getState() == BLEPeripheralState.connecting) {
                        handler.post(
                                () -> observables.channelConnectionFailed.broadcast(new BLEPeripheralObservablesInterface.ConnectionFailedEvent(status))
                        );
                    } else {
                        handler.post(
                                () -> observables.channelDisconnected.broadcast(new BLEPeripheralObservablesInterface.DisconnectedEvent(status))
                        );
                    }
                    ;
                }
            } else {
                close();
                if (getState() == BLEPeripheralState.connecting) {
                    if (status == 133) {
                        int count = incConnectingCount();
                        if (count <= 3) {
                            gatt.close();
                            handler.postDelayed(() -> connectInternal(),
                                    500);
                        } else {
                            close();
                            handler.post(
                                    () -> observables.channelConnectionFailed.broadcast(new BLEPeripheralObservablesInterface.ConnectionFailedEvent(status))
                            );
                        }
                    } else {
                        close();
                        handler.post(
                                () -> observables.channelConnectionFailed.broadcast(new BLEPeripheralObservablesInterface.ConnectionFailedEvent(status))
                        );
                    }
                } else {
                    close();
                    handler.post(
                            () -> observables.channelDisconnected.broadcast(new BLEPeripheralObservablesInterface.DisconnectedEvent(status))
                    );
                }
            }
        }

        @Override
        public void onServicesDiscovered(@NonNull final BluetoothGatt gatt, int status) {
            handler.post(() -> {
                        List<BLEService> serviceList = new ArrayList<>();
                        for (BluetoothGattService service : gatt.getServices()) {
                            serviceList.add(new BLEService(service));
                        }
                        services = new BLEService[serviceList.size()];
                        serviceList.toArray(services);
                        setState(BLEPeripheralState.connected);
                        observables.channelDiscoveredServices.broadcast(new BLEPeripheralObservablesInterface.DiscoveredServicesEvent(services, status));
                    }
            );
        }
    };

    BLEPeripheralDefault(Context context, BLECentralManagerInterface manager, BluetoothDevice device) {
        this.context = context;
        this.device = device;
        this.manager = manager;
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

    public BLEService[] services() {
        return services;
    }

}
