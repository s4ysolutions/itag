package s4y.itag.preference;

import android.content.Context;

public class VolumePreference extends IntPreference {
    public static final int MUTE = 0;
    public static final int LOUD = 1;
    public static final int VIBRATION = 2;

    public VolumePreference(Context context) {
        super(context, "volume", 0);
    }
}
