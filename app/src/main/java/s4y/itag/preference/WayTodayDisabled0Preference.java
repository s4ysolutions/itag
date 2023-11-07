package s4y.itag.preference;

import android.content.Context;

public class WayTodayDisabled0Preference extends BooleanPreference {
    public static boolean get(Context context) {
        return new WayTodayDisabled0Preference(context).get();
    }

    public static void set(Context context, boolean value) {
        new WayTodayDisabled0Preference(context).set(value);
    }

    public WayTodayDisabled0Preference(Context context) {
        super(context, "wt_disabled0", false);
    }
}
