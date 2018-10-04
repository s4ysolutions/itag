package solutions.s4y.itag.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.annotations.CheckReturnValue;
import io.reactivex.annotations.SchedulerSupport;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.plugins.RxJavaPlugins;

public class LeScanObservable extends Observable<LeScanResult> {
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.IO)
    public static LeScanObservable observable(final BluetoothAdapter bluetoothAdapter, final int secTimeout) {
        return (LeScanObservable) RxJavaPlugins.onAssembly(
                new LeScanObservable(bluetoothAdapter, secTimeout));
    }

    private final int mSecTimeout;
    private final BluetoothAdapter mBluetoothAdapter;

    private LeScanObservable(final BluetoothAdapter bluetoothAdapter, final int secTimeout) {
        mSecTimeout = secTimeout;
        mBluetoothAdapter = bluetoothAdapter;
    }


    @Override
    protected void subscribeActual(Observer<? super LeScanResult> observer) {
        InternalDisposable disposable = new InternalDisposable(observer);
        observer.onSubscribe(disposable);
        disposable.run();
    }

    final class InternalDisposable extends AtomicReference<Disposable>
            implements Disposable, Runnable {
        private boolean mScanning;
        private final Observer<? super LeScanResult> mObserver;

        InternalDisposable(Observer<? super LeScanResult> mObserver) {
            this.mObserver = mObserver;
        }

        @Override
        public void dispose() {
            if (!isDisposed()) {
                if (mScanning) {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mObserver.onComplete();
                }
            }
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return get() == DisposableHelper.DISPOSED;
        }

        private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                mObserver.onNext(new LeScanResult(device, rssi, scanRecord));
            }
        };

        @Override
        public void run() {
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mObserver.onComplete();
            }, mSecTimeout * 1000);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
    }
}
