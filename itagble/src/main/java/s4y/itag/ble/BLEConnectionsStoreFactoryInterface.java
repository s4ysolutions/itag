package s4y.itag.ble;

public interface BLEConnectionsStoreFactoryInterface {
    BLEConnectionsStoreInterface store(
            BLEConnectionFactoryInterface connectionFactory,
            BLEFindMeControl findMeDelegate,
            BLECentralManagerInterface manager,
            BLEPeripheralObservablesFactoryInterface peripheralObservablesFactory
    );
}
