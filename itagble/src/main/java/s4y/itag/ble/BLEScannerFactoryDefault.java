package s4y.itag.ble;

class BLEScannerFactoryDefault implements BLEScannerFactoryInterface {

    @Override
    public BLEScannerInterface scanner(BLEConnectionsInterface connections, BLECentralManagerInterface manager) {
        return new BLEScannerDefault(connections, manager);
    }
}
