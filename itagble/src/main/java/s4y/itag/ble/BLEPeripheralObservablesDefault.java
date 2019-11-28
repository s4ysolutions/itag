package s4y.itag.ble;

import s4y.observables.Observable;

class BLEPeripheralObservablesDefault implements BLEPeripheralObservablesInterface {
    private final Observable<>
    @Override
    public Observable<CBPeripheralInterace> getDidDiscoverServices() {
        return null;
    }

    @Override
    public Observable<DiscoveredCharacteristic> getDidDiscoverCharacteristicsForService() {
        return null;
    }

    @Override
    public Observable<Characteristic> getDidWriteValueForCharacteristic() {
        return null;
    }

    @Override
    public Observable<Characteristic> getDidUpdateNotificationStateForCharacteristic() {
        return null;
    }

    @Override
    public Observable<Characteristic> getDidUpdateValueForCharacteristic() {
        return null;
    }
}
