package s4y.itag.preference;

import android.content.Context;

public class WayTodayFirstPreference extends BooleanPreference {
    public static boolean get(Context context) {
        return new WayTodayFirstPreference(context).get();
    }

    public static void set(Context context, boolean value) {
        new WayTodayFirstPreference(context).set(value);
    }

    public WayTodayFirstPreference(Context context) {
        super(context, "wtfirst", true);
    }
}
