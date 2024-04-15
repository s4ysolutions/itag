package s4y.itag.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.bluetooth.BluetoothProfile.GATT;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.le.ScanSettings.MATCH_MODE_AGGRESSIVE;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_BALANCED;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

class BLECentralManagerDefault implements BLECentralManagerInterface, AutoCloseable {
    private static final String L = BLECentralManagerDefault.class.getName();
    private final Context context;
    private final List<String> devices_ids;
    private final HandlerThread operationsThread = new HandlerThread("BLE Central Manager operations");
    private final Handler operationsHandler;
    private final Map<String, BLEPeripheralInterace> scanned = new HashMap<>();

    private final BLECentralManagerObservables observables = new BLECentralManagerObservables();

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice bluetoothDevice = result.getDevice();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d("ingo", "result.isLegacy() " + result.getDevice().getName() + ": " + result.isLegacy());
            }
            int rssi = result.getRssi();
            if (BuildConfig.DEBUG) {
                Log.d(L,"onLeScan address="+bluetoothDevice.getAddress()+" rsss="+ rssi + " thread="+Thread.currentThread().getName());
            }
            BLEPeripheralInterace peripheral = scanned.get(bluetoothDevice.getAddress());
            if (peripheral == null) {
                peripheral = new BLEPeripheralDefault(
                        context,
                        BLECentralManagerDefault.this,
                        bluetoothDevice);
                scanned.put(bluetoothDevice.getAddress(), peripheral);
            }
            observables
                    .observablePeripheralDiscovered
                    .broadcast(new BLEDiscoveryResult(
                            peripheral,
                            rssi
                    ));
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    BLECentralManagerDefault(Context context, List<String> devices_ids) {
        this.context = context;
        this.devices_ids = devices_ids;
        operationsThread.start();
        operationsHandler = new Handler(operationsThread.getLooper());
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

    @NonNull
    @Override
    public BLEState state() {
        BluetoothAdapter bluetoothAdapter = getAdapter();
        if (bluetoothAdapter == null)
            return BLEState.NO_ADAPTER;
        if (bluetoothAdapter.isEnabled())
            return BLEState.OK;
        else
            return BLEState.NOT_ENABLED;
    }

    @Override
    public BLEError enable() {
        BluetoothAdapter adapter = getAdapter();
        if (adapter == null) {
            return BLEError.noAdapter;
        }
        adapter.enable(); // TODO: deprecated
        return BLEError.ok;
    }

    private boolean isScanning = false;

    private boolean isScanning(BluetoothAdapter adapter) {
        return  adapter != null && isScanning;
    }

    public boolean isScanning() {
        BluetoothAdapter adapter = getAdapter();
        return  adapter != null && isScanning;
    }

    public void startScanForNewDevices(){
        startScan(true);
    }

    public void startScan(boolean newDevices) {
        // TODO: For apps targeting Build.VERSION_CODES#S or or higher, this requires the Manifest.permission#BLUETOOTH_SCAN permission which can be gained with Activity.requestPermissions(String[], int).
        scanned.clear();
        BluetoothAdapter adapter = getAdapter();
        if (BuildConfig.DEBUG) {
            Log.d(L,"startLeScan, thread="+Thread.currentThread().getName()+", adapter="+(adapter==null?"null":"not null"));
        }
        if (adapter == null || adapter.getBluetoothLeScanner() == null) return;
        if (isScanning(adapter)) return;
        // replaced with startScan method because startLeScan is deprecated in API 21. startScan also only scans LE devices. https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner#startScan(java.util.List%3Candroid.bluetooth.le.ScanFilter%3E,%20android.bluetooth.le.ScanSettings,%20android.bluetooth.le.ScanCallback)
        ScanSettings.Builder scanSettings = new ScanSettings.Builder();
        if(!newDevices) {
            List<ScanFilter> scanFilters = new ArrayList<>();
            for(String device_id : devices_ids){
                ScanFilter scanFilter = new ScanFilter.Builder().setDeviceAddress(device_id).build();
                scanFilters.add(scanFilter);
            }
            scanSettings.setScanMode(SCAN_MODE_BALANCED); // or SCAN_MODE_LOW_POWER to consume less power
            scanSettings.setReportDelay(500);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                scanSettings.setMatchMode(MATCH_MODE_AGGRESSIVE); // In Aggressive mode, hw will determine a match sooner even with feeble signal strength and few number of sightings/match in a duration.
            }
            adapter.getBluetoothLeScanner().startScan(scanFilters, scanSettings.build(), scanCallback);
        } else {
            scanSettings.setScanMode(SCAN_MODE_LOW_LATENCY);
            scanSettings.setReportDelay(0);
            Log.d("ingo", "je adapter nula? " + (adapter.getBluetoothLeScanner() == null));
            adapter.getBluetoothLeScanner().startScan(new ArrayList<>(), scanSettings.build(), scanCallback);
        }
        isScanning = true;
    }

    public void stopScan() {
        scanned.clear();
        BluetoothAdapter adapter = getAdapter();
        if (BuildConfig.DEBUG) {
            Log.d(L,"stopLeScan, thread="+Thread.currentThread().getName()+", adapter="+(adapter==null?"null":"not null")+" isCanning="+isScanning(adapter));
        }
        if (isScanning(adapter)) {
            try {
                adapter.getBluetoothLeScanner().stopScan(scanCallback);
            }catch (NullPointerException ignored) {

            }
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

    @Override
    public void postOperation(Runnable runnable) {
        operationsHandler.post(runnable);
    }

    @Override
    public void postOperation(Runnable runnable, long delay) {
        operationsHandler.postDelayed(runnable, delay);
    }

    @Override
    public void cancelOperation(Runnable runnable) {
        operationsHandler.removeCallbacks(runnable);
    }

    @Override
    public void close() {
        operationsThread.quit();
    }
}
