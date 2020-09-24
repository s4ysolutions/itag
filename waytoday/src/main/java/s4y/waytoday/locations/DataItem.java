package s4y.waytoday.locations;

import androidx.annotation.NonNull;

import mad.location.manager.lib.Commons.Utils;

public class DataItem implements Comparable<DataItem> {
    public static final double NOT_INITIALIZED = 361.0;
    final double timestamp;

    public DataItem() {
        long now = android.os.SystemClock.elapsedRealtimeNanos();
        this.timestamp = Utils.nano2milli(now);
    }

    public double getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(@NonNull DataItem o) {
        return (int) (this.timestamp - o.timestamp);
    }
}
