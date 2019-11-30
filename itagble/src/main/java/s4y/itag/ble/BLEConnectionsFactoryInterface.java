package s4y.itag.ble;

interface BLEConnectionsFactoryInterface {
    BLEConnectionsInterface connections(BLEConnectionsStoreInterface store);
}
