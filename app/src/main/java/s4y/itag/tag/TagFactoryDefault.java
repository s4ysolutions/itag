package s4y.itag.tag;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

public class TagFactoryDefault implements TagFactoryInterface {
    @Override
    public TagInterface tag(@NonNull String id, @Nullable String name, @Nullable TagColor color,@Nullable Boolean alert) {
        return new TagDefault(id, name, color, alert);
    }

    @Override
    public TagInterface tag(@NonNull String id, Map<String, Object> dict) {
        return null;
    }
}
