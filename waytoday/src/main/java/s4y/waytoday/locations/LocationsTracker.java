package s4y.waytoday.locations;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

import mad.location.manager.lib.Commons.Coordinates;
import mad.location.manager.lib.Commons.GeoPoint;
import mad.location.manager.lib.Commons.Utils;
import mad.location.manager.lib.Filters.GPSAccKalmanFilter;
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
    public static final DataItemAcc zero = new DataItemAcc(0, 0, 0);
    private static GPSAccKalmanFilter mKalmanFilter;
    private static FilterSettings filterSettings = FilterSettings.defaultSettings;
    private static DataItemGPS lastDataItemGPS = new DataItemGPS(null);
    private static float minDistance = 1;

    static private final LocationListener locationListener = new LocationListener() {

        double lastGPSTimeStamp = 0;
        double prevLat = 0;
        double prevLon = 0;

        private void handlePredict(DataItemAcc acc) {
            DataItemGPS gps = lastDataItemGPS;
            if (gps.location == null) {
                if (BuildConfig.DEBUG) {
                    Log.d(LT, "kalmanFilter.predict (no location): east=" + acc.absEastAcc + " north=" + acc.absNorthAcc);
                }
                mKalmanFilter.predict(acc.getTimestamp(), acc.absEastAcc, acc.absNorthAcc);
            } else {
                double declination = gps.getDeclination();
                if (BuildConfig.DEBUG) {
                    Log.d(LT, "kalmanFilter.predict: (location): east=" + acc.getAbsEastAcc(declination) + " north=" + acc.getAbsNorthAcc(declination) + " decl=" + declination);
                }
                mKalmanFilter.predict(acc.getTimestamp(), acc.getAbsEastAcc(declination), acc.getAbsNorthAcc(declination));
            }
        }

        private void handleUpdate(DataItemGPS gps, Location location) {
            double xVel = location.getSpeed() * Math.cos(location.getBearing());
            double yVel = location.getSpeed() * Math.sin(location.getBearing());

            if (BuildConfig.DEBUG) {
                if (gps.location != null) {
                    Log.d(LT, "kalmanFilter.update: lon=" + gps.location.getLongitude() + " lat=" + gps.location.getLatitude() + " xVel=" + xVel + " yVel=" + yVel);
                }
            }

            mKalmanFilter.update(
                    gps.getTimestamp(),
                    Coordinates.longitudeToMeters(location.getLongitude()),
                    Coordinates.latitudeToMeters(location.getLatitude()),
                    xVel,
                    yVel,
                    location.getAccuracy(),
                    gps.getVelErr()
            );
        }

        private Location locationAfterUpdateStep(Location location) {
            double xVel, yVel;
            Location loc = new Location("WayTodayAnroid");
            GeoPoint pp = Coordinates.metersToGeoPoint(mKalmanFilter.getCurrentX(),
                    mKalmanFilter.getCurrentY());
            loc.setLatitude(pp.Latitude);
            loc.setLongitude(pp.Longitude);
            loc.setAltitude(location.getAltitude());
            xVel = mKalmanFilter.getCurrentXVel();
            yVel = mKalmanFilter.getCurrentYVel();
            double speed = Math.sqrt(xVel * xVel + yVel * yVel); //scalar speed without bearing
            loc.setBearing(location.getBearing());
            loc.setSpeed((float) speed);
            loc.setTime(System.currentTimeMillis());
            loc.setElapsedRealtimeNanos(System.nanoTime());
            loc.setAccuracy(location.getAccuracy());

            if (BuildConfig.DEBUG) {
                Log.d(LT, "locationAfterUpdateStep: " + loc.getLongitude() + "," + loc.getLatitude());
            }

            return loc;
        }

        @Override
        public void onLocationChanged(Location originalLocation) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onLocationChanged");
            }
            lastDataItemGPS = new DataItemGPS(originalLocation);
            if (mKalmanFilter == null) {
                Location location = originalLocation;
                double x, y, xVel, yVel, posDev, course, speed;
                long timeStamp;
                speed = location.getSpeed();
                course = location.getBearing();
                x = location.getLongitude();
                y = location.getLatitude();
                xVel = speed * Math.cos(course);
                yVel = speed * Math.sin(course);
                posDev = location.getAccuracy();
                timeStamp = Utils.nano2milli(location.getElapsedRealtimeNanos());
                mKalmanFilter = new GPSAccKalmanFilter(
                        false, //todo move to settings
                        Coordinates.longitudeToMeters(x),
                        Coordinates.latitudeToMeters(y),
                        xVel,
                        yVel,
                        filterSettings.accelerationDeviation,
                        posDev,
                        timeStamp,
                        filterSettings.mVelFactor,
                        filterSettings.mPosFactor);
                handlePredict(zero);
            }
            double ts = lastDataItemGPS.getTimestamp();
            if (ts < lastGPSTimeStamp) {
                return;
            }
            lastGPSTimeStamp = ts;

            handleUpdate(lastDataItemGPS, lastDataItemGPS.location);
            Location location = locationAfterUpdateStep(lastDataItemGPS.location);

            double lat = location.getLatitude();
            double lon = location.getLongitude();

            if (BuildConfig.DEBUG) {
                Log.d(LT, "Request to publish location " + lon + "," + lat);
            }

            if (lat == 0 || lon == 0) {
                return;
            }

            if (Math.abs(lat - prevLat) < minDistance && Math.abs(lon - prevLon) < minDistance) {
                if (BuildConfig.DEBUG) {
                    Log.d(LT, "Skip upload because too close");
                }
                return;
            }

            prevLon = lon;
            prevLat = lat;
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
        minDistance = frequency < 5000 ? 0 : frequency < 15000 ? 0.0002f : 0.0005f;

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
        mKalmanFilter = null;
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
