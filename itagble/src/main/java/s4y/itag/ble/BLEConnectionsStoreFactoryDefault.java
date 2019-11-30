package s4y.itag.ble;

class BLEConnectionsStoreFactoryDefault implements BLEConnectionsStoreFactoryInterface {
    @Override
    public BLEConnectionsStoreInterface store(BLEConnectionFactoryInterface connectionFactory, BLEFindMeControlInterface findMeControl, BLECentralManagerInterface manager) {
        return new BLEConnectionsStoreDefault(connectionFactory, findMeControl, manager);
    }
}
