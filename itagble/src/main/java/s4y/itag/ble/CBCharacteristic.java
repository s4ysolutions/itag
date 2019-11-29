package s4y.itag.ble;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

class CBCharacteristic {
    final BluetoothGattCharacteristic gatt;

    CBCharacteristic(BluetoothGattCharacteristic gattCharacteristic) {
        this.gatt = gattCharacteristic;
    }

    UUID uuid() {
        return gatt.getUuid();
    }
}
