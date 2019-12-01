package s4y.itag.tag;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

import s4y.itag.preference.SerializablePreferenceNotNull;
import s4y.itag.preference.SerializablePreferenceNullable;

class PreferenceIDsForever extends SerializablePreferenceNotNull<Set<String>> {

    PreferenceIDsForever(@NonNull Context context) {
        super(context, "foreverids", new HashSet<String>());
    }
}
