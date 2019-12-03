package s4y.itag.ble;

public class BLEScanResult {
    public final BLEPeripheralInterace peripheral;
    public final int rssi;
    public final byte[] data;

    BLEScanResult(BLEPeripheralInterace peripheral, int rssi, byte[] data) {
        this.peripheral = peripheral;
        this.rssi = rssi;
        this.data = data;
    }
}
