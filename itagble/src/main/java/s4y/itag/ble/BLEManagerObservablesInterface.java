package s4y.itag.ble;

import java.util.Map;

import s4y.observables.Observable;

interface BLEManagerObservablesInterface {
    class CBPeripheralConnected {
       final CBPeripheralInterace peripheral;
       final BLEError error;

        CBPeripheralConnected(CBPeripheralInterace peripheral, BLEError error) {
            this.peripheral = peripheral;
            this.error = error;
        }
    }
    class CBPeripheralDiscovered {
        final CBPeripheralInterace peripheral;
        final Map<String, Object> advertisementData;
        final int rssi;

        CBPeripheralDiscovered(CBPeripheralInterace peripheral, Map<String, Object> advertisementData, int rssi) {
            this.peripheral = peripheral;
            this.advertisementData = advertisementData;
            this.rssi = rssi;
        }
    }
    Observable<CBPeripheralInterace> getDidConnectPeripheral();
    Observable<CBPeripheralConnected> getDidFailToConnectPeripheral();
    Observable<CBPeripheralConnected> getDidDisconnectPeripheral();
    Observable<CBPeripheralDiscovered> getDidDiscoverPeripheral();
    Observable<CBManagerState> getDidUpdateState();
    Observable<CBPeripheralInterace[]> getWillRestoreState();
}
