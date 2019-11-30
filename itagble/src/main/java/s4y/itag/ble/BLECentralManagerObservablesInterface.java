package s4y.itag.ble;

import androidx.annotation.NonNull;

import s4y.rasat.Observable;

interface BLECentralManagerObservablesInterface {
    class DiscoveredEvent {
        final CBPeripheralInterace peripheral;
        final byte[] advertisementData;
        final int rssi;

        DiscoveredEvent(@NonNull CBPeripheralInterace peripheral, byte[] advertisementData, int rssi) {
            this.peripheral = peripheral;
            this.advertisementData = advertisementData;
            this.rssi = rssi;
        }
    }

    Observable<DiscoveredEvent> observablePeripheralDiscovered();
}
