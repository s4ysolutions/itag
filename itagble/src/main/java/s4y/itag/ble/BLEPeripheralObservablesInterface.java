package s4y.itag.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import s4y.observables.Observable;

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
    class Characteristic {
        final CBPeripheralInterace peripheral;
        final BluetoothGattCharacteristic characteristic;
        final BLEError error;

        public Characteristic(CBPeripheralInterace peripheral, BluetoothGattCharacteristic characteristic, BLEError error) {
            this.peripheral = peripheral;
            this.characteristic = characteristic;
            this.error = error;
        }
    }

    Observable<CBPeripheralInterace> getDidDiscoverServices();
    Observable<DiscoveredCharacteristic> getDidDiscoverCharacteristicsForService();
    Observable<Characteristic> getDidWriteValueForCharacteristic();
    Observable<Characteristic> getDidUpdateNotificationStateForCharacteristic();
    Observable<Characteristic> getDidUpdateValueForCharacteristic();
}
