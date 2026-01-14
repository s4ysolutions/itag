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
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.DEVICE_TYPE_UNKNOWN;
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
    private final BluetoothGatt[] gatt = new BluetoothGatt[]{null};
    private static final int RSSI_INTERVAL_MS = 500;
    private BLEService[] services = new BLEService[]{};

    private BLEPeripheralState state = BLEPeripheralState.disconnected;

    private synchronized void setState(BLEPeripheralState state) {
        this.state = state;
    }

    /*
    private synchronized BLEPeripheralState getState() {
        return this.state;
    }
     */

    private final Context context;
    private final boolean cached;
    private final BLEPeripheralObservables observables = new BLEPeripheralObservables();
    private final RSSIFilter filter = new RSSIFilter();
    
    private final Boolean debug;

    final Handler handler = new Handler(Looper.getMainLooper());
    final int CLOSE_DELAY_MS = 200;
    @NonNull
    private final Runnable mRssiRunable = new Runnable() {
        @Override
        public void run() {
            if (debug) {
                Log.d(LT, "RSSI runnable id=" + identifier() + " isConnected=" + isConnected());
            }
            if (gatt() != null && isConnected()) {
                //Log.v(LT, "request RSSI id=" + identifier());
                gatt().readRemoteRssi();
            }
            manager.postOperation(this, RSSI_INTERVAL_MS);
        }
    };
    private int lastStatus;
    private final Runnable closeSuccessRunnable = this::waitForClose;

    BLEPeripheralDefault(@NonNull Context context, @NonNull BLECentralManagerInterface manager, @NonNull BluetoothDevice device, boolean debug){
        this.device = device;
        this.manager = manager;
        this.context = context;
        this.debug = debug;
        int type = device.getType();
        if (debug) {
            Log.d(LT, "BLEPeripheralDefault addr=" + device.getAddress() + " name=" + device.getName() + " type=" + type);
        }
        this.cached = type != DEVICE_TYPE_UNKNOWN;
    }

    @NonNull
    public String identifier() {
        return device.getAddress();
    }


    @Override
    public void connect() {
        connect(false);
    }

    public void connect(boolean auto) {
        if (debug) {
            Log.d(LT, "connect id=" + identifier());
        }
        if (isConnected()) {
            if (gatt() == null) {
                Log.w(LT, "gatt == null but device is connected");
            } else {
                return;
            }
        }
        setState(BLEPeripheralState.connecting);
        BluetoothGatt g;
        Log.e("ingo", "poveži");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            g = device.connectGatt(context, false, callback, TRANSPORT_LE);
        } else {
            g = device.connectGatt(context, false, callback);
        }
        if (debug) {
            Log.d(LT, "init gatt id=" + identifier() + " gatt is null: " + (g == null ? "yes" : "no") + " id=" + identifier());
        }
        if (g == null) {
            Log.w(LT, "will init gatt to null, id=" + identifier());
        }
        // TODO: it already should be set in onConnectionStateChange but just in case
        if (g != null) {
            setGatt(g);
        }
    }
    private final BluetoothGattCallback callback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(@NonNull BluetoothGatt gatt, int status, int newState) {
            if (debug) {
                Log.d(LT, "onConnectionStateChange id=" + identifier() +
                        " addr=" + gatt.getDevice().getAddress() +
                        " status=" + status +
                        " newState=" + newState);
            }
            lastStatus = status;
            if (status == GATT_SUCCESS) {
                setGatt(gatt);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    setState(BLEPeripheralState.connected);
                    observables.channelConnected.broadcast(new BLEPeripheralObservablesInterface.ConnectedEvent());
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("ingo", "first");
                    waitForClose();
                }
            } else {
                // if status isn't GATT_SUCCESS, it means that connect/disconnect operation failed, possibly due to network problems.
                gatt.disconnect();
                Log.d("ingo", "second");
                waitForClose();
            }
        }

        @Override
        public void onServicesDiscovered(@NonNull final BluetoothGatt gatt, int status) {
            if (debug) {
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
            setState(BLEPeripheralState.services_discovered);
            observables.channelDiscoveredServices.broadcast(new BLEPeripheralObservablesInterface.DiscoveredServicesEvent(services, status));
        }

        private int rssi = 0;

        /*
                private ArrayList<Integer> rssis = new ArrayList<>();
                private ArrayList<Integer> rssiVels = new ArrayList<>();

                private double avg = 0;
                private double avgV = 0;

                private void rssiAvg() {
                    long sum = 0;
                    long sumV = 0;
                    long prev = rssis.get(0);
                    for (int rssi : rssis) {
                        sum += rssi;
                        sumV += (rssi - prev);
                        prev = rssi;
                    }
                    avg = (double) sum / rssis.size();
                    avgV = (double) sumV / rssis.size();
                }

                private double sigma = 0;
                private double sigmaV = 0;

                private void rssiSigma() {
                    rssiAvg();
                    long sum = 0;
                    long sumV = 0;
                    long prev = rssis.get(0);
                    for (int rssi : rssis) {
                        double d = avg - rssi;
                        sum += d * d;
                        double v = (rssi - prev);
                        double dv = avgV - v;
                        sumV += dv * dv;
                        prev = rssi;
                    }
                    sigma = Math.sqrt((double) sum / rssis.size());
                    sigmaV = Math.sqrt((double) sumV / rssis.size());
                };
        */

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssiRaw, int status) {
                /*
                rssis.clear();
                rssiVels.clear();
                 */
            filter.add(rssiRaw);
            /*
            rssis.add(rssiRaw);
            rssiSigma();
            Log.d(LT, "rssi=" + rssiRaw + " avg=" + avg + " sigma = " + sigma + " avgV=" + avgV + " sigmaV=" + sigmaV);
             */

            int rssi = filter.get();

            if (rssi != this.rssi) {
                Log.v(LT, "rssi=" + rssi + " raw=" + rssiRaw);
                if (debug) {
                    Log.v(LT, "onReadRemoteRssi id=" + identifier() + " rssiEnabled=" + rssi + " status=" + status);
                }
                this.rssi = rssi;
                observables.channelRSSI.broadcast(new BLEPeripheralObservablesInterface.RSSIEvent(rssi, status));
            } else {
                if (debug) {
                    Log.v(LT, "onReadRemoteRssi skipped id=" + identifier() + " rssiEnabled=" + rssi + " status=" + status);
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (debug) {
                Log.d(LT, "onCharacteristicWrite id=" + identifier() +
                        " addr=" + gatt.getDevice().getAddress() +
                        " characteristic=" + characteristic.getUuid() +
                        " status=" + status);
            }
            observables.channelWrite.broadcast(new BLEPeripheralObservablesInterface.CharacteristicEvent(
                    new BLECharacteristic(characteristic),
                    status));
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (debug) {
                Log.d(LT, "onCharacteristicChanged id=" + identifier() +
                        " addr=" + gatt.getDevice().getAddress() +
                        " characteristic=" + characteristic.getUuid());
            }
            observables.channelNotification.broadcast(new BLECharacteristic(characteristic));
        }
    };

    @Override
    public void discoveryServices() {
        if (debug) {
            Log.d(LT, "discoveryServices id=" + identifier());
        }
        if (isConnected()) {
            setState(BLEPeripheralState.discovering);
            gatt().discoverServices();
        }
    }

    @Override
    public BLEError writeInt8(BLECharacteristic characteristic, int value) {
        if (debug) {
            Log.d(LT, "writeInt8 id=" + identifier() +
                    " gatt=" + (gatt() == null ? "null" : gatt().getDevice().getAddress()) +
                    " connected=" + manager.connected(device) +
                    " characteristic=" + characteristic.gattCharacteristic.getUuid() +
                    " value=" + value);
        }
        if (gatt() == null) {
            return BLEError.noGatt;
        }
        if (!manager.connected(device)) {
            return BLEError.notConnected;
        }
        setState(BLEPeripheralState.writting);
        characteristic.setInt8(value);
        gatt().writeCharacteristic(characteristic.gattCharacteristic);
        return BLEError.ok;
    }

    @Override
    public BLEError setNotify(BLECharacteristic characteristic, boolean enable) {
        if (gatt() == null) {
            return BLEError.noGatt;
        }
        if (!manager.connected(device)) {
            return BLEError.notConnected;
        }
        gatt().setCharacteristicNotification(characteristic.gattCharacteristic, enable);
        BluetoothGattDescriptor descriptor = characteristic.gattCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt().writeDescriptor(descriptor);
        }

        return BLEError.ok;
    }

    /*
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
    */
    @Override
    public BLEPeripheralObservablesInterface observables() {
        return observables;
    }

    private void waitForClose() {
        if (isConnected()) {
            if (debug) {
                Log.d(LT, "post close device " + device.getAddress());
            }
            handler.postDelayed(closeSuccessRunnable, CLOSE_DELAY_MS);
        } else {
            if (debug) {
                Log.d(LT, "close device " + device.getAddress());
            }
            close();
            observables.channelDisconnected.broadcast(new BLEPeripheralObservablesInterface.DisconnectedEvent(lastStatus));
        }
    }

    @Override
    synchronized public void close() {
        setState(BLEPeripheralState.disconnected);
        if (isConnected()) {
            Log.w(LT, "gatt will be closed, but device is connected");
        }
        if (gatt() == null) {
            Log.w(LT, "gatt will be closed, but  gatt is null");
        }
        if (gatt() != null) {
            Log.d(LT, "close");
            gatt().close();
        }
        if (debug) {
            Log.d(LT, "set gatt null");
        }
        setGatt(null);
    }

    @Override
    public void disconnect() {
        if (BLEPeripheralState.disconnected.equals(state)) {
            if (debug) {
                Log.d(LT, "disconnect id=" + identifier() + ", already disconnected, no action");
            }
        } else {
            BluetoothGatt gatt = gatt();
            boolean connected = isConnected();
            if (connected && gatt != null) {
                if (debug) {
                    Log.d(LT, "disconnect id=" + identifier() + ", connected, will wait connection change");
                }
                setState(BLEPeripheralState.disconnecting);
                gatt.disconnect();
            } else {
                if (debug) {
                    Log.d(LT, "disconnect id=" + identifier() + ", not connected will simulate connection change");
                }
                setState(BLEPeripheralState.disconnecting);
                if (gatt != null)
                    gatt.disconnect(); // just in case ?
                try {
                    Thread.sleep(400); // 5 times per 2 secs
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                close();
                observables.channelDisconnected.broadcast(new BLEPeripheralObservablesInterface.DisconnectedEvent(0));
            }
        }
    }

    private int rssiCount = 0;

    @Override
    public void enableRSSI() {
        /*
        rssiCount++;
        Log.d(LT, "enableRSSI id=" + identifier() + " count=" + rssiCount);
        if (rssiCount == 1) {
            filter.reset();
            manager.postOperation(mRssiRunable);
        }
         */
        Log.d(LT, "enableRSSI id=" + identifier());
        filter.reset();
        rssiCount = 1;
        manager.postOperation(mRssiRunable);
    }

    @Override
    public void disableRSSI() {
        /*
        if (rssiCount > 0) {
            rssiCount--;
            Log.d(LT, "disableRSSI id=" + identifier() + " count=" + rssiCount);
            manager.cancelOperation(mRssiRunable);
        }
         */
        Log.d(LT, "disableRSSI id=" + identifier());
        manager.cancelOperation(mRssiRunable);
        rssiCount = 0;
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

    @Override
    public boolean isConnected() {
        boolean connected = manager.connected(device);
        if (debug) {
            Log.w(LT, "isConnected, device=" + device.getAddress() + " manager.connected=" + connected);
        }
        BluetoothGatt g = gatt();
        if (g == null && connected) {
            Log.w(LT, "isConnected, but gatt == null, id=" + identifier());
        }
        return connected;
    }

    @Override
    public boolean isDisconnected() {
        boolean notconnected = !manager.connected(device);
        if (gatt() == null && !notconnected) {
            Log.w(LT, "not isDisconnected, but gatt == null, id=" + identifier());
        }
        return notconnected;
    }

    public boolean rssiEnabled() {
        return rssiCount > 0;
    }

    private BluetoothGatt gatt() {
        BluetoothGatt ret;
        synchronized (gatt) {
            ret = gatt[0];
        }
        if (debug) {
            Log.v(LT, "getGatt null:" + (ret == null ? "yes" : "no") + " id=" + identifier());
        }
        return ret;
    }

    private void setGatt(BluetoothGatt gatt) {
        synchronized (this.gatt) {
            if (debug) {
                Log.v(LT, "setGatt null:" + (gatt == null ? "yes" : "no") + " id=" + identifier());
            }
            if (gatt != null && this.gatt[0] != null) {
                Log.w(LT, "Overwrite not null gatt with other not null, id=" + identifier());
            }
            this.gatt[0] = gatt;
        }
    }

    @Override
    public boolean cached() {
        return cached;
    }

}
