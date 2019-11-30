package s4y.itag.ble;

interface BLEConnectionsStoreFactoryInterface {
    BLEConnectionsStoreInterface store(
            BLEConnectionFactoryInterface connectionFactory,
            BLEFindMeControlInterface findMeControl,
            BLECentralManagerInterface manager
    );
}
