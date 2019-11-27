package s4y.itag.ble.internal;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import s4y.observables.Observable;

interface BLEManagerInterface {
    void scanForPeripherals();
    boolean isScanning();
    void stopScan();
    boolean canScan();

    BluetoothDevice retrievePeripheral(@NonNull String uuid);
    boolean isConnected(BluetoothDevice device);

    Observable<BLEDiscoveryResult> observableDidDiscoverPeripheral();
}
