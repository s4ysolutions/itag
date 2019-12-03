package s4y.itag.itag;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

import s4y.itag.preference.SerializablePreferenceNotNull;

class PreferenceIDsForever extends SerializablePreferenceNotNull<Set<String>> {

    PreferenceIDsForever(@NonNull Context context) {
        super(context, "foreverids", new HashSet<String>());
    }
}
