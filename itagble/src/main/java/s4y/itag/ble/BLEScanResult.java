package s4y.itag.ble;

public class BLEScanResult {
    public final String id;
    public final String name;
    public int rssi;

    BLEScanResult(String id, String name, int rssi) {
        this.id = id;
        this.name = name;
        this.rssi = rssi;
    }
}
