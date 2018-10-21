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
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import solutions.s4y.itag.BuildConfig;
import solutions.s4y.itag.ITagApplication;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

public class ITagGatt {
    private static final int DOUBLE_TAG_CLICK_DELAY = 300;

    private static final String LT = ITagGatt.class.getName();

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

    public final String mAddr;
    private BluetoothDevice mDevice;
    private BluetoothGatt mGatt;
    private boolean mIsError;
    private boolean mIsConnected;
    private boolean mIsConnecting;
    private boolean mIsTransmitting;
    private boolean mIsFindingITag;
    private boolean mIsStartingITagFind;
    private boolean mIsStoppingITagFind;
    public int mRssi;
    private int mDevicesCount;

    private final Handler mHandler = new Handler();

    private BluetoothGattService mServiceImmediateAlert;

    public interface ITagChangeListener {
        void onITagChange(@NonNull final ITagGatt gatt);

        void onITagRssi(@NonNull final ITagGatt gatt, int rssi);

        void onITagClicked(@NonNull final ITagGatt gatt);

        void onITagDoubleClicked(@NonNull final ITagGatt gatt);
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

    private void notifyITagDoubleClicked() {
        for (ITagChangeListener listener : mITagChangeListeners) {
            listener.onITagDoubleClicked(this);
        }
    }

