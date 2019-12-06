package s4y.itag.ble;

class BLEDiscoveryResult {
    public final BLEPeripheralInterace peripheral;
    public final int rssi;
    public final byte[] data;

    BLEDiscoveryResult(BLEPeripheralInterace peripheral, int rssi, byte[] data) {
        this.peripheral = peripheral;
        this.rssi = rssi;
        this.data = data;
    }
}
