package s4y.itag.ble;

import android.bluetooth.BluetoothDevice;

class BLEScanResult {
    final BLEPeripheralInterace device;
    final int rssi;
    final byte[] data;

    BLEScanResult(BLEPeripheralInterace device, int rssi, byte[] data) {
        this.device = device;
        this.rssi = rssi;
        this.data = data;
    }
}
