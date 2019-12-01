package s4y.itag.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

@SuppressWarnings("unchecked")
public class SerializablePreferenceNotNull<T> {
    @NonNull
    private final SharedPreferences preferences;
    @NonNull
    private final String key;
    @NonNull
    private final String defaultValue;
    @NonNull
    private final Serializable defaultObject;

    protected SerializablePreferenceNotNull(@NonNull Context context,
                                            @NonNull String key,
                                            @NonNull Serializable defaultValue) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.key = key;
        this.defaultObject = defaultValue;
        this.defaultValue = objectToString(defaultValue);
    }

    public @NonNull
    T get() {
        String serialized = preferences.getString(key, defaultValue);
        return (T) stringToObject(serialized);
    }

    public void set(@NonNull T value) {
        String serialized = objectToString((Serializable) value);
        preferences.edit().putString(key, serialized).apply();
    }

    @NonNull
    private String objectToString(Object object) {
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

    @NonNull
    private Object stringToObject(@NonNull String string) {
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
