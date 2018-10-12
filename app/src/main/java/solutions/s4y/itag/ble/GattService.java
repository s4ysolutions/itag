package solutions.s4y.itag.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import io.reactivex.subjects.PublishSubject;
import solutions.s4y.itag.BuildConfig;
import solutions.s4y.itag.ITagApplication;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

public class GattService extends Service {
    private static final String T = GattService.class.getName();

    public static class RssiData {
        public String addr;
        public int rssi;

        public RssiData(BluetoothGatt gatt, int rssi) {
            this.addr = gatt.getDevice().getAddress();
            this.rssi = rssi;
        }
    }

    public static final PublishSubject<RssiData> subjectRssi = PublishSubject.create();

    public static final UUID IMMEDIATE_ALERT_SERVICE =
            UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    public static final UUID FIND_ME_SERVICE =
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    public static final UUID LINK_LOSS_SERVICE =
            UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_SERVICE =
            UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    public static final UUID GENERIC_SERVICE =
            UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID ALERT_LEVEL_CHARACTERISTIC =
            UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    public static final UUID FIND_ME_CHARACTERISTIC =
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private HashMap<String, BluetoothGatt> mGatts = new HashMap<>(4);
    private HashMap<String, Runnable> mRssiListeners = new HashMap<>(4);

    // Debug only
    private Set<String> mConnectedAddr = new HashSet<>(4);

    private Handler mHandler = new Handler();

    public class GattBinder extends Binder {
        public GattService getService() {
            return GattService.this;
        }
    }

    private IBinder mBinder = new GattBinder();

    public GattService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private class GattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (BuildConfig.DEBUG) {
                Log.d(T,
                        "GattCallback.onConnectionStateChange: addr="+gatt.getDevice().getAddress()+
                                " status="+status+
                                " state="+newState);
            }
            if (status == GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (BuildConfig.DEBUG) {
                        Log.d(T,
                                "GattCallback.onConnectionStateChange, STATE_CONNECTED");
                        Log.d(T,
                                "gatt.discoverServices");
                    }
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (BuildConfig.DEBUG) {
                        Log.d(T,
                                "GattCallback.onConnectionStateChange, STATE_DISCONNECTED");
                    }
                    gatt.close();
                }
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            if (BuildConfig.DEBUG) {
                Log.d(T,
                        "GattCallback.onServicesDiscovered: addr="+gatt.getDevice().getAddress()+
                                " status="+status+" , will startListenRssi");
            }

//            startListenRssi(gatt.getDevice().getAddress());
            if (BluetoothGatt.GATT_SUCCESS == status) {
                for (BluetoothGattService service : gatt.getServices()) {
                    if (IMMEDIATE_ALERT_SERVICE.equals(service.getUuid())) {
                    } else if (BATTERY_SERVICE.equals(service.getUuid())) {
                    } else if (FIND_ME_SERVICE.equals(service.getUuid())) {
                    } else if (LINK_LOSS_SERVICE.equals(service.getUuid())) {
                    } else if (GENERIC_SERVICE.equals(service.getUuid())) {
                    } else {
                        if (BuildConfig.DEBUG) {
                            ITagApplication.errorNotifier.onNext(new Exception("Unknown service: " + service.getUuid().toString()));
                        }
                    }
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (BuildConfig.DEBUG) {
                Log.d(T,
                        "GattCallback.onReadRemoteRssi: addr="+gatt.getDevice().getAddress()+
                                " status="+status+" , rssi="+rssi);
            }

            if (BluetoothGatt.GATT_SUCCESS == status) {
                if (BuildConfig.DEBUG) {
                    Log.d(T,
                            "GattCallback.onReadRemoteRssi: addr="+gatt.getDevice().getAddress()+
                                    " status=GATT_SUCCESS");
                }
                RssiData rssiData = new RssiData(gatt, rssi);
                subjectRssi.onNext(rssiData);
            }
        }
    }

    public void connect(String address, boolean autoConnect) {
        if (BuildConfig.DEBUG) {
            Log.d(T,
                    "connect: addr="+address);
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            ITagApplication.errorNotifier.onNext(new Exception("No adapter"));
            return;
        }
        if (mGatts.containsKey(address)) {
            if (BuildConfig.DEBUG) {
                Log.d(T,
                        "gatt.discoverServices: addr="+address);
            }
            Objects.requireNonNull(mGatts.get(address)).discoverServices();
        } else {
            if (BuildConfig.DEBUG) {
                if (mConnectedAddr.contains(address)) {
                    ITagApplication.errorNotifier.onNext(new Exception("Duplicate add connect"));
                }
                mConnectedAddr.add(address);
            }

            BluetoothDevice device = adapter.getRemoteDevice(address);
            // it will call discoverServices on success connect
            if (BuildConfig.DEBUG) {
                Log.d(T,
                        "device.connectGatt: addr="+address+" autoConnect="+autoConnect);
            }
            mGatts.put(address,
                    device.connectGatt(this, autoConnect, new GattCallback()));
        }
    }

    public synchronized void disconnect(String address) {
        if (BuildConfig.DEBUG) {
            Log.d(T, "disconnect: addr="+address);
        }

        BluetoothGatt gatt = mGatts.get(address);
        if (gatt != null) {
            if (BuildConfig.DEBUG) {
                Log.d(T, "gatt.disconnect: addr="+address);
            }
            gatt.disconnect();
        }
        mGatts.remove(address);
        stopListenRssi(address);
    }

    public void startListenRssi(final String addr) {
        if (BuildConfig.DEBUG) {
            Log.d(T, "startListenRssi: addr="+addr+", will try to stop");
        }

        boolean restart = stopListenRssi(addr);
        if (BuildConfig.DEBUG) {
            Log.d(T, "startListenRssi: addr="+addr+", attempt to stop returned "+restart);
        }
        if (BuildConfig.DEBUG && restart) {
            ITagApplication.errorNotifier.onNext(new Exception("Restart existing Rssi Listener"));
        }

        final BluetoothGatt gatt = mGatts.get(addr);
        if (gatt == null) {
            ITagApplication.errorNotifier.onNext(new Exception("Attempt to startListenRssi on non-existing gatt"));
            return;
        }

        Runnable RssiListener = new Runnable() {
            @Override
            public void run() {
                if (BuildConfig.DEBUG) {
                    Log.d(T, "gatt.readRemoteRssi: addr="+addr);
                }
                gatt.readRemoteRssi();
                mHandler.postDelayed(this, 1000);
            }
        };

        mHandler.post(RssiListener);
        mRssiListeners.put(addr, RssiListener);
    }

    public boolean stopListenRssi(final String addr) {
        if (BuildConfig.DEBUG) {
            Log.d(T, "stopListenRssi: addr="+addr);
        }

        if (mRssiListeners.containsKey(addr)) {
            if (BuildConfig.DEBUG) {
                Log.d(T, "stopListenRssi: addr="+addr+" remove");
            }
            mHandler.removeCallbacks(mRssiListeners.get(addr));
            mRssiListeners.remove(addr);
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(T, "onDestroy");
        }
        for (Map.Entry<String,BluetoothGatt> entry:mGatts.entrySet()){
            entry.getValue().disconnect();
        }
        mGatts.clear();

        super.onDestroy();
    }
}
