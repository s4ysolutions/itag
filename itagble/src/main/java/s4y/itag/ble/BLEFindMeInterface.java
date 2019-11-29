package s4y.itag.ble;

import s4y.rasat.Channel;

public interface BLEFindMeInterface {
    Channel<FindMe> observable();
    boolean isFindMe(String id);
    void cancel(String id);
}
