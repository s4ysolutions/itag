package s4y.itag.ble;

import s4y.rasat.Channel;
import s4y.rasat.Observable;

class BLECentralManagerObservables implements BLECentralManagerObservablesInterface {
    final Channel<BLEScanResult> observablePeripheralDiscovered = new Channel<>();

    @Override
    public Observable<BLEScanResult> observablePeripheralDiscovered() {
        return observablePeripheralDiscovered.observable;
    }
}
