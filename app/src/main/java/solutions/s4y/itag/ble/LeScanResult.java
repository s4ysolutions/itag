package solutions.s4y.itag.ble;

import android.bluetooth.BluetoothDevice;

public class LeScanResult {
    final public BluetoothDevice device;
    final int rssi;
    final byte[] scanRecord;

    public LeScanResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
        this.device = device;
        this.rssi = rssi;
        this.scanRecord = scanRecord;
    }
}
