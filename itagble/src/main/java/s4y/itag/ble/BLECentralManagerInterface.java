package s4y.itag.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.NonNull;

interface BLECentralManagerInterface {
    void scanForPeripherals();
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
