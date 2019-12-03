package s4y.itag.ble;

import android.bluetooth.BluetoothDevice;

class LeScanResult {
    final public BluetoothDevice device;
    public int rssi;
    final byte[] scanRecord;

    LeScanResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
        this.device = device;
        this.rssi = rssi;
        this.scanRecord = scanRecord;
    }
}
