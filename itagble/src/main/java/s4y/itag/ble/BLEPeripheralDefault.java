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
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

class BLEPeripheralDefault implements BLEPeripheralInterace {
    private static final String LT = BLEPeripheralDefault.class.getName();
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

    private final Context context;

    private final BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(@NonNull BluetoothGatt gatt, int status, int newState) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onConnectionStateChange id=" + identifier() +
                        " addr=" + gatt.getDevice().getAddress() +
                        " status=" + status +
                        " newState=" + newState);
            }
            if (status == GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    setState(BLEPeripheralState.connected);
                    observables.channelConnected.broadcast(new BLEPeripheralObservablesInterface.ConnectedEvent());
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    close();
                    observables.channelDisconnected.broadcast(new BLEPeripheralObservablesInterface.DisconnectedEvent(status));
                }
            } else {
                close();
                if (getState() == BLEPeripheralState.connecting) {
                    observables.channelConnectionFailed.broadcast(new BLEPeripheralObservablesInterface.ConnectionFailedEvent(status));
                } else {
                    observables.channelDisconnected.broadcast(new BLEPeripheralObservablesInterface.DisconnectedEvent(status));
                }
            }
        }

        @Override
        public void onServicesDiscovered(@NonNull final BluetoothGatt gatt, int status) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onServicesDiscovered id=" + identifier() +
                        " addr=" + gatt.getDevice().getAddress() +
                        " status=" + status);
            }
            List<BLEService> serviceList = new ArrayList<>();
            for (BluetoothGattService service : gatt.getServices()) {
                serviceList.add(new BLEService(service));
            }
            services = new BLEService[serviceList.size()];
            serviceList.toArray(services);
            setState(BLEPeripheralState.discovered);
            observables.channelDiscoveredServices.broadcast(new BLEPeripheralObservablesInterface.DiscoveredServicesEvent(services, status));
        }

        private int rssi = 0;

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (rssi != this.rssi) {
                if (BuildConfig.DEBUG) {
                    Log.d(LT, "onReadRemoteRssi id=" + identifier() + " rssi=" + rssi + " status=" + status);
                }
                this.rssi = rssi;
                observables.channelRSSI.broadcast(new BLEPeripheralObservablesInterface.RSSIEvent(rssi, status));
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(LT, "onReadRemoteRssi skipped id=" + identifier() + " rssi=" + rssi + " status=" + status);
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onCharacteristicWrite id=" + identifier() +
                        " addr=" + gatt.getDevice().getAddress() +
                        " characteristic=" + characteristic.getUuid() +
                        " status=" + status);
            }
            observables.channelWrite.broadcast(new BLEPeripheralObservablesInterface.CharacteristicEvent(
                    new BLECharacteristic(characteristic),
                    status));
        }
    };

    BLEPeripheralDefault(@NonNull Context context, @NonNull BLECentralManagerInterface manager, @NonNull BluetoothDevice device) {
        this.device = device;
        this.manager = manager;
        this.context = context;
    }

    @NonNull
    public String identifier() {
        return device.getAddress();
    }


    @Override
    public void connect() {
        setState(BLEPeripheralState.connecting);
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
    public void disconnect() {
        if (state() == BLEPeripheralState.disconnected) {
            setState(BLEPeripheralState.disconnected);
        } else {
            setState(BLEPeripheralState.disconnecting);
            gatt.disconnect();
        }
    }

    @Override
    public void discoveryServices() {
        if (!BLEPeripheralState.disconnected.equals(state())) {
            setState(BLEPeripheralState.discovering);
            gatt.discoverServices();
        }
    }

    @Override
    public BLEError writeInt8(BLECharacteristic characteristic, int value) {
        BluetoothGatt g;
        synchronized (this) {
            g = gatt;
        }
        if (BuildConfig.DEBUG) {
            Log.d(LT, "writeInt8 id=" + identifier() +
                    " gatt=" + (g==null? "null" :g.getDevice().getAddress()) +
                    " connected=" + manager.connected(device) +
                    " characteristic=" + characteristic.gattCharacteristic.getUuid() +
                    " value=" + value);
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
            if (!BLEPeripheralState.disconnected.equals(state())) { // https://github.com/s4ysolutions/itag/issues/14
                gatt.readRemoteRssi();
            }
            Log.d(LT, "request RSSI id=" + identifier());
            manager.postOperation(this, RSSI_INTERVAL_MS);
        }
    };

    private int rssiCount = 0;

    @Override
    public void enableRSSI() {
        rssiCount++;
        Log.d(LT, "enableRSSI id=" + identifier() + " count=" + rssiCount);
        if (rssiCount == 1) {
            manager.postOperation(mRssiRunable);
        }
    }

    @Override
    public void disableRSSI() {
        if (rssiCount > 0) {
            rssiCount--;
            Log.d(LT, "disableRSSI id=" + identifier() + " count=" + rssiCount);
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
}
