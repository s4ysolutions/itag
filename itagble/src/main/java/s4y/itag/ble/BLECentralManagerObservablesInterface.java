package s4y.itag.ble;

import androidx.annotation.NonNull;

import s4y.rasat.Observable;

interface BLECentralManagerObservablesInterface {
    Observable<BLEScanResult> observablePeripheralDiscovered();
}
