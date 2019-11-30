package s4y.itag.ble;

interface BLEPeripheralInterace {
    String identifier();
    void connect();
    void disconnect();
    BLEError writeInt8(BLECharacteristic characteristic, int value);
    BLEError setNotify(BLECharacteristic characteristic, boolean enable);
    BLEPeripheralState state();
    BLEService[] services();
    BLEPeripheralObservablesInterface observables();
}
