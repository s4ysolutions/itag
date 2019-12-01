package s4y.itag.tag;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import s4y.itag.ITagApplication;
import s4y.itag.R;

public class TagDefault implements TagInterface {
    @NonNull
    private final String id;
    @NonNull
    private String name;
    @NonNull
    private TagColor color;
    private boolean alert;

    TagDefault(@NonNull String id, @Nullable String name, @Nullable TagColor color, @Nullable Boolean alert) {
        this.id = id;
        this.name = name == null ? ITagApplication.context.getString(R.string.unknown):name;
        this.color = color == null? TagColor.black : color;
        this.alert = alert == null ? false : alert;
    }

    public TagDefault(@NonNull String id, Map<String, Object> dict) {
        this(id, (String)dict.get("name"), (TagColor)dict.get("color"), (Boolean)dict.get("alert"));
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
    public boolean isAlertig() {
        return alert;
    }

    @Override
    public void setAlerting(boolean alerting) {
        this.alert = alerting;
    }

    @Override
    public void copyFromTag(@NonNull TagInterface tag) {
        name = tag.name();
        alert = tag.isAlertig();
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
