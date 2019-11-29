package s4y.itag.ble;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import s4y.rasat.Channel;

interface CBCentralManagerInterface {
    void scanForPeripherals();
    boolean isScanning();
    void stopScan();
    boolean canScan();
    CBManagerState state();

    BluetoothDevice retrievePeripheral(@NonNull String uuid);
    boolean isConnected(BluetoothDevice device);

    Channel<BLEDiscoveryResult> observableDidDiscoverPeripheral();
}
