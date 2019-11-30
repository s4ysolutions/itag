package s4y.itag.ble;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

interface BLECentralManagerInterface {
    void scanForPeripherals();
    boolean isScanning();
    void stopScan();
    boolean canScan();
    BLECentralManagerState state();

    CBPeripheralInterace retrievePeripheral(@NonNull String id);
    boolean isConnected(BluetoothDevice device);

    BLECentralManagerObservablesInterface observables();
}
