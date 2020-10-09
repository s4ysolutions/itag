package s4y.itag.waytoday;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import s4y.itag.BuildConfig;
import s4y.itag.ITagApplication;
import solutions.s4y.waytoday.sdk.id.IDJobService;
import solutions.s4y.waytoday.sdk.locations.LocationsGPSUpdater;
import solutions.s4y.waytoday.sdk.locations.LocationsUpdater;
import solutions.s4y.waytoday.sdk.tracker.LocationTracker;
import solutions.s4y.waytoday.sdk.upload.UploadJobService;

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

    private static final LocationTracker.ILocationListener locationListener =
            location -> {
                if (BuildConfig.DEBUG) Log.d(LT, "location=" + location);
                UploadJobService.enqueueUploadLocation(ITagApplication.context, location);
            };

    private static final IDJobService.IIDSeriviceListener seriviceListener = trackID -> {
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
        LocationTracker.addOnLocationListener(locationListener);
        IDJobService.addOnTrackIDChangeListener(seriviceListener);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean wt = preferences.getBoolean("wt", false);
        int freq = preferences.getInt("freq", 0);

        if (BuildConfig.DEBUG) Log.d(LT, "init wt=" + wt + " freq=" + freq);

        if (wt && freq > 0) {
            start(freq);
        }
    }

    public static void done() {
        LocationTracker.removeOnLocationListener(locationListener);
        LocationTracker.stop();
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
            final IDJobService.IIDSeriviceListener listener = new IDJobService.IIDSeriviceListener() {
                @Override
                public void onTrackID(@NonNull String trackID) {
                    IDJobService.removeOnTrackIDChangeListener(this);
                    if (BuildConfig.DEBUG) Log.d(LT, "got new tid=" + trackID);
                    if (!"".equals(trackID)) {
                        // duplicate of ITagsService.onTrackID
                        preferences.edit().putString("tid", trackID).apply();
                        if (BuildConfig.DEBUG) Log.d(LT, "will request start after new tid");
                        handler.post(() ->
                                LocationTracker.requestStart(gpsLocatonUpdater, frequency));
                    }
                }
            };
            IDJobService.addOnTrackIDChangeListener(listener);
            IDJobService.enqueueRetrieveId(context, "");
        } else {
            if (BuildConfig.DEBUG) Log.d(LT, "will request start immediately");
            LocationTracker.requestStart(gpsLocatonUpdater, frequency);
        }
    }

    public static void stop() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ITagApplication.context);
        preferences.edit().putBoolean("wt", false).apply();
        LocationTracker.stop();
    }
}
