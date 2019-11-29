package s4y.itag.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import s4y.rasat.Channel;

class BLEPeripheralObservablesDefault implements BLEPeripheralObservablesInterface, CBPeripheralDelegate {
    private final Channel<CBPeripheralInterace> discoverServices = new Channel<>();
    private final Channel<DiscoveredCharacteristic> didDiscoverCharacteristicsForService = new Channel<>();
    private final Channel<CharacteristicEvent> didWriteValueForCharacteristic = new Channel<>();
    private final Channel<CharacteristicEvent> didUpdateNotificationStateForCharacteristic = new Channel<>();
    private final Channel<CharacteristicEvent> didUpdateValueForCharacteristic = new Channel<>();

    @Override
    public Channel<CBPeripheralInterace> didDiscoverServices() {
        return discoverServices;
    }

    @Override
    public Channel<DiscoveredCharacteristic> didDiscoverCharacteristicsForService() {
        return didDiscoverCharacteristicsForService;
    }

    @Override
    public Channel<CharacteristicEvent> didWriteValueForCharacteristic() {
        return didWriteValueForCharacteristic;
    }

    @Override
    public Channel<CharacteristicEvent> didUpdateNotificationStateForCharacteristic() {
        return didUpdateNotificationStateForCharacteristic;
    }

    @Override
    public Channel<CharacteristicEvent> didUpdateValueForCharacteristic() {
        return didUpdateValueForCharacteristic;
    }

    @Override
    public void didDiscoverServices(CBPeripheralInterace peripheral, BLEError error) {

    }

    @Override
    public void didDiscoverCharacteristics(CBPeripheralInterace peripheral, BluetoothGattService service, BLEError error) {

    }

    @Override
    public void didWriteValue(CBPeripheralInterace peripheral, BluetoothGattCharacteristic characteristic, BLEError error) {

    }

    @Override
    public void didUpdateNotificationState(CBPeripheralInterace peripheral, BluetoothGattCharacteristic characteristic, BLEError error) {

    }

    @Override
    public void didUpdateValue(CBPeripheralInterace peripheral, BluetoothGattCharacteristic characteristic, BLEError error) {

    }
}
