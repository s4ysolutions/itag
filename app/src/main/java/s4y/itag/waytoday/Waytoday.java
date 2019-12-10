package s4y.itag.waytoday;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import s4y.itag.BuildConfig;
import s4y.itag.ITagApplication;
import s4y.waytoday.idservice.IDService;
import s4y.waytoday.locations.LocationsGPSUpdater;
import s4y.waytoday.locations.LocationsTracker;
import s4y.waytoday.locations.LocationsUpdater;
import s4y.waytoday.upload.UploadJobService;

public class Waytoday {
    private static final String LT=Waytoday.class.getName();
    //private static final HandlerThread thread;
    private static final Handler handler;
    private static LocationsUpdater gpsLocatonUpdater;

    static {
     //   thread = new HandlerThread("WayToday");
      //  thread.start();
        handler = new Handler(Looper.getMainLooper());
    }

    private static LocationsTracker.ILocationListener locationListener =
            location -> {
                if (BuildConfig.DEBUG) Log.d(LT, "location=" + location);
                UploadJobService.enqueueUploadLocation(ITagApplication.context, location);
            };

    private static IDService.IIDSeriviceListener seriviceListener = trackID -> {
        if (BuildConfig.DEBUG) Log.d(LT, "trackID=" + trackID);
        if ("".equals(trackID))
            return;

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ITagApplication.context);
        sp.edit().putString("tid", trackID).apply();
        sp.edit().putBoolean("wtfirst", false).apply();
    };


    public static void init() {
        Context context = ITagApplication.context;

        gpsLocatonUpdater = new LocationsGPSUpdater(context);
        LocationsTracker.addOnLocationListener(locationListener);
        IDService.addOnTrackIDChangeListener(seriviceListener);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean wt = preferences.getBoolean("wt", false);
        int freq = preferences.getInt("freq", 0);

        if (BuildConfig.DEBUG) Log.d(LT, "init wt=" + wt+ " freq="+freq);

        if (wt && freq > 0) {
            start(freq);
        }
    }

    public static void done() {
        LocationsTracker.removeOnLocationListener(locationListener);
        LocationsTracker.stop();
    //    thread.quit();
    }

    public static void start(int frequency) {
        Context context = ITagApplication.context;
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean("wt", true).apply();
        preferences.edit().putInt("freq", frequency).apply();
        final String tid = preferences.getString("tid", null);
        if (BuildConfig.DEBUG) Log.d(LT, "start tid=" + tid+ " freq="+frequency);
        if (tid == null) {
            ITagApplication.faWtNoTrackID();
            if (BuildConfig.DEBUG) Log.d(LT, "will request tid");
            final IDService.IIDSeriviceListener listener = new IDService.IIDSeriviceListener() {
                @Override
                public void onTrackID(@NonNull String trackID) {
                    IDService.removeOnTrackIDChangeListener(this);
                    if (BuildConfig.DEBUG) Log.d(LT, "got new tid="+trackID);
                    if (!"".equals(trackID)) {
                        // duplicate of ITagsService.onTrackID
                        preferences.edit().putString("tid", trackID).apply();
                        if (BuildConfig.DEBUG) Log.d(LT, "will request start after new tid");
                        handler.post(() ->
                                LocationsTracker.requestStart(gpsLocatonUpdater, frequency));
                    }
                }
            };
            IDService.addOnTrackIDChangeListener(listener);
            IDService.enqueueRetrieveId(context, "");
        } else {
            if (BuildConfig.DEBUG) Log.d(LT, "will request start immediately");
            LocationsTracker.requestStart(gpsLocatonUpdater, frequency);
        }
    }

    public static void stop() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ITagApplication.context);
        preferences.edit().putBoolean("wt", false).apply();
        LocationsTracker.stop();
    }
}
