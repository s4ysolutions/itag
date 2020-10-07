package s4y.itag.ble;

import solutions.s4y.rasat.Observable;

interface BLECentralManagerObservablesInterface {
    Observable<BLEDiscoveryResult> observablePeripheralDiscovered();
}
