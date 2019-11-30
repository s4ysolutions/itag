package s4y.itag.ble;

import s4y.rasat.Channel;
import s4y.rasat.Observable;

class BLECentralManagerObservables implements BLECentralManagerObservablesInterface {
    final Channel<DiscoveredEvent> observablePeripheralDiscovered = new Channel<>();

    @Override
    public Observable<DiscoveredEvent> observablePeripheralDiscovered() {
        return observablePeripheralDiscovered.observable;
    }
}
