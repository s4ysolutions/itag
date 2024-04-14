package s4y.itag.itag;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import solutions.s4y.rasat.Observable;

public interface ITagsStoreInterface {
    int count();
    boolean isDisconnectAlertOn();
    @NonNull
    Observable<StoreOp> observable();
    @Nullable
    ITagInterface byId(@NonNull String id);
    ITagInterface byPos(int pos);
    ITagInterface everById(@NonNull String id);
    @NonNull
    String[] forgottenIDs();
    void forget(@NonNull String id);
    void forget(@NonNull ITagInterface tag);
    void remember(@NonNull ITagInterface tag);
    boolean remembered(@NonNull String id);
    void setAlertDelay(@NonNull String id,int delay);
    void setAlertMode(@NonNull String id, TagAlertMode alertMode);
    void setShakingOnConnectDisconnect(@NonNull String id, Boolean shaking);
    void setConnectionMode(@NonNull String id, TagConnectionMode connectionMode);
    void setConnectMode(@NonNull String id, TagConnectionMode connectionMode);
    void setColor(@NonNull String id,@NonNull TagColor color);
    void setName(@NonNull String id,String name);
    List<String> getIds();
}
