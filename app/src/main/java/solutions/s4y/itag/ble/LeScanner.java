package solutions.s4y.itag.ble;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;
import solutions.s4y.itag.ITagApplication;

public final class LeScanner {
    static public boolean isScanning;
    static public final List<LeScanResult> results = new ArrayList<>(4);

    private static CompositeDisposable disposable;

    static public final PublishSubject<Class<LeScanner>> subject = PublishSubject.create();

    static public void startScan(final BluetoothAdapter bluetoothAdapter) {
        results.clear();
        isScanning = true;
        subject.onNext(LeScanner.class);
        if (disposable == null) {
            disposable = new CompositeDisposable();
            disposable.add(LeScanObservable
                    .observable(bluetoothAdapter, 30)
                    .subscribe(
                            result->{
                                if (result.device.getAddress()==null) return;
                                String name=result.device.getAddress();
                                if (name==null) return;
                                for (LeScanResult r: results) {
                                    if (name.equals(r.device.getAddress())) return;
                                }
                                results.add(result);
                                subject.onNext(LeScanner.class);
                            },
                            err->{
                                ITagApplication.errorNotifier.onNext(err);
                                stopScan();
                            },
                            LeScanner::stopScan
                    )
            );
        }
    }

    static public void stopScan(){
        if (disposable!=null) {
            disposable.dispose();
            disposable=null;
        }
        isScanning = false;
        subject.onNext(LeScanner.class);
    }
}
