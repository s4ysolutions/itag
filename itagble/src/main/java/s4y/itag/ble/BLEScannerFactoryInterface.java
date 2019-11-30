package s4y.itag.ble;

interface BLEScannerFactoryInterface {
    BLEScannerInterface scanner(BLEConnectionsInterface connections, BLECentralManagerInterface manager);
}
