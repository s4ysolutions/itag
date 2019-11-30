package s4y.itag.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.annotation.NonNull;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;

class BLEPeripheralDefault implements CBPeripheralInterace {

    private int lastStatus = GATT_SUCCESS;

    @NonNull final private Context context;
    @NonNull private final BluetoothDevice device;
    private CBPeripheralDelegate delegate;
    private BluetoothGatt gatt = null;


    BLEPeripheralDefault(Context context, BluetoothDevice device) {
        this.context = context;
        this.device = device;
    }

    @NonNull
    public String identifier(){
       return device.getAddress();
    }
    @Override
    public void connect(){
        device.connectGatt(context, true, gattCallback)
    }

    @Override
    public void setDelegagte(CBPeripheralDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public CBPeripheralDelegate delegate() {
        return delegate;
    }

}
