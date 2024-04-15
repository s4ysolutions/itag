package s4y.itag.ble;

class BLEDiscoveryResult {
    public final BLEPeripheralInterace peripheral;
    public final int rssi;

    BLEDiscoveryResult(BLEPeripheralInterace peripheral, int rssi) {
        this.peripheral = peripheral;
        this.rssi = rssi;
    }
}
