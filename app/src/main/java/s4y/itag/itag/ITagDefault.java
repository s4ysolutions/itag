package s4y.itag.itag;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import s4y.itag.ITagApplication;
import s4y.itag.R;
import s4y.itag.ble.BLEScanResult;

public class ITagDefault implements ITagInterface, Serializable {
private static final long serialVersionUID = 1575220516;
@NonNull
    private final String id;
    @NonNull
    private String name;
    @NonNull
    private TagColor color;
    private boolean alert;
    private int alertDelay;

    public ITagDefault(@NonNull String id, @Nullable String name, @Nullable TagColor color, @Nullable Boolean alert, @Nullable Integer alertDelay) {
        this.id = id;
        this.name = name == null ? ITagApplication.context.getString(R.string.unknown):name;
        this.color = color == null? TagColor.black : color;
        this.alert = alert == null ? false : alert;
        this.alertDelay = alertDelay == null ? 7: alertDelay;
    }

    public ITagDefault(@NonNull BLEScanResult scanResult) {
        this(scanResult.id, scanResult.name, null, null, null);
    }

    public ITagDefault(@NonNull String id, Map<String, Object> dict) {
        this(id, (String)dict.get("name"), (TagColor)dict.get("color"), (Boolean)dict.get("alert"), (Integer)dict.get("alertDelay"));
    }

    @NonNull
    @Override
    public String id() {
        return id;
    }

    @NonNull
    @Override
    public String name() {
        return name;
    }

    @Override
    public void setName(@NonNull  String name) {
        this.name = name;
    }

    @NonNull
    @Override
    public TagColor color() {
        return color;
    }

    @Override
    public void setColor(@NonNull TagColor color) {
        this.color = color;
    }

    @Override
    public boolean isAlertDisconnected() {
        return alert;
    }

    @Override
    public void setAlertDisconnected(boolean alerting) {
        this.alert = alerting;
    }

    @Override
    public int alertDelay() {
        return alertDelay;
    }

    @Override
    public void setAlertDelay(int alertDelay) {
        this.alertDelay = alertDelay;
    }

    @Override
    public void copyFromTag(@NonNull ITagInterface tag) {
        name = tag.name();
        alert = tag.isAlertDisconnected();
        color = tag.color();
    }

    @NonNull
    @Override
    public Map<String, Object> toDict() {
        return new HashMap<String, Object>(){{
            put("id", id);
            put("name", name);
            put("color", color);
            put("alert", alert);
        }};
    }
}
