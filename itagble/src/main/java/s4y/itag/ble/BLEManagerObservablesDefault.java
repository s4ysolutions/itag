package s4y.itag.ble;

import java.util.Map;

import s4y.observables.Observable;

class BLEManagerObservablesDefault implements BLEManagerObservablesInterface, CBCentralManagerDelegate {
    private final Observable<CBPeripheralInterace> didConnectPeripheral = new Observable<>();
    private final Observable<CBPeripheralConnected> didFailToConnectPeripheral = new Observable<>();
    private final Observable<CBPeripheralConnected> didDisconnectPeripheral = new Observable<>();
    private final Observable<CBPeripheralDiscovered> didDiscoverPeripheral = new Observable<>();
    private final Observable<CBManagerState> didUpdateState = new Observable<>(CBManagerState.unknown);
    private final Observable<CBPeripheralInterace[]> willRestoreState = new Observable<>();

    @Override
    public Observable<CBPeripheralInterace> getDidConnectPeripheral() {
        return didConnectPeripheral;
    }

    @Override
    public Observable<CBPeripheralConnected> getDidFailToConnectPeripheral() {
        return didFailToConnectPeripheral;
    }

    @Override
    public Observable<CBPeripheralConnected> getDidDisconnectPeripheral() {
        return didDisconnectPeripheral;
    }

    @Override
    public Observable<CBPeripheralDiscovered> getDidDiscoverPeripheral() {
        return didDiscoverPeripheral;
    }

    @Override
    public Observable<CBManagerState> getDidUpdateState() {
        return didUpdateState;
    }

    @Override
    public Observable<CBPeripheralInterace[]> getWillRestoreState() {
        return willRestoreState;
    }

    @Override
    public void willRestorePeripherals(CBCentralManagerInterface central, CBPeripheralInterace[] peripherals) {
        willRestoreState.onNext(peripherals);
    }

    @Override
    public void didDiscoverPeripheral(CBCentralManagerInterface central, CBPeripheralInterace peripheral, Map<String, Object> advertisementData, int rssi) {
        didDiscoverPeripheral.onNext(new CBPeripheralDiscovered(peripheral, advertisementData, rssi));
    }

    @Override
    public void didUpdateState(CBCentralManagerInterface central) {
        didUpdateState.onNext(central.state());
    }

    @Override
    public void didConnectPeripheral(CBCentralManagerInterface central, CBPeripheralInterace peripheral) {
        didConnectPeripheral.onNext(peripheral);
    }

    @Override
    public void didFailToConnectPeripheral(CBCentralManagerInterface central, CBPeripheralInterace peripheral, BLEError error) {
        didFailToConnectPeripheral.onNext(new CBPeripheralConnected(peripheral, error));
    }

    @Override
    public void didDisconnectPeripheral(CBCentralManagerInterface central, CBPeripheralInterace peripheral, BLEError error) {
        didDisconnectPeripheral.onNext(new CBPeripheralConnected(peripheral, error));
    }
}
