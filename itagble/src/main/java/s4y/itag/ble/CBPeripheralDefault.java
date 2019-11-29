package s4y.itag.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.annotation.NonNull;

import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;

class CBPeripheralDefault implements CBPeripheralInterace {

    private int lastStatus = GATT_SUCCESS;

    @NonNull final private Context context;
    @NonNull private final BluetoothDevice device;
    private BluetoothGatt gatt = null;

    static class ConnectionStateChange {
        final BluetoothGatt gatt;
        final int status;
        final int newState;

        ConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            this.gatt = gatt;
            this.status = status;
            this.newState = newState;
        }
    }

    static class ServicesDiscovered {
        final BluetoothGatt gatt;
        final int status;

        ServicesDiscovered(BluetoothGatt gatt, int status) {
            this.gatt = gatt;
            this.status = status;
        }
    }

    static class CharacteristicWrite {
        final BluetoothGatt gatt;
        final BluetoothGattCharacteristic characteristic;
        final int status;

        CharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            this.gatt = gatt;
            this.characteristic = characteristic;
            this.status = status;
        }
    }

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
                    observableAlertVolume.broadcast(alertVolume);
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

    CBPeripheralDefault(Context context, BluetoothDevice device) {
        this.context = context;
        this.device = device;
    }

    @NonNull
    String identifier(){
       return device.getAddress()
    };
}
