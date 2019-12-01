package s4y.itag.preference;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

public class StringPreference {
    @NonNull private final SharedPreferences preferences;
    @NonNull private final String key;
    @Nullable private final String defaultValue;

    protected StringPreference(@NonNull Context context,
                               @NonNull String key,
                               @Nullable String defaultValue) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public @Nullable String get() {
        return preferences.getString(key, defaultValue);
    }

    public void set(@Nullable String value) {
        preferences.edit().putString(key, value).apply();
    }
}
