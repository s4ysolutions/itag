package solutions.s4y.waytoday.locations;

import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import androidx.annotation.NonNull;
import solutions.s4y.waytoday.BuildConfig;
import solutions.s4y.waytoday.R;
import solutions.s4y.waytoday.errors.ErrorsObservable;

public class LocationsGPSUpdater implements LocationsUpdater {
    private static final String LT = LocationsTracker.class.getSimpleName();
    private LocationManager mLocationManager;

    public LocationsGPSUpdater(@NonNull Context context) {
        mLocationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void requestLocationUpdates(
            @NonNull LocationListener locationListener,
            @NonNull RequestUpdatesListener requestUpdatesListener,
            int frequency
    ) {
        if (mLocationManager == null) {
            return;
        }
        if (BuildConfig.DEBUG) {
            Log.d(LT, "requestLocationUpdates " + frequency + " ms");
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    frequency,
                    1,
                    locationListener
            );
            requestUpdatesListener.onRequestResult(true);
        } catch (IllegalArgumentException e) {
            requestUpdatesListener.onRequestResult(false);
            ErrorsObservable.toast(e);
        } catch (SecurityException e) {
            requestUpdatesListener.onRequestResult(false);
            ErrorsObservable.notify(e, true);
        }
    }

    @Override
    public void cancelLocationUpdates(@NonNull LocationListener listener) {
        if (mLocationManager == null) {
            ErrorsObservable.toast(R.string.no_location_manager);
            return;
        }
        mLocationManager.removeUpdates(listener);
    }

}
