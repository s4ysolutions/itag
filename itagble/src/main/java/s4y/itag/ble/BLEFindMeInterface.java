package s4y.itag.ble;

import s4y.rasat.android.Channel;

public interface BLEFindMeInterface {
    Channel<FindMe> observable();
    boolean isFindMe(String id);
    void cancel(String id);
}
