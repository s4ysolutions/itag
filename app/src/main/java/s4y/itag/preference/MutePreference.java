package s4y.itag.preference;

import android.content.Context;

public class MutePreference extends BooleanPreference {
    public MutePreference(Context context) {
        super(context, "mute", false);
    }
}
