package solutions.s4y.itag.history;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

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

import solutions.s4y.itag.BuildConfig;
import solutions.s4y.itag.ITagApplication;
import solutions.s4y.itag.R;
import solutions.s4y.itag.ble.ITagGatt;

public final class HistoryRecord implements Serializable {
    private static final long serialVersionUID = 1845673754412L;

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

    @NotNull
    static private File getDbFile(
            @NotNull final Context context) throws IOException {
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

    private static LocationListener sLocationListener;

    public static void add(final Context context, final ITagGatt gatt) {
        if (gatt == null)
            return;

        final String addr = gatt.mAddr;
        final LocationManager locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null) return;

        if (sLocationListener != null)
            locationManager.removeUpdates(sLocationListener);
        sLocationListener = null;

        boolean isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        boolean isNetworkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);


        Location location = null;
        if (isGPSEnabled || isNetworkEnabled) {
            try {
                location = locationManager
                        .getLastKnownLocation(isGPSEnabled ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER);
                if (location != null)
                    add(context, new HistoryRecord(addr, location));
            } catch (SecurityException e) {
                ITagApplication.handleError(e);
            }
        }

        if (isGPSEnabled && sLocationListener == null && (
                location == null ||
                        !LocationManager.GPS_PROVIDER.equals(location.getProvider()) ||
                        location.getTime()<System.currentTimeMillis()-1000
                        )
                )
        {
            sLocationListener = new LocationListener() {

                @Override
                public void onLocationChanged(Location location) {
                    if (sLocationListener != null ) {
                        locationManager.removeUpdates(sLocationListener);
                    }
                    sLocationListener = null;
                    if (location != null && !gatt.isConnected() )
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
            };
            try {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 1, 1, sLocationListener, Looper.getMainLooper());
                ITagApplication.faIssuedGpsRequest();
            } catch (SecurityException e) {
                ITagApplication.handleError(e,R.string.can_not_get_gps_location);
                sLocationListener=null;
                ITagApplication.faGpsPermissionError();
            } catch (Exception e){
                ITagApplication.handleError(e,true);
                sLocationListener=null;
                ITagApplication.faGpsPermissionError();
            }
        }
    }

    public static void clear(Context context, String addr) {
        records.remove(addr);
        save(context);
        notifyChange();
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
