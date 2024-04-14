package s4y.itag.itag;

import androidx.annotation.NonNull;

import java.util.Map;

public interface ITagInterface {
    @NonNull
    String id();
    String name();
    void setName(String name);
    Boolean isShaking();
    TagColor color();
    void setColor(TagColor color);
    void setShaking(Boolean currentlyShaking);
    void setAlertMode(TagAlertMode alertMode);
    void setConnectionMode(TagConnectionMode connectionMode);
    TagConnectionMode connectionMode();
    TagAlertMode alertMode();
    boolean isConnectModeEnabled();
    int alertDelay();
    void setAlertDelay(int alarmDelay);
    void copyFromTag(ITagInterface tag);
    @NonNull
    Map<String,Object> toDict();
    @NonNull
    String toString();
}
