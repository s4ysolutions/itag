package s4y.waytoday.locations;

import android.location.LocationListener;

import androidx.annotation.NonNull;

public interface LocationsUpdater {
    void requestLocationUpdates(
            @NonNull LocationListener locationListener,
            @NonNull RequestUpdatesListener requestListener,
            int frequency);

    void cancelLocationUpdates(@NonNull LocationListener listener);
}
