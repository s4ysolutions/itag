package s4y.itag.ble;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import s4y.rasat.Channel;
import s4y.rasat.ChannelDistinct;
import s4y.rasat.DisposableBag;
import s4y.rasat.Observable;

class BLEScannerDefault implements BLEScannerInterface {
    private final BLECentralManagerInterface manager;
    private final BLEConnectionsInterface connections;
    private final Channel<Integer> channelTimer = new Channel<>(0);
    private final Channel<Boolean> channelActive = new ChannelDistinct<>(false);
    private final Channel<BLEScanResult> channelScan = new Channel<>();
    // private final List<BLEDiscoveryResult> resultList = new ArrayList<>();
    private final Handler handlerTimer = new Handler(Looper.getMainLooper());
    private Handler handlerStart;
    private int timeout = 0;
    private final Runnable runnableTimer = new Runnable() {
        @Override
        public void run() {
            channelTimer.broadcast(timeout);
            timeout--;
            if (timeout < 0) {
                if (handlerStart == null) {
                    stop();
                } else {
                    handlerStart.post(() -> stop());
                }
            } else {
                handlerTimer.postDelayed(this, 1000);
            }
        }
    };

    BLEScannerDefault(BLEConnectionsInterface connections, BLECentralManagerInterface manager) {
        this.manager = manager;
        this.connections = connections;
    }

    @Override
    public boolean isScanning() {
        return manager.isScanning();
    }

    @Override
    public Observable<Integer> observableTimer() {
        return channelTimer.observable;
    }

    @Override
    public Observable<BLEScanResult> observableScan() {
        return channelScan.observable;
    }

    @Override
    public Observable<Boolean> observableActive() {
        return channelActive.observable;
    }

    private final DisposableBag disposableBag = new DisposableBag();

    @Override
    public void start(int timeout, String[] forceCancelIds) {
        Thread thread = Thread.currentThread();
        if (thread instanceof HandlerThread) {
            handlerStart = new Handler(((HandlerThread) Thread.currentThread()).getLooper());
        } else {
            handlerStart = null;
        }
        stop();
        if (!manager.canScan())
            return;
        //                        resultList.add(result);
        disposableBag.add(
                manager.observables().observablePeripheralDiscovered().subscribe(
                        event -> channelScan.broadcast(new BLEScanResult(event.peripheral.address(), event.peripheral.name(), event.rssi))
                ));
        for (String id : forceCancelIds) {
            connections.disconnect(id);
        }
        this.timeout = timeout;
        manager.scanForPeripherals();
        handlerTimer.post(runnableTimer);
        channelActive.broadcast(true);
    }

    @Override
    public void stop() {
        handlerTimer.removeCallbacks(runnableTimer);
        handlerStart = null;
        manager.stopScan();
     //   resultList.clear();
        disposableBag.dispose();
        channelActive.broadcast(false);
    }
}
