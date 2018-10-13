package solutions.s4y.itag.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import solutions.s4y.itag.BuildConfig;
import solutions.s4y.itag.ITagApplication;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

public class ITagGatt {
    private static final String T = ITagGatt.class.getName();

    private static final int NO_ALERT = 0x00;
    //public static final int MEDIUM_ALERT = 0x01;
    private static final int HIGH_ALERT = 0x02;

    private static final UUID IMMEDIATE_ALERT_SERVICE =
            UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    private static final UUID FIND_ME_SERVICE =
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID LINK_LOSS_SERVICE =
            UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_SERVICE =
            UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID GENERIC_SERVICE =
            UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    /*
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
            */
    private static final UUID ALERT_LEVEL_CHARACTERISTIC =
            UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    /*
    private static final UUID FIND_ME_CHARACTERISTIC =
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
            */

    private final String mAddr;
    private BluetoothDevice mDevice;
    private BluetoothGatt mGatt;
    private boolean mIsError;
    private boolean mIsConnected;
    private boolean mIsConnecting;
    private boolean mIsTransmitting;

    private BluetoothGattService mImmediateAlertService;

    public interface OnConnectionChangeListener {
        void onConnectionChange(@NotNull final ITagGatt gatt);
    }

    private static final List<OnConnectionChangeListener> mConnectionChangeListeners =
            new ArrayList<>(4);

    public static void addOnConnectionChnageListener(OnConnectionChangeListener listener){
        if (BuildConfig.DEBUG) {
            if (mConnectionChangeListeners.contains(listener)) {
                ITagApplication.handleError(new Error("Add duplicate OnConnectionChangeListener listener"));
            }
        }
        mConnectionChangeListeners.add(listener);
    }

    public static void removeOnConnectionChnageListener(OnConnectionChangeListener listener){
        if (BuildConfig.DEBUG) {
            if (!mConnectionChangeListeners.contains(listener)) {
                ITagApplication.handleError(new Error("Remove nonexisting OnConnectionChangeListener listener"));
            }
        }
        mConnectionChangeListeners.remove(listener);
    }

    private void notifyConnectionChanged(){
        for(OnConnectionChangeListener listener: mConnectionChangeListeners){
            listener.onConnectionChange(this);
        }
    }

    private final BluetoothGattCallback mCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (BuildConfig.DEBUG) {
                Log.d(T,
                        "GattCallback.onConnectionStateChange: addr=" + gatt.getDevice().getAddress() +
                                " status=" + status +
                                " state=" + newState);
            }

