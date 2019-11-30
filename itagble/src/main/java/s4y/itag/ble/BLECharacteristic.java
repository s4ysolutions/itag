package s4y.itag.ble;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

class BLECharacteristic {
    final BluetoothGattCharacteristic gattCharacteristic;

    BLECharacteristic(BluetoothGattCharacteristic gattCharacteristic) {
        this.gattCharacteristic = gattCharacteristic;
    }

    UUID uuid() {
        return gattCharacteristic.getUuid();
    }

    int int8() {
        return gattCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
    }

    void setInt8(int value) {
        gattCharacteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
    }

}
