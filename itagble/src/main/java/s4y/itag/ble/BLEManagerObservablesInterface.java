package s4y.itag.ble;

import java.util.Map;
import s4y.rasat.Observable;

interface BLEManagerObservablesInterface {
    class CBPeripheralConnectedEvent {
       final CBPeripheralInterace peripheral;
       final BLEError error;

        CBPeripheralConnectedEvent(CBPeripheralInterace peripheral, BLEError error) {
            this.peripheral = peripheral;
            this.error = error;
        }
    }
    class CBPeripheralDiscoveredEvent {
        final CBPeripheralInterace peripheral;
        final Map<String, Object> advertisementData;
        final int rssi;

        CBPeripheralDiscoveredEvent(CBPeripheralInterace peripheral, Map<String, Object> advertisementData, int rssi) {
            this.peripheral = peripheral;
            this.advertisementData = advertisementData;
            this.rssi = rssi;
        }
    }
    Observable<CBPeripheralInterace> didConnectPeripheral();
    Observable<CBPeripheralConnectedEvent> didFailToConnectPeripheral();
    Observable<CBPeripheralConnectedEvent> didDisconnectPeripheral();
    Observable<CBPeripheralDiscoveredEvent> didDiscoverPeripheral();
    Observable<CBManagerState> didUpdateState();
    Observable<CBPeripheralInterace[]> willRestoreState();
}
