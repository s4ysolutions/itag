package s4y.itag.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class BLEService {
    final BluetoothGattService gatt;
    final BLECharacteristic[] characteristics;

    BLEService(BluetoothGattService gattService) {
        this.gatt = gattService;
        List<BLECharacteristic> characteristicList = new ArrayList<>();
        for (BluetoothGattCharacteristic characteristic: gattService.getCharacteristics()){
            characteristicList.add(new BLECharacteristic(characteristic));
        }
        characteristics = new BLECharacteristic[characteristicList.size()];
        characteristicList.toArray(characteristics);
    }

    UUID uuid() {
        return gatt.getUuid();
    }
}
