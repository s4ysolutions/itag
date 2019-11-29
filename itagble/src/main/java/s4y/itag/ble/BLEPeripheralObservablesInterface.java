package s4y.itag.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import s4y.rasat.Observable;

interface BLEPeripheralObservablesInterface {
    class DiscoveredCharacteristic {
        final CBPeripheralInterace peripheral;
        final BluetoothGattService service;
        final BluetoothGattCharacteristic characteristic;
        final BLEError error;

        public DiscoveredCharacteristic(CBPeripheralInterace peripheral, BluetoothGattService service, BluetoothGattCharacteristic characteristic, BLEError error) {
            this.peripheral = peripheral;
            this.service = service;
            this.characteristic = characteristic;
            this.error = error;
        }
    }
    class CharacteristicEvent {
        final CBPeripheralInterace peripheral;
        final BluetoothGattCharacteristic characteristic;
        final BLEError error;

        public CharacteristicEvent(CBPeripheralInterace peripheral, BluetoothGattCharacteristic characteristic, BLEError error) {
            this.peripheral = peripheral;
            this.characteristic = characteristic;
            this.error = error;
        }
    }

    Observable<CBPeripheralInterace> didDiscoverServices();
    Observable<DiscoveredCharacteristic> didDiscoverCharacteristicsForService();
    Observable<CharacteristicEvent> didWriteValueForCharacteristic();
    Observable<CharacteristicEvent> didUpdateNotificationStateForCharacteristic();
    Observable<CharacteristicEvent> didUpdateValueForCharacteristic();
}
