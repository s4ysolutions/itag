package s4y.itag.waytoday;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import s4y.itag.BuildConfig;
import s4y.itag.ITagApplication;
import solutions.s4y.waytoday.sdk.locations.LocationGPSUpdater;
import solutions.s4y.waytoday.sdk.tracker.Tracker;

public class Waytoday {

    public static LocationGPSUpdater gpsLocationUpdater;
    public static Tracker tracker = new Tracker(BuildConfig.WAYTODAY_SECRET, "iTagAndroid");
    private static boolean initialized = false;

    public static void init(Context context) {
        initialized = true;
        gpsLocationUpdater = new LocationGPSUpdater(context);
        gpsLocationUpdater.addOnPermissionListener(() -> {

        });
        if (tracker.isOn(context)) {
            start(context);
        }
    }

    public static void done(Context context) {
        tracker.stop(context);
    }

    public static void start(Context context) {
        if (!initialized) {
            init(context);
        }
        tracker.requestStart(context, gpsLocationUpdater);
    }

    public static void stop(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ITagApplication.context);
        preferences.edit().putBoolean("wt", false).apply();
        tracker.stop(context);
        tracker.resetFilter();
    }
}
