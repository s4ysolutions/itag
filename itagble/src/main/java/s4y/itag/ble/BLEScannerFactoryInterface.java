package s4y.itag.ble;

interface BLEScannerFactoryInterface {
    BLEScannerInterface scanner(BLECentralManagerInterface manager);
}
