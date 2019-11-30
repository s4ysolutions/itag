package s4y.itag.ble;

import java.util.HashSet;
import java.util.Set;

import s4y.rasat.DisposableBag;

class BLEAlertDefault implements BLEAlertInterface, AutoCloseable {
    private final BLEConnectionsStoreInterface store;
    private final Set<String> alerts = new HashSet<>();
    private final DisposableBag disposableBag = new DisposableBag();

    BLEAlertDefault(BLEConnectionsStoreInterface store) {
        this.store = store;
    }

    @Override
    public boolean isAlerting(String id) {
        return alerts.contains(id);
    }

    @Override
    public void toggleAlert(String id, int timeout) {
        if (isAlerting(id)) {
            stopAlert(id, timeout);
        } else {
            startAlert(id, timeout);
        }
    }

    @Override
    public void startAlert(String id, int timeout) {
        BLEConnectionInterface connection = store.getOrMake(id);
        try {
            BLEError error = connection.connect(timeout);
            if (error == BLEError.ok) {
                error = connection.writeImmediateAlert(AlertVolume.HIGH_ALERT, timeout);
                if (error == BLEError.ok) {
                    alerts.add(id);
                } else {
                    alerts.remove(id);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopAlert(String id, int timeout) {
        BLEConnectionInterface connection = store.getOrMake(id);
        try {
            BLEError error = connection.connect(timeout);
            if (error == BLEError.ok) {
                connection.writeImmediateAlert(AlertVolume.NO_ALERT, timeout);
                alerts.remove(id);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        disposableBag.dispose();
    }
}
