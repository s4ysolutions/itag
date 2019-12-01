package s4y.itag.tag;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

public interface TagFactoryInterface {
    TagInterface tag(@NonNull String id, @Nullable String name,@Nullable TagColor color,@Nullable Boolean alert);
    TagInterface tag(@NonNull String id, Map<String, Object> dict);
}
