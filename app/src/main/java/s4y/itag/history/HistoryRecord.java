package s4y.itag.history;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import s4y.itag.BuildConfig;
import s4y.itag.ITagApplication;
import s4y.itag.R;
import s4y.itag.ble.ITagGatt;


public final class HistoryRecord implements Serializable {
    private static final long serialVersionUID = 1845673754412L;
    private static final String LT = HistoryRecord.class.getName();
    private static final Handler mGpsTimeoutHandler = new Handler(Looper.getMainLooper());
    private static final long LOCATION_TIMEOUT = 180000;

    public String addr;
    public Double latitude;
    public Double longitude;
    public long ts;

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

    private HistoryRecord(String addr, Location location) {
        this.addr = addr;
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.ts = location.getTime();
    }

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
        if (records == null || records.size() <= 0) {
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
        } catch (FileNotFoundException e) {
            ITagApplication.handleError(e, true);
            e.printStackTrace();
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

    private static Map<String, LocationListener> sLocationListeners = new HashMap<>(4);

    private static class HistoryLocationListener implements LocationListener {
        private final Context context;
        private final LocationManager locationManager;
        private final String addr;
        private final ITagGatt gatt;
        private final long ts;

        public HistoryLocationListener(Context context, LocationManager locationManager, String addr, ITagGatt gatt) {
            this.context = context;
            this.locationManager = locationManager;
            this.addr = addr;
            this.gatt = gatt;
            this.ts = System.currentTimeMillis();
        }

        @Override
        public void onLocationChanged(Location location) {
            boolean isListening = sLocationListeners.containsKey(addr);
            locationManager.removeUpdates(this);
            ITagApplication.faRemovedGpsRequestBySuccess();
            if (BuildConfig.DEBUG)
                Log.d(LT, "GPS removeUpdates on location changed" + addr);
            if (isListening && !gatt.isConnected() && location.getTime() < ts + LOCATION_TIMEOUT)
                add(context, new HistoryRecord(addr, location));
            ITagApplication.faGotGpsLocation();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    public static void add(final Context context, final ITagGatt gatt) {
        if (gatt == null)
            return;

        final String addr = gatt.mAddr;
        final LocationManager locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null) return;

        boolean isGPSEnabled = locationManager
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
                    add(context, new HistoryRecord(addr, location));
                    gotBestLocation = location.getTime() > System.currentTimeMillis() - 5000;
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
                    if (location == null || location.getTime() < System.currentTimeMillis() - 120000)
                        add(context, new HistoryRecord(addr, networklocation));
                }
            } catch (SecurityException e) {
                ITagApplication.handleError(e);
            }
        }

        if (isGPSEnabled && !sLocationListeners.containsKey(addr) && !gotBestLocation) {
            LocationListener locationListener =
                    new HistoryLocationListener(context, locationManager, addr, gatt);

            try {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 1, 1, locationListener, Looper.getMainLooper());
                if (BuildConfig.DEBUG) Log.d(LT, "GPS requestLocationUpdates " + addr);
                sLocationListeners.put(addr, locationListener);
                mGpsTimeoutHandler.postDelayed(() -> {
                    sLocationListeners.remove(addr);
                    locationManager.removeUpdates(locationListener);
                    ITagApplication.faRemovedGpsRequestByTimeout();
                }, LOCATION_TIMEOUT);

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

    public static void clear(Context context, String addr) {
        records.remove(addr);
        save(context);
        LocationListener locationListener = sLocationListeners.get(addr);
        sLocationListeners.remove(addr);
        notifyChange();
        if (locationListener != null) {
            final LocationManager locationManager = (LocationManager) context
                    .getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(locationListener);
            ITagApplication.faRemovedGpsRequestByConnect();
            if (BuildConfig.DEBUG) Log.d(LT, "GPS removeUpdates on clear history" + addr);
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
                List rr = (List) read;
                records.clear();
                for (Object r : rr) {
                    if (r instanceof HistoryRecord) {
                        records.put(((HistoryRecord) r).addr, (HistoryRecord) r);
                    }
                }
            }
            ois.close();
        } catch (FileNotFoundException e) {
            ITagApplication.handleError(e, true);
            e.printStackTrace();
        } catch (IOException e) {
            ITagApplication.handleError(e, true);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            ITagApplication.handleError(e, true);
            e.printStackTrace();
        }
    }

    static public Map<String, HistoryRecord> getHistoryRecords(@NonNull Context context) {
        checkRecords(context);
        return records;
    }
}
