package s4y.itag.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import s4y.observables.Observable;

import static android.bluetooth.BluetoothProfile.GATT;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;

class CBCentralManagerDefault implements CBCentralManagerInterface {
    private final Context context;
    private final CBCentralManagerDelegate delegate;

    private final Observable<BLEDiscoveryResult> didDiscoverPeripheral = new Observable<>();
    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] data) {
            didDiscoverPeripheral.onNext(new BLEDiscoveryResult(bluetoothDevice, rssi, data));
        }
    };

    CBCentralManagerDefault(Context context, CBCentralManagerDelegate delegate) {
        this.context = context;
        this.delegate = delegate;
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

    public BluetoothDevice retrievePeripheral(String uuid) {
        return BluetoothAdapter.getDefaultAdapter().getRemoteDevice(uuid);
    }

    @Override
    public Observable<BLEDiscoveryResult> observableDidDiscoverPeripheral() {
        return didDiscoverPeripheral;
    }

    @Override
    public boolean isConnected(BluetoothDevice device){
        BluetoothManager bluetoothManager = getManager();
        if (bluetoothManager == null)
            return false;
        int state = bluetoothManager.getConnectionState(device, GATT);
        return state == STATE_CONNECTED;
        // List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.STATE_CONNECTED);
    }
}
