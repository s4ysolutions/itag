package solutions.s4y.itag.ble;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import solutions.s4y.itag.BuildConfig;
import solutions.s4y.itag.ITagApplication;
import solutions.s4y.itag.MainActivity;
import solutions.s4y.itag.R;

public final class LeScanner {
    static public int TIMEOUT = 30;
    static public boolean isScanning;
    static public int tick;
    static public final List<LeScanResult> results = new ArrayList<>(4);

    public interface LeScannerListener {
        void onStartScan();
        void onNewDeviceScanned(LeScanResult result);
        void onTick(int tick, int max);
        void onStopScan();
    }

    static final private List<LeScannerListener> mListeners=new ArrayList<>();

    static public void addListener(LeScannerListener listener) {
        if (BuildConfig.DEBUG) {
            if (mListeners.contains(listener)) {
                ITagApplication.handleError(new Exception("LeScanner.addListener duplicate listener"));
            }
        }
        mListeners.add(listener);
    }

    static public void removeListener(LeScannerListener listener) {
        if (BuildConfig.DEBUG) {
            if (!mListeners.contains(listener)) {
                ITagApplication.handleError(new Exception("LeScanner.removeListener non existing listener"));
            }
        }
        mListeners.add(listener);
    }

    static private void notifyStart() {
        for(LeScannerListener listener: mListeners) {
            listener.onStartScan();
        }
    }

    static private void notifyStop() {
        for(LeScannerListener listener: mListeners) {
            listener.onStopScan();
        }
    }

    static private void notifyTick() {
        for(LeScannerListener listener: mListeners) {
            listener.onTick(tick, TIMEOUT);
        }
    }

    static private void notifyNewDeviceScanned(LeScanResult result) {
        for(LeScannerListener listener: mListeners) {
            listener.onNewDeviceScanned(result);
        }
    }

    private static BluetoothAdapter.LeScanCallback sLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            LeScanResult result = new LeScanResult(device, rssi, scanRecord);
            if (Db.has(device)) return;
            if (result.device.getAddress() == null) return;
            String addr = result.device.getAddress();
            if (addr == null) return;
            LeScanResult existing=null;
            for(LeScanResult r: results) {
                if (addr.equals(r.device.getAddress())){
                    existing=r;
                    break;
                }
            }
            if (existing==null) {
                results.add(result);
            }else {
                existing.rssi=result.rssi;
            }

            notifyNewDeviceScanned(result);
        }
    };

    @TargetApi(Build.VERSION_CODES.M)
    static public void startScan(final BluetoothAdapter bluetoothAdapter, MainActivity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setMessage(R.string.request_location_permission)
                            .setTitle(R.string.request_permission_title)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> activity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MainActivity.REQUEST_ENABLE_LOCATION))
                            .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                            .show();
                    return;
                } else {
                    activity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MainActivity.REQUEST_ENABLE_LOCATION);
                    return;
                }
            }
        }
        tick=0;
        results.clear();
        isScanning = true;
        notifyStart();

        final Handler handler = new Handler();

        final Runnable run1sec = new Runnable() {
            @Override
            public void run() {
                if (isScanning) {
                    tick++;
                    notifyTick();
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.postDelayed(run1sec,1000);

        handler.postDelayed(() -> {
            stopScan(bluetoothAdapter);
        }, TIMEOUT * 1000);


        bluetoothAdapter.startLeScan(sLeScanCallback);
    }

    static public void stopScan(final BluetoothAdapter bluetoothAdapter) {
        bluetoothAdapter.stopLeScan(sLeScanCallback);
        isScanning = false;
        notifyStop();
    }
}
