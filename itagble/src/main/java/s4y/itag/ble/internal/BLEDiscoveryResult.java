package s4y.itag.ble.internal;

import android.bluetooth.BluetoothDevice;

public class BLEDiscoveryResult {
    final BluetoothDevice peripheral;
    final int rssi;
    final byte[] data;

    public BLEDiscoveryResult(BluetoothDevice peripheral, int rssi, byte[] data) {
        this.peripheral = peripheral;
        this.rssi = rssi;
        this.data = data;
    }
}
