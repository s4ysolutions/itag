package s4y.itag.tag;

import androidx.annotation.NonNull;

import java.util.Map;

public interface TagInterface {
    @NonNull
    String id();
    String name();
    void setName(String name);
    TagColor color();
    void setColor(TagColor color);
    boolean isAlertig();
    void setAlerting(boolean alerting);
    void copyFromTag(TagInterface tag);
    @NonNull
    Map<String,Object> toDict();
    @NonNull
    String toString();
}
