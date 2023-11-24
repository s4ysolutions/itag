package s4y.itag.history;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import s4y.gps.sdk.GPSCurrentPositionManager;
import s4y.gps.sdk.android.GPSPermissionManager;
import s4y.gps.sdk.android.implementation.FusedGPSCurrentPositionProvider;
import s4y.itag.BuildConfig;
import s4y.itag.ITagApplication;
import s4y.itag.R;
import s4y.itag.ble.BLEConnectionInterface;
import s4y.itag.itag.ITag;


public final class HistoryRecord implements Serializable {
    private static final long serialVersionUID = 1845673754412L;
    private static final String LT = HistoryRecord.class.getName();

    public final String addr;
    public final Double latitude;
    public final Double longitude;
    public final long ts;

    public interface HistoryRecordListener {
        void onHistoryRecordChange();
    }

    private final static List<HistoryRecordListener> mListeners = new ArrayList<>(4);

    public static void addListener(HistoryRecordListener listener) {
        if (BuildConfig.DEBUG) {
            if (mListeners.contains(listener)) {
                ITagApplication.handleError(new Exception("HistoryRecord.addListener duplcate"));
            }
        }
        mListeners.add(listener);
    }

    public static void removeListener(HistoryRecordListener listener) {
        if (BuildConfig.DEBUG) {
            if (!mListeners.contains(listener)) {
                ITagApplication.handleError(new Exception("HistoryRecord.removeListener non existing"));
            }
        }
        mListeners.remove(listener);
    }

    private static void notifyChange() {
        for (HistoryRecordListener listener : mListeners) {
            listener.onHistoryRecordChange();
        }
    }

    private static final Map<String, LocationListener> sLocationListeners = new HashMap<>(4);

    private static final String DB_FILE_NAME = "dbh1";
    private static Map<String, HistoryRecord> records = null;

    @NonNull
    static private File getDbFile(
            @NonNull final Context context) throws IOException {
        File file = new File(context.getFilesDir(), HistoryRecord.DB_FILE_NAME);
        //noinspection ResultOfMethodCallIgnored
        file.createNewFile();
        return file;
    }

    private static void checkRecords(Context context) {
        if (records == null) {
            records = new HashMap<>(4);
            load(context);
        }
    }

    private static void save(Context context) {
        if (records == null || records.size() == 0) {
            try {
                //noinspection ResultOfMethodCallIgnored
                getDbFile(context).delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(getDbFile(context))) {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            List<HistoryRecord> rl = new ArrayList<>(records.size());
            rl.addAll(records.values());
            oos.writeObject(rl);
            oos.close();
        } catch (IOException e) {
            ITagApplication.handleError(e, true);
            e.printStackTrace();
        }
    }

    private static void add(Context context, HistoryRecord historyRecord) {
        if (historyRecord == null || historyRecord.addr == null) return;

        checkRecords(context);

        records.put(historyRecord.addr, historyRecord);
        save(context);
        notifyChange();
    }

    private HistoryRecord(String addr, Location location, long ts) {
        this.addr = addr;
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.ts = ts;
    }

    public static void add(final Context context, String id) {

        final LocationManager locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null) return;

        boolean gpsPermitted = ActivityCompat
                .checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED;
        boolean isGPSEnabled = gpsPermitted && locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        boolean isNetworkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);


