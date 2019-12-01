package s4y.itag.tag;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import s4y.itag.ble.BLEDefault;
import s4y.itag.ble.BLEInterface;
import s4y.rasat.Channel;
import s4y.rasat.Observable;

public class TagStoreDefault implements TagStoreInterface, Serializable {
    private static final long serialVersionUID = 1575220516;

    private static TagStoreInterface _shared;

    static TagStoreInterface shared(Context context) {
        if (_shared == null) {
            _shared = new TagStoreDefault(context);
        }
        return _shared;
    }

    private final Context context;
    private final BLEInterface ble;
    private final List<String> ids;
    private final Set<String> idsForever;
    private final Map<String, TagInterface> tags = new HashMap<>();

    private final Channel<StoreOp> channel = new Channel<>();

    private TagStoreDefault(Context context) {
        this.context = context;
        this.ble = BLEDefault.shared(context);
        ids = new PreferenceIDs(context).get();
        idsForever = new PreferenceIDsForever(context).get();

        for (String id : idsForever) {
            // TODO: hardcoded reference to Default implementation
            TagInterface tag = new PreferenceTagDefault(context, id).get();
            if (tag != null) {
                tags.put(id, tag);
            }
        }
    }

    @Override
    public int count() {
        return ids.size();
    }

    @NonNull
    @Override
    public Observable<StoreOp> observable() {
        return channel.observable;
    }

    @Nullable
    @Override
    public TagInterface byId(@NonNull String id) {
        return tags.get(id);
    }

    @Override
    public TagInterface byPos(int pos) {
        if (pos >= ids.size()) {
            return null;
        }
        return tags.get(ids.get(pos));
    }

    @Override
    public TagInterface everById(@NonNull String id) {
        TagInterface active = byId(id);
        if (active != null) {
            return active;
        }
        // TODO: hardcoded reference to Default implementation
        return new PreferenceTagDefault(context, id).get();
    }

    @NonNull
    @Override
    public String[] forgottenIDs() {
        List<String> ret = new ArrayList<>(idsForever.size());

        for (String id : idsForever) {
            if (!ids.contains(id)) {
                ret.add(id);
            }
        }
        return (String[]) ret.toArray();
    }

    @Override
    public void forget(@NonNull String id) {
        int pos = ids.indexOf(id);
        if (pos >= 0) {
            ids.remove(pos);
            new PreferenceIDs(context).set(ids);
            TagInterface tag = tags.get(id);
            if (tag != null) {
                channel.broadcast(new StoreOp(StoreOpType.forget, tag));
            }
        }
    }

    @Override
    public void remember(@NonNull TagInterface tag) {
        if (!ids.contains(tag.id())) {
            ids.add(tag.id());
            new PreferenceIDs(context).set(ids);
        }

        if (!idsForever.contains(tag.id())) {
            idsForever.add(tag.id());
            new PreferenceIDsForever(context).set(idsForever);
        }

        TagInterface existing = everById(tag.id());
        if (existing != null) {
            tag.copyFromTag(existing);
        }

        tags.put(tag.id(), tag);
        // TODO: hardcoded reference to Default implementation
        new PreferenceTagDefault(context, tag.id()).set((TagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.remember, tag));
    }

    @Override
    public boolean remembered(@NonNull String id) {
        return ids.contains(id);
    }

    @Override
    public void setAlert(@NonNull String id, boolean alert) {
        TagInterface tag = tags.get(id);
        if (tag == null) {
            return;
        }
        tag.setAlerting(alert);
        new PreferenceTagDefault(context, tag.id()).set((TagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.change, tag));
    }

    @Override
    public void setColor(@NonNull String id, @NonNull TagColor color) {
        TagInterface tag = tags.get(id);
        if (tag == null) {
            return;
        }
        tag.setColor(color);
        new PreferenceTagDefault(context, tag.id()).set((TagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.change, tag));
    }

    @Override
    public void setName(@NonNull String id, String name) {
        TagInterface tag = tags.get(id);
        if (tag == null) {
            return;
        }
        tag.setName(name);
        new PreferenceTagDefault(context, tag.id()).set((TagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.change, tag));
    }

    @Override
    public void connectAll() {
        for (TagInterface tag : tags.values()) {
            new Thread(() -> {
                try {
                    ble.connections().connect(tag.id());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            },"BLE Connect "+tag.id()+"-"+System.currentTimeMillis()).start();
        }
    }

    @Override
    public void stopAlertAll() {
        for (TagInterface tag : tags.values()) {
            if (tag.isAlertig()) {
                ITag.handler.post(() -> ble.alert().stopAlert(tag.id(), ITag.BLE_TIMEOUT));
            }
        }
    }
}
