package s4y.itag.ble;

interface BLEPeripheralInterace {
    String identifier();
    String name();
    String address();
    BLEPeripheralState state();
    BLEService[] services();

    BLEPeripheralObservablesInterface observables();

    void connect();
    void disconnect();
    BLEError writeInt8(BLECharacteristic characteristic, int value);
    BLEError setNotify(BLECharacteristic characteristic, boolean enable);
    void enableRSSI();
    void disableRSSI();
    boolean rssi();
}
