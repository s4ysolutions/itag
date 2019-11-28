package s4y.itag.ble;

public interface BLEConnectionsStoreFactoryInterface {
    BLEConnectionsStoreInterface store(
            BLEConnectionFactoryInterface connectionFactory,
            BLEFindMeControl findMeDelegate,
            CBCentralManagerInterface manager,
            BLEPeripheralObservablesFactoryInterface peripheralObservablesFactory
    );
}
