package s4y.itag.ble.internal;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

public interface BLEManagerOnDeviceDiscoveredListener {
    void DeviceDiscovered(@NonNull BluetoothDevice device, int rssi, byte[] scanRecord);
}
