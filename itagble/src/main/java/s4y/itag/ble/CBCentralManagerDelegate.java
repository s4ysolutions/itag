package s4y.itag.ble;

import java.util.Map;

interface CBCentralManagerDelegate {
    void willRestorePeripherals(CBCentralManagerInterface central,
                                CBPeripheralInterace[] peripherals);

    void didDiscoverPeripheral(CBCentralManagerInterface central,
                               CBPeripheralInterace peripheral,
                               Map<String, Object> advertisementData,
                               int rssi);

    void didUpdateState(CBCentralManagerInterface central);

    void didConnectPeripheral(CBCentralManagerInterface central,
                              CBPeripheralInterace peripheral);

    void didFailToConnectPeripheral(CBCentralManagerInterface central,
                                    CBPeripheralInterace peripheral,
                                    BLEError error);

    void didDisconnectPeripheral(CBCentralManagerInterface central,
                          CBPeripheralInterace peripheral,
                          BLEError error);
}
