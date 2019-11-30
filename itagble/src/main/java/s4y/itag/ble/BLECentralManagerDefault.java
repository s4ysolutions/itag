package s4y.itag.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import androidx.annotation.NonNull;

import static android.bluetooth.BluetoothProfile.GATT;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;

class BLECentralManagerDefault implements BLECentralManagerInterface {
    private final Context context;
    private final BLECentralManagerObservables observables = new BLECentralManagerObservables();

    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] data) {
            observables
                    .observablePeripheralDiscovered
                    .broadcast(new BLECentralManagerObservablesInterface.DiscoveredEvent(
                            new BLEPeripheralDefault(context,  BLECentralManagerDefault.this, bluetoothDevice),
                            data,
                            rssi
                    ));
        }
    };

    BLECentralManagerDefault(Context context) {
        this.context = context;
    }

    private BluetoothManager getManager() {
        return (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    private BluetoothAdapter getAdapter() {
        BluetoothManager bluetoothManager = getManager();
        if (bluetoothManager == null)
            return null;
        return bluetoothManager.getAdapter();
    }

    public boolean canScan() {
        return true;
    }

    @Override
    public BLECentralManagerState state() {
        return null;
    }

    private boolean isScanning = false;

    public boolean isScanning() {
        return isScanning;
    }

    public void scanForPeripherals() {
        if (isScanning) {
            stopScan();
        }
        BluetoothAdapter adapter = getAdapter();
        if (adapter != null) {
            adapter.startLeScan(leScanCallback);
            isScanning = true;
        }
    }

    public void stopScan() {
        BluetoothAdapter adapter = getAdapter();
        if (adapter != null) {
            adapter.stopLeScan(leScanCallback);
            isScanning = false;
        }
    }

    public BLEPeripheralInterace retrievePeripheral(@NonNull String id) {
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(id);
        return new BLEPeripheralDefault(context, this, device);
    }

    @Override
    public boolean connected(BluetoothDevice device) {
        BluetoothManager bluetoothManager = getManager();
        if (bluetoothManager == null)
            return false;
        int state = bluetoothManager.getConnectionState(device, GATT);
        return state == STATE_CONNECTED;
        // List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.STATE_CONNECTED);
    }

    @Override
    public BLECentralManagerObservablesInterface observables() {
        return observables;
    }

}
