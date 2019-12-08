package s4y.itag.ble;

class BLEScannerFactoryDefault implements BLEScannerFactoryInterface {

    @Override
    public BLEScannerInterface scanner(BLECentralManagerInterface manager) {
        return new BLEScannerDefault(manager);
    }
}
