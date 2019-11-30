package s4y.itag.ble;

class BLEAlertFactoryDefault implements BLEAlertFactoryInterface {
    @Override
    public BLEAlertInterface alert(BLEConnectionsStoreInterface store) {
        return new BLEAlertDefault(store);
    }
}
