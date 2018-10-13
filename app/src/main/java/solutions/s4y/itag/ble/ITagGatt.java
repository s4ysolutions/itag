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

    private BluetoothDevice mDevice;
    private BluetoothGatt mGatt;
    private boolean mIsError;
    private boolean mIsConnected;
    private boolean mIsConnecting;

    private BluetoothGattService mImmediateAlertService;

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
                ITagApplication.handleError(new Exception("onServicesDiscovered failed"));
                mIsError = true;
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
            } else {
                mIsError = true;

                if (BuildConfig.DEBUG) {
                    Log.d(T, "GattCallback.onServicesDiscovered: not GATT_SUCCESS");
                }
                ITagApplication.handleError(new Exception("onServicesDiscovered failed"));
            }
        }

    };

    private void endConnection() {
        mGatt.close();
        mGatt = null;
        mIsConnected = false;
        mDevice = null;
        mIsError = false;
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
        Log.d(T,
                "writeCharacteristic: service=" + service.getUuid() +
                        " characteristic=" + characteristic.getUuid() +
                        " value desired=" + value+
                        " value =" + characteristic.getValue()[0]
        );
        mGatt.writeCharacteristic(characteristic);
    }

    public void connect(@NotNull final Context contex, @NotNull final String address) {
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
        mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
        mDevice.connectGatt(contex, true, mCallback);
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

    public void alert() {
        if (BuildConfig.DEBUG) {
            Log.d(T,"alert15sec");
        }

        if (!mIsConnected) {
            ITagApplication.handleError(new Exception("Not connected"));
            return;
        }

        writeCharacteristic(mImmediateAlertService, ALERT_LEVEL_CHARACTERISTIC, NO_ALERT);
    }

    public void stopAlert() {
        if (BuildConfig.DEBUG) {
            Log.d(T,"alert15sec");
        }

        if (!mIsConnected) {
            ITagApplication.handleError(new Exception("Not connected"));
            return;
        }

        writeCharacteristic(mImmediateAlertService, ALERT_LEVEL_CHARACTERISTIC, HIGH_ALERT);
    }

}