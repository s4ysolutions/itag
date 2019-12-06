package s4y.itag.ble;

import s4y.rasat.Observable;

interface BLECentralManagerObservablesInterface {
    Observable<BLEDiscoveryResult> observablePeripheralDiscovered();
}
