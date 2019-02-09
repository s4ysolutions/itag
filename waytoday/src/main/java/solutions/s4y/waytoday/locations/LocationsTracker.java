package solutions.s4y.waytoday.locations;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import solutions.s4y.waytoday.BuildConfig;
import solutions.s4y.waytoday.errors.ErrorsObservable;
import solutions.s4y.waytoday.grpc.LocationOuterClass;

public class LocationsTracker {
    private static final List<ILocationListener> mLocationListeners =
            new ArrayList<>(2);
    private static final String LT = LocationOuterClass.Location.class.getSimpleName();
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
            ErrorsObservable.notify(
                    "LocationListener.onStatusChanged: " + status,
                    BuildConfig.DEBUG);
        }

        @Override
        public void onProviderEnabled(String provider) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onProviderEnabled");
            }
            isSuspended = false;
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onProviderDisabled");
            }
            isSuspended = true;
        }
    };
    static private final RequestUpdatesListener requestListener = new RequestUpdatesListener() {
        @Override
        public void onRequestResult(boolean success) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onRequestResult: " + success);
            }
            isSuspended = !success;
        }
    };
    public static boolean isUpdating;
    private static LocationsUpdater updater;

    public static void addOnITagChangeListener(ILocationListener listener) {
        mLocationListeners.add(listener);
    }

    public static void removeOnITagChangeListener(ILocationListener listener) {
        mLocationListeners.remove(listener);
    }

    private static void notifyLocation(@NonNull Location location) {
        for (ILocationListener listener : mLocationListeners) {
            listener.onLocation(location);
        }
    }

    public static void requestStart(@NonNull final LocationsUpdater updater, int frequency) {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "requestStart");
        }
        stop();
        LocationsTracker.updater = updater;
        isUpdating = true;
        isSuspended = false;
        updater.requestLocationUpdates(locationListener, requestListener, frequency);
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
        }
    }

    public interface ILocationListener {
        void onLocation(@NonNull Location location);
    }

    static public class TrackingState {
        final boolean isUpdating;
        final boolean isSuspended;

        TrackingState() {
            this.isUpdating = LocationsTracker.isUpdating;
            this.isSuspended = LocationsTracker.isSuspended;
        }
    }
}
