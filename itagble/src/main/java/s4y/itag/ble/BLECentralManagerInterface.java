package s4y.itag.ble;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

interface BLECentralManagerInterface {
    void startScan();
    boolean isScanning();
    void stopScan();
    boolean canScan();
    @NonNull BLEState state();
    BLEError enable();

    BLEPeripheralInterace retrievePeripheral(@NonNull String id);
    boolean connected(BluetoothDevice device);

    BLECentralManagerObservablesInterface observables();

    void postOperation(Runnable runnable);
    void postOperation(Runnable runnable, long delay);
    void cancelOperation(Runnable runnable);
}