        Location location = null;
        boolean gotBestLocation = false;
        if (isGPSEnabled) {
            try {
                location = locationManager
                        .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    if (BuildConfig.DEBUG) {
                        Log.d(LT, "getLastKnownLocation from GPS in " + (System.currentTimeMillis() - location.getTime()) / 1000 + "sec. id:" + id);
                    }
                    add(context, new HistoryRecord(id, location, System.currentTimeMillis()));
                    gotBestLocation = System.currentTimeMillis() - location.getTime() > 30000;
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(LT, "can't getLastKnownLocation from GPS. id:" + id);
                    }
                }
            } catch (SecurityException e) {
                ITagApplication.handleError(e);
            }
        }

        if (!gotBestLocation && isNetworkEnabled) {
            try {
                Location networklocation = locationManager
                        .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (networklocation != null) {
                    if (location == null || location.getTime() < System.currentTimeMillis() - 30000)
                        add(context, new HistoryRecord(id, networklocation, System.currentTimeMillis()));
                }
                if (BuildConfig.DEBUG) {
                    if (location == null) {
                        Log.d(LT, "can't getLastKnownLocation from Networkd. id:" + id);
                    } else {
                        Log.d(LT, "getLastKnownLocation from Network in " + (System.currentTimeMillis() - location.getTime()) / 1000 + "sec. id:" + id);
                    }
                }
            } catch (SecurityException e) {
                ITagApplication.handleError(e);
            }
        }

        if (isGPSEnabled && !sLocationListeners.containsKey(id)) {
            LocationListener locationListener =
                    new HistoryLocationListener(context, locationManager, id);

            try {
                Log.d(LT, "will request GPS location id:" + id);
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 1, 1, locationListener, Looper.getMainLooper());
                if (BuildConfig.DEBUG) Log.d(LT, "GPS requestLocationUpdates " + id);
                sLocationListeners.put(id, locationListener);
                ITagApplication.faIssuedGpsRequest();
            } catch (SecurityException e) {
                ITagApplication.handleError(e, R.string.can_not_get_gps_location);
                ITagApplication.faGpsPermissionError();
            } catch (Exception e) {
                ITagApplication.handleError(e, true);
                ITagApplication.faGpsPermissionError();
            }
        }
    }

    private static class HistoryLocationListener implements LocationListener {
        private final Context context;
        private final LocationManager locationManager;
        private final String addr;
        private final Location[] locations = {null, null, null};
        private int count = 0;
        private final long ts;

        public HistoryLocationListener(Context context, LocationManager locationManager, String addr) {
            this.context = context;
            this.locationManager = locationManager;
            this.addr = addr;
            this.ts = System.currentTimeMillis();
        }

        @Override
        public void onLocationChanged(@NonNull Location location) {
            boolean isListening = sLocationListeners.containsKey(addr);
            ITagApplication.faRemovedGpsRequestBySuccess();
            if (BuildConfig.DEBUG)
                Log.d(LT, "GPS onLocationChanged. id:" + addr + ", listening: " + isListening);
            BLEConnectionInterface connection = ITag.ble.connectionById(addr);
            if (isListening && !connection.isConnected()) {
                if (count > 1) { // skip first locations - might be a spike
                    if (count == locations.length + 2) {
                        if (BuildConfig.DEBUG)
                            Log.d(LT, "GPS onLocationChanged adds history record. id:" + addr);
                        Location avg = new Location("Lost location");
                        double lat = 0;
                        double lon = 0;
                        for (Location value : locations) {
                            lat += value.getLatitude();
                            lon += value.getLongitude();
                        }
                        avg.setLatitude(lat / locations.length);
                        avg.setLongitude(lon / locations.length);
                        add(context, new HistoryRecord(addr, avg, ts));

                        sLocationListeners.remove(addr);
                        locationManager.removeUpdates(this);
                    } else {
                        locations[count - 2] = location;
                    }
                }
                count++;
            }
            ITagApplication.faGotGpsLocation();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {

        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {

        }
    }

    public static void clear(Context context, String addr) {
        if (BuildConfig.DEBUG) Log.d(LT, "clear history" + addr);
        checkRecords(context);
        records.remove(addr);
        save(context);
        LocationListener locationListener = sLocationListeners.get(addr);
        sLocationListeners.remove(addr);
        notifyChange();
        if (locationListener != null) {
            final LocationManager locationManager = (LocationManager) context
                    .getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                if (BuildConfig.DEBUG) Log.d(LT, "GPS removeUpdates on clear history" + addr);
                locationManager.removeUpdates(locationListener);
            }
            ITagApplication.faRemovedGpsRequestByConnect();
        }

    }

    private static void load(Context context) {
        File file;

        try {
            file = getDbFile(context);
        } catch (IOException e) {
            ITagApplication.handleError(e, true);
            e.printStackTrace();
            return;
        }

        long l = file.length();
        if (l == 0)
            return;

        try (FileInputStream fis = new FileInputStream(file)) {
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object read = ois.readObject();

            if (read instanceof List) {
                //noinspection rawtypes
                List rr = (List) read;
                records.clear();
                for (Object r : rr) {
                    if (r instanceof HistoryRecord) {
                        records.put(((HistoryRecord) r).addr, (HistoryRecord) r);
                    }
                }
            }
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            ITagApplication.handleError(e, true);
            e.printStackTrace();
        }
    }

    static public Map<String, HistoryRecord> getHistoryRecords(@NonNull Context context) {
        checkRecords(context);
        return records;
    }
}
