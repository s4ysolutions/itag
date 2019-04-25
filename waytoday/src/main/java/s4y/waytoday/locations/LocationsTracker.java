package s4y.waytoday.locations;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import s4y.waytoday.BuildConfig;
import s4y.waytoday.grpc.LocationOuterClass;

public class LocationsTracker {
    private static final List<ILocationListener> mLocationListeners =
            new ArrayList<>(2);
    private static final String LT = LocationOuterClass.Location.class.getSimpleName();
    @VisibleForTesting
    private static final List<ITrackingStateListener> sStateListeners =
            new ArrayList<>(2);
    @SuppressWarnings("WeakerAccess")
    public static boolean isSuspended;
    static private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onLocationChanged");
            }
            notifyLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onProviderEnabled");
            }
            isSuspended = false;
            notifyStateChange();
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onProviderDisabled");
            }
            isSuspended = true;
            notifyStateChange();
        }
    };
    static private final RequestUpdatesListener requestListener = new RequestUpdatesListener() {
        @Override
        public void onRequestResult(boolean success) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onGPSPermissionRequest: " + success);
            }
            isSuspended = !success;
            notifyStateChange();
        }
    };
    private static LocationsUpdater updater;
    public static boolean isUpdating;

    public static void addOnLocationListener(ILocationListener listener) {
        mLocationListeners.add(listener);
    }

    private static void notifyLocation(@NonNull Location location) {
        for (ILocationListener listener : mLocationListeners) {
            listener.onLocation(location);
        }
    }

    public static void removeOnLocationListener(ILocationListener listener) {
        mLocationListeners.remove(listener);
    }

    public static void requestStart(@NonNull final LocationsUpdater updater, int frequency) {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "requestStart");
        }
        stop();
        LocationsTracker.updater = updater;
        isUpdating = true;
        isSuspended = false;
        notifyStateChange();
        updater.requestLocationUpdates(locationListener, requestListener, frequency);
    }

    public interface ILocationListener {
        void onLocation(@NonNull Location location);
    }

    public static void stop() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "stop");
        }
        if (updater != null) {
            updater.cancelLocationUpdates(locationListener);
            updater = null;
        }
        if (isUpdating) {
            isUpdating = false;
            notifyStateChange();
        }
    }

    public static void addOnTrackingStateListener(ITrackingStateListener listener) {
        sStateListeners.add(listener);
    }

    public static void removeOnTrackingStateListener(ITrackingStateListener listener) {
        sStateListeners.remove(listener);
    }

    private static void notifyStateChange() {
        TrackingState state = new TrackingState();
        for (ITrackingStateListener listener : sStateListeners) {
            listener.onStateChange(state);
        }
    }

    public interface ITrackingStateListener {
        void onStateChange(@NonNull TrackingState state);
    }

    static public class TrackingState {
        public final boolean isUpdating;
        public final boolean isSuspended;

        TrackingState() {
            this.isUpdating = LocationsTracker.isUpdating;
            this.isSuspended = LocationsTracker.isSuspended;
        }
    }
}
