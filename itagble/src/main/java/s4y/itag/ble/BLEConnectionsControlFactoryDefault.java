package s4y.itag.ble;

class BLEConnectionsControlFactoryDefault implements BLEConnectionsControlFactoryInterface {
    @Override
    public BLEConnectionsControlInterface connectionsControl(BLEConnectionsInterface connections) {
        return (BLEConnectionsControlInterface)connections;
    }
}
