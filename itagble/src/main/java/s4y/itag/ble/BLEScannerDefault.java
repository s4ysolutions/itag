package s4y.itag.ble;

import android.os.Handler;
import android.os.Looper;

import solutions.s4y.rasat.Channel;
import solutions.s4y.rasat.ChannelDistinct;
import solutions.s4y.rasat.DisposableBag;
import solutions.s4y.rasat.Observable;

class BLEScannerDefault implements BLEScannerInterface {
    private final BLECentralManagerInterface manager;
    private final Channel<Integer> channelTimer = new Channel<>(0);
    private final ChannelDistinct<Boolean> channelActive = new ChannelDistinct<>(false);
    private final Channel<BLEScanResult> channelScan = new Channel<>();
    // private final List<BLEDiscoveryResult> resultList = new ArrayList<>();
    private final Handler handlerTimer = new Handler(Looper.getMainLooper());
    private final Runnable runnableTimer = new Runnable() {
        @Override
        public void run() {
            int timeout = channelTimer.observable.value() - 1;
            channelTimer.broadcast(timeout);
            if (timeout < 0) {
                    stop();
            } else {
                handlerTimer.postDelayed(this, 1000);
            }
        }
    };

    BLEScannerDefault(BLECentralManagerInterface manager) {
        this.manager = manager;
    }

    private final Boolean[] isScanning = new Boolean[]{false};

    @Override
    public boolean isScanning() {
        synchronized (isScanning) {
            return isScanning[0];
        }
    }

    private void setScanning(boolean scanning) {
        synchronized (isScanning) {
            isScanning[0] = scanning;
        }
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
    public void start(boolean newDevices, int timeout, String[] forceCancelIds) {
        stop();
        if (!manager.canScan())
            return;
        disposableBag.add(
                manager.observables().observablePeripheralDiscovered().subscribe(
                        event -> channelScan.broadcast(new BLEScanResult(event.peripheral.address(), event.peripheral.name(), event.rssi))
                ));
        setScanning(true);
        manager.startScan(true);
        if(timeout != 0) {
            channelTimer.broadcast(timeout);
            handlerTimer.postDelayed(runnableTimer, 1000);
        }
        channelActive.broadcast(true);
    }

    @Override
    public void stop() {
        handlerTimer.removeCallbacks(runnableTimer);
        manager.stopScan();
        //   resultList.clear();
        disposableBag.dispose();
        setScanning(false);
        channelActive.broadcast(false);
    }
}
