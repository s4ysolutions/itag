package s4y.itag.ble;

import android.bluetooth.BluetoothGattCharacteristic;

enum AlertVolume {
    NO_ALERT((byte) 0x00),
    MEDIUM_ALERT((byte) 0x01),
    HIGH_ALERT((byte) 0x02);

    public final byte value;

    AlertVolume(byte value) {
        this.value = value;
    }

    static AlertVolume fromCharacteristic(BluetoothGattCharacteristic characteristic) {
        int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        switch (value) {
            case 0:
                return NO_ALERT;
            case 1:
                return MEDIUM_ALERT;
            default:
                return HIGH_ALERT;
        }
    }
}
