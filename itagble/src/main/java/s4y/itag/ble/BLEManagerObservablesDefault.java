package s4y.itag.ble;

import java.util.Map;

import s4y.rasat.Channel;

class BLEManagerObservablesDefault implements BLEManagerObservablesInterface, CBCentralManagerDelegate {
    private final Channel<CBPeripheralInterace> didConnectPeripheral = new Channel<>();
    private final Channel<CBPeripheralConnectedEvent> didFailToConnectPeripheral = new Channel<>();
    private final Channel<CBPeripheralConnectedEvent> didDisconnectPeripheral = new Channel<>();
    private final Channel<CBPeripheralDiscoveredEvent> didDiscoverPeripheral = new Channel<>();
    private final Channel<CBManagerState> didUpdateState = new Channel<>(CBManagerState.unknown);
    private final Channel<CBPeripheralInterace[]> willRestoreState = new Channel<>();

    @Override
    public Channel<CBPeripheralInterace> getDidConnectPeripheral() {
        return didConnectPeripheral;
    }

    @Override
    public Channel<CBPeripheralConnectedEvent> getDidFailToConnectPeripheral() {
        return didFailToConnectPeripheral;
    }

    @Override
    public Channel<CBPeripheralConnectedEvent> getDidDisconnectPeripheral() {
        return didDisconnectPeripheral;
    }

    @Override
    public Channel<CBPeripheralDiscoveredEvent> getDidDiscoverPeripheral() {
        return didDiscoverPeripheral;
    }

    @Override
    public Channel<CBManagerState> getDidUpdateState() {
        return didUpdateState;
    }

    @Override
    public Channel<CBPeripheralInterace[]> getWillRestoreState() {
        return willRestoreState;
    }

    @Override
    public void willRestorePeripherals(CBCentralManagerInterface central, CBPeripheralInterace[] peripherals) {
        willRestoreState.broadcast(peripherals);
    }

    @Override
    public void didDiscoverPeripheral(CBCentralManagerInterface central, CBPeripheralInterace peripheral, Map<String, Object> advertisementData, int rssi) {
        didDiscoverPeripheral.broadcast(new CBPeripheralDiscoveredEvent(peripheral, advertisementData, rssi));
    }

    @Override
    public void didUpdateState(CBCentralManagerInterface central) {
        didUpdateState.broadcast(central.state());
    }

    @Override
    public void didConnectPeripheral(CBCentralManagerInterface central, CBPeripheralInterace peripheral) {
        didConnectPeripheral.broadcast(peripheral);
    }

    @Override
    public void didFailToConnectPeripheral(CBCentralManagerInterface central, CBPeripheralInterace peripheral, BLEError error) {
        didFailToConnectPeripheral.broadcast(new CBPeripheralConnectedEvent(peripheral, error));
    }

    @Override
    public void didDisconnectPeripheral(CBCentralManagerInterface central, CBPeripheralInterace peripheral, BLEError error) {
        didDisconnectPeripheral.broadcast(new CBPeripheralConnectedEvent(peripheral, error));
    }
}
