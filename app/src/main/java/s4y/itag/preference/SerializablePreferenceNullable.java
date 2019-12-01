package s4y.itag.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import s4y.rasat.Observable;

@SuppressWarnings("unchecked")
public class SerializablePreferenceNullable<T> {
    @NonNull
    private final SharedPreferences preferences;
    @NonNull
    private final String key;
    @Nullable
    private final String defaultValue;
    @Nullable
    private final Serializable defaultObject;

    protected SerializablePreferenceNullable(@NonNull Context context,
                                             @NonNull String key,
                                             @Nullable Serializable defaultValue) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.key = key;
        this.defaultObject = defaultValue;
        this.defaultValue = objectToString(defaultValue);
    }

    public @Nullable
    T get() {
        String serialized = preferences.getString(key, defaultValue);
        return (T) stringToObject(serialized);
    }

    public void set(@Nullable T value) {
        String serialized = objectToString((Serializable) value);
        preferences.edit().putString(key, serialized).apply();
    }

    @Nullable
    private String objectToString(Object object) {
        if (object == null) {
            return null;
        }
        String encoded = defaultValue;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
                objectOutputStream.writeObject(object);
                objectOutputStream.close();
                encoded = Base64.encodeToString(byteArrayOutputStream.toByteArray(), 0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return encoded;
    }

    @Nullable
    private Object stringToObject(@Nullable String string) {
        if (string == null) {
            return null;
        }
        Object object = defaultObject;
        byte[] bytes = Base64.decode(string, 0);
        try(ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes))){
            object = objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            e.printStackTrace();
        }
        return object;
    }
}
