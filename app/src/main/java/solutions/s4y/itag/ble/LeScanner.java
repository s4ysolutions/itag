package solutions.s4y.itag.ble;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import solutions.s4y.itag.ITagApplication;
import solutions.s4y.itag.MainActivity;
import solutions.s4y.itag.R;

public final class LeScanner {
    static public int TIMEOUT = 30;
    static public boolean isScanning;
    static public int tick;
    static public final List<LeScanResult> results = new ArrayList<>(4);

    private static CompositeDisposable disposable;

    static public final PublishSubject<Class<LeScanner>> subject = PublishSubject.create();
    static public final PublishSubject<Class<LeScanner>> subjectTimer = PublishSubject.create();

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
        results.clear();
        isScanning = true;
        subject.onNext(LeScanner.class);
        if (disposable == null) {
            tick = 0;
            disposable = new CompositeDisposable();

            final Disposable timer = Observable
                    .interval(1, TimeUnit.SECONDS)
                    .subscribe(ts -> {
                        tick++;
                        subjectTimer.onNext(LeScanner.class);
                    });

            disposable.add(LeScanObservable
                    .observable(bluetoothAdapter, TIMEOUT)
                    .filter(result -> !Db.has(result.device))
                    .subscribe(
                            result -> {
                                if (result.device.getAddress() == null) return;
                                String name = result.device.getAddress();
                                if (name == null) return;
                                for (LeScanResult r : results) {
                                    if (name.equals(r.device.getAddress())) return;
                                }
                                results.add(result);
                                subject.onNext(LeScanner.class);
                            },
                            err -> {
                                timer.dispose();
                                ITagApplication.errorNotifier.onNext(err);
                                stopScan();
                            },
                            () -> {
                                timer.dispose();
                                stopScan();
                            }
                    )
            );
            disposable.add(timer);
        }
    }

    static public void stopScan() {
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
        isScanning = false;
        subject.onNext(LeScanner.class);
    }
}
