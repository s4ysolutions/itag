package s4y.itag.itag;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import s4y.itag.preference.SerializablePreferenceNotNull;

class PreferenceIDs extends SerializablePreferenceNotNull<List<String>> {

    PreferenceIDs(@NonNull Context context) {
        super(context, "ids", new ArrayList<String>());
    }
}
