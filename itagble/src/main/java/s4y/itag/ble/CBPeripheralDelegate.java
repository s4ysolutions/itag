package s4y.itag.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

public interface CBPeripheralDelegate {
    void didDiscoverServices(CBPeripheralInterace peripheral, BLEError error);
    void didDiscoverCharacteristics(CBPeripheralInterace peripheral, BluetoothGattService service, BLEError error);
    void didWriteValue(CBPeripheralInterace peripheral, BluetoothGattCharacteristic characteristic, BLEError error);
    void didUpdateNotificationState(CBPeripheralInterace peripheral, BluetoothGattCharacteristic characteristic, BLEError error);
    void didUpdateValue(CBPeripheralInterace peripheral, BluetoothGattCharacteristic characteristic, BLEError error);
}