            if (status == GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (BuildConfig.DEBUG) {
                        Log.d(T,
                                "GattCallback.onConnectionStateChange, STATE_CONNECTED");
                        Log.d(T,
                                "will gatt.discoverServices");
                    }
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (BuildConfig.DEBUG) {
                        Log.d(T,
                                "GattCallback.onConnectionStateChange, STATE_DISCONNECTED");
                    }
                    endConnection();
                }
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(T, "GattCallback.onServicesDiscovered: not GATT_SUCCESS");
                }
                ITagApplication.handleError(new Exception("onServicesDiscovered failed: code="+status));
                mIsError = true;
                notifyConnectionChanged();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            if (BuildConfig.DEBUG) {
                Log.d(T, "GattCallback.onServicesDiscovered: addr=" + gatt.getDevice().getAddress());
            }


            if (GATT_SUCCESS == status) {
                if (BuildConfig.DEBUG) {
                    Log.d(T, "GattCallback.onServicesDiscovered: GATT_SUCCESS");
                }
                for (BluetoothGattService service : gatt.getServices()) {
                    if (BuildConfig.DEBUG) {
                        Log.d(T, "GattCallback.onServicesDiscovered, iterate service=" + service.getUuid());
                    }
                    if (IMMEDIATE_ALERT_SERVICE.equals(service.getUuid())) {
                        mImmediateAlertService = service;
                        gatt.readCharacteristic(service.getCharacteristic(ALERT_LEVEL_CHARACTERISTIC));
                    } else if (BATTERY_SERVICE.equals(service.getUuid())) {
                    } else if (FIND_ME_SERVICE.equals(service.getUuid())) {
                    } else if (LINK_LOSS_SERVICE.equals(service.getUuid())) {
                    } else if (GENERIC_SERVICE.equals(service.getUuid())) {
                    } else {
                        if (BuildConfig.DEBUG) {
                            ITagApplication.handleError(new Exception("Unknown service: " + service.getUuid().toString()));
                        }
                    }
                }
                mIsConnecting = false;
                mIsConnected = true;
                notifyConnectionChanged();
            } else {
                mIsError = true;

                if (BuildConfig.DEBUG) {
                    Log.d(T, "GattCallback.onServicesDiscovered: not GATT_SUCCESS");
                }
                ITagApplication.handleError(new Exception("onServicesDiscovered failed"));
                notifyConnectionChanged();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            mIsTransmitting=false;
            notifyConnectionChanged();
        }
    };

    private void endConnection() {
        mGatt.close();
        reset();
        notifyConnectionChanged();
    }

    private void reset(){
        mGatt = null;
        mIsConnected = false;
        mDevice = null;
        mIsError = false;
        mIsTransmitting = false;
    }

    private void writeCharacteristic(
        @NotNull final BluetoothGattService service,
        @NotNull final UUID characteristicUUID,
        int value
    ){
        if (service.getCharacteristics() == null || service.getCharacteristics().size() == 0) {
            ITagApplication
                    .handleError(
                            new Exception(
                                    "DeviceGatt.writeCharacteristic, no characteristic="
                                            + characteristicUUID));
            return;
        }
        final BluetoothGattCharacteristic characteristic = service.getCharacteristics().get(0);
        characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        Log.d(T,
                "writeCharacteristic: service=" + service.getUuid() +
                        " characteristic=" + characteristic.getUuid() +
                        " value desired=" + value+
                        " value =" + (characteristic.getValue()!=null && characteristic.getValue().length>0?characteristic.getValue()[0]:"N/A")
        );
        mGatt.writeCharacteristic(characteristic);
        mIsTransmitting=true;
        notifyConnectionChanged();
    }

    void connect(@NotNull final Context contex) {
        if (BuildConfig.DEBUG) {
            if (mGatt != null) {
                ITagApplication.handleError(new Exception("DeviceGatt.connect: mGatt!=null"));
            }
            if (mIsConnected) {
                ITagApplication.handleError(new Exception("DeviceGatt.connect: mIsConnected"));
            }
            if (mIsConnecting) {
                ITagApplication.handleError(new Exception("DeviceGatt.connect: mIsConnecting"));
            }
        }
        reset();
        mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mAddr);
        mGatt=mDevice.connectGatt(contex, true, mCallback);
    }

    public void disconnect() {
        if (BuildConfig.DEBUG) {
            if (mGatt == null) {
                ITagApplication.handleError(new Exception("DeviceGatt.disconnect: mGatt==null"));
            }
            if (!mIsConnected) {
                ITagApplication.handleError(new Exception("DeviceGatt.connect: !mIsConnected"));
            }
        }
        mGatt.disconnect();
        endConnection();
    }

    void alert() {
        if (BuildConfig.DEBUG) {
            Log.d(T,"alert15sec");
        }

        // TODO: handle not connected
        if (!mIsConnected) {
            ITagApplication.handleError(new Exception("Not connected"));
            return;
        }

        writeCharacteristic(mImmediateAlertService, ALERT_LEVEL_CHARACTERISTIC, HIGH_ALERT);
    }

    public void stopAlert() {
        if (BuildConfig.DEBUG) {
            Log.d(T,"alert15sec");
        }

        if (!mIsConnected) {
            ITagApplication.handleError(new Exception("Not connected"));
            return;
        }

        writeCharacteristic(mImmediateAlertService, ALERT_LEVEL_CHARACTERISTIC, NO_ALERT);
    }

    public boolean isConnecting() {
        return mIsConnecting;
    }

    public boolean isConnected() {
        return mIsConnected && !mIsError;
    }

    public boolean isTransmitting() {
        return mIsTransmitting;
    }

    public ITagGatt(String addr) {
        mAddr = addr;
    }
}