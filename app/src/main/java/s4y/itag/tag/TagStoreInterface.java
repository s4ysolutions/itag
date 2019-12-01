package s4y.itag.tag;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import s4y.rasat.Observable;

public interface TagStoreInterface {
    int count();
    @NonNull
    Observable<StoreOp> observable();
    @Nullable
    TagInterface byId(@NonNull String id);
    TagInterface byPos(int pos);
    TagInterface everById(@NonNull String id);
    @NonNull
    String[] forgottenIDs();
    void forget(@NonNull String id);
    void remember(@NonNull TagInterface tag);
    boolean remembered(@NonNull String id);
    void setAlert(@NonNull String id,boolean alert);
    void setColor(@NonNull String id,@NonNull TagColor color);
    void setName(@NonNull String id,String name);
    void connectAll();
    void stopAlertAll();
}
