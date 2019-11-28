package s4y.itag.ble;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BLEConnectionsStoreDefault implements BLEConnectionsStoreInterface {
    private final Map<String, BLEConnectionInterface> map = new HashMap<>();

    public BLEConnectionsStoreDefault() {
    }


    @Nullable
    @Override
    public BLEConnectionInterface get(String id) {
        return map.get(id);
    }

    @NonNull
    @Override
    public BLEConnectionInterface getOrMake(String id) {
        return null;
    }
}
