package s4y.itag.waytoday;

import android.app.Application;
import s4y.itag.BuildConfig;
import s4y.itag.ITagApplication;
import solutions.s4y.waytoday.sdk.locations.LocationGPSUpdater;
import solutions.s4y.waytoday.sdk.tracker.Tracker;

public class Waytoday {

    public static LocationGPSUpdater gpsLocationUpdater;
    public static final Tracker tracker = new Tracker(BuildConfig.WAYTODAY_SECRET, "iTagAndroid");
    private static boolean initialized = false;
    private static Application context;

    public static void init(Application context) {
        initialized = true;
        Waytoday.context = context;

        gpsLocationUpdater = new LocationGPSUpdater(context);
        gpsLocationUpdater.addOnPermissionListener(() -> {

        });
        if (tracker.isOn(context)) {
            start();
        }
    }

    public static void done() {
        tracker.stop(context);
    }

    public static void start() {
        if (!initialized) {
            init(ITagApplication.application);
        }
        tracker.requestStart(ITagApplication.context, gpsLocationUpdater);
    }

    public static void stop() {
        tracker.stop(context);
        tracker.resetFilter();
    }
}
