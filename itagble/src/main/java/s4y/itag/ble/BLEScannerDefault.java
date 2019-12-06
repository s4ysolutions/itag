package s4y.itag.ble;

import android.os.Handler;
import android.os.Looper;

import s4y.rasat.Channel;
import s4y.rasat.DisposableBag;
import s4y.rasat.Observable;

class BLEScannerDefault implements BLEScannerInterface {
    private final BLECentralManagerInterface manager;
    private final BLEConnectionsInterface connections;
    private final Channel<Integer> channelTimer = new Channel<>();
    private final Channel<Boolean> channelActive = new Channel<>();
    private final Channel<BLEScanResult> channelScan = new Channel<>();
    // private final List<BLEDiscoveryResult> resultList = new ArrayList<>();
    private final Handler handlerTimer = new Handler(Looper.getMainLooper());
    private int timeout = 0;
    private final Runnable runnableTimer = new Runnable() {
        @Override
        public void run() {
            timeout--;
            channelTimer.broadcast(timeout);
            if (timeout <= 0) {
                handlerTimer.removeCallbacks(this);
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
        stop();
        if (!manager.canScan())
            return;
        //                        resultList.add(result);
        disposableBag.add(
                manager.observables().observablePeripheralDiscovered().subscribe(
                        event -> channelScan.broadcast(new BLEScanResult(event.peripheral.address(), event.peripheral.name(), event.rssi))
                ));
        channelActive.broadcast(true);
        for (String id : forceCancelIds) {
            connections.disconnect(id);
        }
        this.timeout = timeout;
        manager.scanForPeripherals();
        handlerTimer.postDelayed(runnableTimer, 1000);
    }

    @Override
    public void stop() {
        handlerTimer.removeCallbacks(runnableTimer);
        manager.stopScan();
     //   resultList.clear();
        disposableBag.dispose();
        channelActive.broadcast(false);
    }
}
