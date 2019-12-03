package s4y.itag.itag;

import android.content.Context;

import androidx.annotation.NonNull;

import s4y.itag.preference.SerializablePreferenceNullable;

class PreferenceTag<T> extends SerializablePreferenceNullable<T> {
    PreferenceTag(@NonNull Context context, String id) {
        super(context, "tag"+id, null);
    }
}

