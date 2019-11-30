package s4y.itag.ble;

class BLEConnectionsFactoryDefault implements BLEConnectionsFactoryInterface {
    @Override
    public BLEConnectionsInterface connections(BLEConnectionsStoreInterface store) {
        return new BLEConnectionsDefault(store);
    }
}
