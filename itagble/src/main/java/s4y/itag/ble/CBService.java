package s4y.itag.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class CBService {
    final BluetoothGattService gatt;
    final CBCharacteristic[] characteristics;

    CBService(BluetoothGattService gattService) {
        this.gatt = gattService;
        List<CBCharacteristic> characteristicList = new ArrayList<>();
        for (BluetoothGattCharacteristic characteristic: gattService.getCharacteristics()){
            characteristicList.add(new CBCharacteristic(characteristic));
        }
        characteristics = new CBCharacteristic[characteristicList.size()];
        characteristicList.toArray(characteristics);
    }

    UUID uuid() {
        return gatt.getUuid();
    }
}
