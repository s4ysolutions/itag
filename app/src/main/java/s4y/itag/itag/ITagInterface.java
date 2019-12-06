package s4y.itag.itag;

import androidx.annotation.NonNull;

import java.util.Map;

public interface ITagInterface {
    @NonNull
    String id();
    String name();
    void setName(String name);
    TagColor color();
    void setColor(TagColor color);
    boolean isAlertDisconnected();
    void setAlertDisconnected(boolean alerting);
    int alertDelay();
    void setAlertDelay(int alarmDelay);
    void copyFromTag(ITagInterface tag);
    @NonNull
    Map<String,Object> toDict();
    @NonNull
    String toString();
}