    private void notifyITagRssi(int rssi) {
        for (ITagChangeListener listener : mITagChangeListeners) {
            listener.onITagRssi(this, rssi);
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
                Log.d(LT,
                        "GattCallback.onConnectionStateChange: addr=" + gatt.getDevice().getAddress() +
                                " status=" + status +
                                " state=" + newState);
            }

            if (status == GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (BuildConfig.DEBUG) {
                        Log.d(LT,
                                "GattCallback.onConnectionStateChange, STATE_CONNECTED");
                        Log.d(LT,
                                "will gatt.discoverServices");
                    }
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (BuildConfig.DEBUG) {
                        Log.d(LT,
                                "GattCallback.onConnectionStateChange, STATE_DISCONNECTED");
                    }
                    mGatt.close();
                    endConnection();
                }
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(LT, "GattCallback.onConnectionStateChange: not GATT_SUCCESS, status=" + status);
                }
                // 8 to be know as disconnection status
                if (status != 8) {
                    ITagApplication.handleError(new Exception("onConnectionStateChange failed: code=" + status + " state=" + newState));
                }
                mIsError = true;
                notifyITagChanged();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "GattCallback.onServicesDiscovered: addr=" + gatt.getDevice().getAddress());
            }


            if (GATT_SUCCESS == status) {
                if (BuildConfig.DEBUG) {
                    Log.d(LT, "GattCallback.onServicesDiscovered: GATT_SUCCESS");
                }
                for (BluetoothGattService service : gatt.getServices()) {
                    if (BuildConfig.DEBUG) {
                        Log.d(LT, "GattCallback.onServicesDiscovered, iterate service=" + service.getUuid());
                    }
                    if (IMMEDIATE_ALERT_SERVICE.equals(service.getUuid())) {
                        if (BuildConfig.DEBUG) {
                            Log.d(LT, "GattCallback.onServicesDiscovered, iterated service is IMMEDIATE_ALERT_SERVICE");
                        }
                        mServiceImmediateAlert = service;
                        gatt.readCharacteristic(service.getCharacteristic(ALERT_LEVEL_CHARACTERISTIC));
                        setCharacteristicNotification(gatt, mServiceImmediateAlert.getCharacteristics().get(0));
                    } else if (BATTERY_SERVICE.equals(service.getUuid())) {
                        if (BuildConfig.DEBUG) {
                            Log.d(LT, "GattCallback.onServicesDiscovered, iterated service is BATTERY_SERVICE");
                        }
                    } else if (FIND_ME_SERVICE.equals(service.getUuid())) {
                        if (BuildConfig.DEBUG) {
                            Log.d(LT, "GattCallback.onServicesDiscovered, iterated service is FIND_ME_SERVICE");
                        }
                        BluetoothGattCharacteristic characteristicFindMe = service.getCharacteristics().get(0);
                        setCharacteristicNotification(gatt, characteristicFindMe);
                    } else if (LINK_LOSS_SERVICE.equals(service.getUuid())) {
                        if (BuildConfig.DEBUG) {
                            Log.d(LT, "GattCallback.onServicesDiscovered, iterated service is LINK_LOSS_SERVICE");
                        }
                    } else if (GENERIC_SERVICE.equals(service.getUuid())) {
                        if (BuildConfig.DEBUG) {
                            Log.d(LT, "GattCallback.onServicesDiscovered, iterated service is GENERIC_SERVICE");
                        }
                    } else {
                        if (BuildConfig.DEBUG) {
                            ITagApplication.handleError(new Exception("Unknown service: " + service.getUuid().toString()));
                        }
                    }
                }
                mIsError = false; // we need to reset error because of auto connect
                mIsConnecting = false;
                mIsConnected = true;
                if (mIsRssi) {
                    startListenRssi();
                }
                notifyITagChanged();
            } else {
                mIsError = true;

                if (BuildConfig.DEBUG) {
                    Log.d(LT, "GattCallback.onServicesDiscovered: not GATT_SUCCESS");
                }
                ITagApplication.handleError(new Exception("onServicesDiscovered failed"));
                notifyITagChanged();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "GattCallback.onCharacteristicWrite: addr=" + gatt.getDevice().getAddress()
                        + " characteristic=" + characteristic.getUuid() + " value=" + characteristic.getStringValue(0));
            }
            mIsTransmitting = false;
            if (mIsStartingITagFind) {
                mIsStartingITagFind = false;
                mIsFindingITag = true;
            } else if (mIsStoppingITagFind) {
                mIsStoppingITagFind = false;
                mIsFindingITag = false;
            }
            notifyITagChanged();
        }

        private int mClicksCount = 0;

        private Runnable mWaitForDoubleClick = () -> {
            mClicksCount = 0;
            notifyITagClicked();
        };

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "GattCallback.onCharacteristicChanged: addr=" + gatt.getDevice().getAddress()
                        + " characteristic=" + characteristic.getUuid() + " value=" + characteristic.getStringValue(0));
            }

            if (mClicksCount == 0) {
                mClicksCount++;
                mHandler.postDelayed(mWaitForDoubleClick, DOUBLE_TAG_CLICK_DELAY);
            } else if (mClicksCount == 1) {
                mHandler.removeCallbacks(mWaitForDoubleClick);
                mClicksCount = 0;
                notifyITagDoubleClicked();
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(LT, "onReadRemoteRssi, addr=" + mAddr + " rssi=" + rssi);
            mRssi = rssi;
            notifyITagRssi(rssi);
        }
    };

    private void endConnection() {
        reset();
        notifyITagChanged();
    }

    private void reset() {
        if (mGatt != null) {
            mGatt.close();
        }
        mGatt = null;
        mIsConnected = false;
        mDevice = null;
        mIsError = false;
        mIsTransmitting = false;
        mRssi = -1000;
    }

    private void writeCharacteristicAlertLevel(
            @NonNull final BluetoothGattService service,
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
        if (BuildConfig.DEBUG) {
            Log.d(LT,
                    "writeCharacteristicAlertLevel: service=" + service.getUuid() +
                            " characteristic=" + characteristic.getUuid() +
                            " value desired=" + value +
                            " value =" + (characteristic.getValue() != null && characteristic.getValue().length > 0 ? characteristic.getValue()[0] : "N/A")
            );
        }
        mGatt.writeCharacteristic(characteristic);
        mIsTransmitting = true;
        notifyITagChanged();
    }

    public void connect(@NonNull final Context contex) {
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
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }
        reset();
        mIsConnecting = true;
        notifyITagChanged();
        mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mAddr);
        mGatt = mDevice.connectGatt(contex, true, mCallback);
        mDevicesCount = ITagsDb.getDevices(contex).size();
    }

    private static final int RSSI_INTERVAL_MS = 1000;
    private Runnable mRssiRunable = new Runnable() {
        @Override
        public void run() {
            mGatt.readRemoteRssi();
            mHandler.postDelayed(this, RSSI_INTERVAL_MS * (mDevicesCount == 0 ? 1 : mDevicesCount));
        }
    };

    private boolean mIsRssi;

    public void startListenRssi() {
        stopListenRssi();
        mIsRssi = true;
        if (isConnected()) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "startListenRssi, addr=" + mAddr);
            }
            mHandler.postDelayed(mRssiRunable, RSSI_INTERVAL_MS);
        }
    }

    public void stopListenRssi() {
        mIsRssi = false;
        if (BuildConfig.DEBUG) {
            Log.d(LT, "stopListenRssi, addr=" + mAddr);
        }
        mHandler.removeCallbacks(mRssiRunable);
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

    public void disconnect() {
        if (BuildConfig.DEBUG) {
            if (mGatt == null) {
                ITagApplication.handleError(new Exception("DeviceGatt.disconnect: mGatt==null"));
            }
            if (!mIsConnected) {
                ITagApplication.handleError(new Exception("DeviceGatt.connect: !mIsConnected"));
            }
        }
        stopListenRssi();
        mGatt.disconnect();
        endConnection();
    }

    public void findITag() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "find iTag for 15sec");
        }

        // TODO: handle not connected
        if (!mIsConnected) {
            ITagApplication.handleError(new Exception("Not connected"));
            return;
        }

        mIsStartingITagFind = true;
        mIsFindingITag = false;
        mIsStoppingITagFind = false;
        writeCharacteristicAlertLevel(mServiceImmediateAlert, HIGH_ALERT);
    }

    public void stopFindITag() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "stop find iTag");
        }

        if (!mIsConnected) {
            ITagApplication.handleError(new Exception("Not connected"));
            return;
        }

        mIsStoppingITagFind = true;
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

    public boolean isFindingITag() {
        return mIsFindingITag || mIsStartingITagFind || mIsStoppingITagFind;
    }

    ITagGatt(String addr) {
        mAddr = addr;
    }
}