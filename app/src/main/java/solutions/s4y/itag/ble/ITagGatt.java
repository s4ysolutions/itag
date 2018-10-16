package solutions.s4y.itag.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID ALERT_LEVEL_CHARACTERISTIC =
            UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    /*
    private static final UUID FIND_ME_CHARACTERISTIC =
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
            */

    final String mAddr;
    private BluetoothDevice mDevice;
    private BluetoothGatt mGatt;
    private boolean mIsError;
    private boolean mIsConnected;
    private boolean mIsConnecting;
    private boolean mIsTransmitting;
    private boolean mIsAlert;
    private boolean mIsAlertStarting;
    private boolean mIsAlertStoping;

    private BluetoothGattService mServiceImmediateAlert;

    public interface ITagChangeListener {
        void onITagChange(@NotNull final ITagGatt gatt);
        void onITagClicked(@NotNull final ITagGatt gatt);
    }

    private static final List<ITagChangeListener> mITagChangeListeners =
            new ArrayList<>(4);

    public static void addOnITagChangeListener(ITagChangeListener listener) {
        if (BuildConfig.DEBUG) {
            if (mITagChangeListeners.contains(listener)) {
                ITagApplication.handleError(new Error("Add duplicate ITagChangeListener listener"));
            }
        }
        mITagChangeListeners.add(listener);
    }

    public static void removeOnITagChangeListener(ITagChangeListener listener) {
        if (BuildConfig.DEBUG) {
            if (!mITagChangeListeners.contains(listener)) {
                ITagApplication.handleError(new Error("Remove nonexisting ITagChangeListener listener"));
            }
        }
        mITagChangeListeners.remove(listener);
    }

    private void notifyITagChanged() {
        for (ITagChangeListener listener : mITagChangeListeners) {
            listener.onITagChange(this);
        }
    }

    private void notifyITagClicked() {
        for (ITagChangeListener listener : mITagChangeListeners) {
            listener.onITagClicked(this);
        }
    }

    private void setCharacteristicNotification(BluetoothGatt bluetoothgatt, BluetoothGattCharacteristic bluetoothgattcharacteristic) {
        bluetoothgatt.setCharacteristicNotification(bluetoothgattcharacteristic, true);
        BluetoothGattDescriptor descriptor = bluetoothgattcharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothgatt.writeDescriptor(descriptor);
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
                    mGatt.close();
                    endConnection();
                }
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(T, "GattCallback.onConnectionStateChange: not GATT_SUCCESS");
                }
                // 8 to be know as disconnection status
                if (status!= 8) {
                    ITagApplication.handleError(new Exception("onConnectionStateChange failed: code=" + status + " state="+newState));
                }
                mIsError = true;
                notifyITagChanged();
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
                        if (BuildConfig.DEBUG) {
                            Log.d(T, "GattCallback.onServicesDiscovered, iterated service is IMMEDIATE_ALERT_SERVICE");
                        }
                        mServiceImmediateAlert = service;
                        gatt.readCharacteristic(service.getCharacteristic(ALERT_LEVEL_CHARACTERISTIC));
                        setCharacteristicNotification(gatt, mServiceImmediateAlert.getCharacteristics().get(0));
                    } else if (BATTERY_SERVICE.equals(service.getUuid())) {
                        if (BuildConfig.DEBUG) {
                            Log.d(T, "GattCallback.onServicesDiscovered, iterated service is BATTERY_SERVICE");
                        }
                    } else if (FIND_ME_SERVICE.equals(service.getUuid())) {
                        if (BuildConfig.DEBUG) {
                            Log.d(T, "GattCallback.onServicesDiscovered, iterated service is FIND_ME_SERVICE");
                        }
                        BluetoothGattCharacteristic characteristicFindMe = service.getCharacteristics().get(0);
                        setCharacteristicNotification(gatt, characteristicFindMe);
                    } else if (LINK_LOSS_SERVICE.equals(service.getUuid())) {
                        if (BuildConfig.DEBUG) {
                            Log.d(T, "GattCallback.onServicesDiscovered, iterated service is LINK_LOSS_SERVICE");
                        }
                    } else if (GENERIC_SERVICE.equals(service.getUuid())) {
                        if (BuildConfig.DEBUG) {
                            Log.d(T, "GattCallback.onServicesDiscovered, iterated service is GENERIC_SERVICE");
                        }
                    } else {
                        if (BuildConfig.DEBUG) {
                            ITagApplication.handleError(new Exception("Unknown service: " + service.getUuid().toString()));
                        }
                    }
                }
                mIsError=false; // we need to reset error because of auto connect
                mIsConnecting = false;
                mIsConnected = true;
                notifyITagChanged();
            } else {
                mIsError = true;

                if (BuildConfig.DEBUG) {
                    Log.d(T, "GattCallback.onServicesDiscovered: not GATT_SUCCESS");
                }
                ITagApplication.handleError(new Exception("onServicesDiscovered failed"));
                notifyITagChanged();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (BuildConfig.DEBUG) {
                Log.d(T, "GattCallback.onCharacteristicWrite: addr=" + gatt.getDevice().getAddress()
                        +" characteristic="+characteristic.getUuid()+" value="+characteristic.getStringValue(0));
            }
            mIsTransmitting = false;
            if (mIsAlertStarting) {
                mIsAlertStarting=false;
                mIsAlert=true;
            }else if (mIsAlertStoping) {
                mIsAlertStoping=false;
                mIsAlert=false;
            }
            notifyITagChanged();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (BuildConfig.DEBUG) {
                Log.d(T, "GattCallback.onCharacteristicChanged: addr=" + gatt.getDevice().getAddress()
                +" characteristic="+characteristic.getUuid()+" value="+characteristic.getStringValue(0));
            }
            notifyITagClicked();
        }


    };

    private void endConnection() {
        reset();
        notifyITagChanged();
    }

    private void reset() {
        mIsConnected = false;
        mDevice = null;
        mIsError = false;
        mIsTransmitting = false;
    }

    private void writeCharacteristicAlertLevel(
            @NotNull final BluetoothGattService service,
            int value
    ) {
        if (service.getCharacteristics() == null || service.getCharacteristics().size() == 0) {
            ITagApplication
                    .handleError(
                            new Exception(
                                    "DeviceGatt.writeCharacteristicAlertLevel, no characteristic="
                                            + ITagGatt.ALERT_LEVEL_CHARACTERISTIC));
            return;
        }
        final BluetoothGattCharacteristic characteristic = service.getCharacteristics().get(0);
        characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        Log.d(T,
                "writeCharacteristicAlertLevel: service=" + service.getUuid() +
                        " characteristic=" + characteristic.getUuid() +
                        " value desired=" + value +
                        " value =" + (characteristic.getValue() != null && characteristic.getValue().length > 0 ? characteristic.getValue()[0] : "N/A")
        );
        mGatt.writeCharacteristic(characteristic);
        mIsTransmitting = true;
        notifyITagChanged();
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
        mIsConnecting = true;
        notifyITagChanged();
        mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mAddr);
        mGatt = mDevice.connectGatt(contex, true, mCallback);
    }

    void close() {
        if (BuildConfig.DEBUG) {
            if (mGatt == null) {
                ITagApplication.handleError(new Exception("DeviceGatt.disconnect: mGatt==null"));
            }
            if (!mIsConnected) {
                ITagApplication.handleError(new Exception("DeviceGatt.connect: !mIsConnected"));
            }
        }
        reset();
        mGatt.close();
    }

    void disconnect() {
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

    public void alert() {
        if (BuildConfig.DEBUG) {
            Log.d(T, "alert15sec");
        }

        // TODO: handle not connected
        if (!mIsConnected) {
            ITagApplication.handleError(new Exception("Not connected"));
            return;
        }

        mIsAlertStarting=true;
        mIsAlert=false;
        mIsAlertStoping=false;
        writeCharacteristicAlertLevel(mServiceImmediateAlert, HIGH_ALERT);
    }

    public void stopAlert() {
        if (BuildConfig.DEBUG) {
            Log.d(T, "alert15sec");
        }

        if (!mIsConnected) {
            ITagApplication.handleError(new Exception("Not connected"));
            return;
        }

        mIsAlertStoping=true;
        writeCharacteristicAlertLevel(mServiceImmediateAlert, NO_ALERT);
    }

    public boolean isConnecting() {
        return mIsConnecting;
    }

    public boolean isError() {
        // without mIsConnected mIsError may overwrite other statuse
        // but error may happen beyond connected state boundaries
        return /* mIsConnected && */ mIsError;
    }

    public boolean isConnected() {
        return mIsConnected && !mIsError;
    }

    public boolean isTransmitting() {
        return mIsTransmitting;
    }

    public boolean isAlert() {
        return mIsAlert || mIsAlertStarting || mIsAlertStoping;
    }

    ITagGatt(String addr) {
        mAddr = addr;
    }
}