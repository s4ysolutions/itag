package s4y.itag.ble;

import s4y.observables.Observable;

public interface BLEFindMeInterface {
    Observable<FindMe> observable();
    boolean isFindMe(String id);
    void cancel(String id);
}
