package s4y.itag.itag;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import solutions.s4y.rasat.Channel;
import solutions.s4y.rasat.Observable;

public class ITagsStoreDefault implements ITagsStoreInterface {
    private final Context context;
    private final List<String> ids;
    private final Set<String> idsForever;
    private final Map<String, ITagInterface> tags = new HashMap<>();

    private final Channel<StoreOp> channel = new Channel<>();

    public ITagsStoreDefault(Context context) {
        this.context = context;

        List<String> ids0 = new PreferenceIDs(context).get();
        synchronized (this) {
            idsForever = new PreferenceIDsForever(context).get();

            for (String id : idsForever) {
                // TODO: hardcoded reference to Default implementation
                ITagInterface tag = new PreferenceTagDefault(context, id).get();
                if (tag != null) {
                    tags.put(id, tag);
                }
            }

            if (ids0.size() > 0) {
                ArrayList<String> absent = new ArrayList<>();
                for (String id : ids0) {
                    if (!tags.containsKey(id) || tags.get(id) == null) {
                        absent.add(id);
                    }
                }
                for (String id : absent) {
                    int pos = ids0.indexOf(id);
                    while (pos >= 0) {
                        ids0.remove(pos);
                        pos = ids0.indexOf(id);
                    }
                }
            }
            ids = ids0;
        }
    }

    @Override
    synchronized public int count() {
        return ids.size();
    }

    @Override
    synchronized public boolean isDisconnectAlertOn() {
        for (String id : ids) {
            ITagInterface itag = tags.get(id);
            if (itag != null && itag.isConnectModeEnabled()) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public Observable<StoreOp> observable() {
        return channel.observable;
    }

    @Nullable
    @Override
    synchronized public ITagInterface byId(@NonNull String id) {
        return tags.get(id);
    }

    @Override
    synchronized public ITagInterface byPos(int pos) {
        if (pos >= ids.size()) {
            return null;
        }
        return tags.get(ids.get(pos));
    }

    @Override
    synchronized public ITagInterface everById(@NonNull String id) {
        ITagInterface active = byId(id);
        if (active != null) {
            return active;
        }
        // TODO: hardcoded reference to Default implementation
        return new PreferenceTagDefault(context, id).get();
    }

    @NonNull
    @Override
    synchronized public String[] forgottenIDs() {
        List<String> ret = new ArrayList<>(idsForever.size());

        for (String id : idsForever) {
            if (!ids.contains(id)) {
                ret.add(id);
            }
        }
        return ret.toArray(new String[0]);
    }

    @Override
    synchronized public void forget(@NonNull String id) {
        int pos = ids.indexOf(id);
        if (pos >= 0) {
            ids.remove(pos);
            new PreferenceIDs(context).set(ids);
            ITagInterface tag = tags.get(id);
            if (tag != null) {
                channel.broadcast(new StoreOp(StoreOpType.forget, tag));
            }
        }
    }

    @Override
    synchronized public void forget(@NonNull ITagInterface tag) {
        forget(tag.id());
    }

    @Override
    synchronized public void remember(@NonNull ITagInterface tag) {
        if (!ids.contains(tag.id())) {
            ids.add(tag.id());
            new PreferenceIDs(context).set(ids);
        }

        if (!idsForever.contains(tag.id())) {
            idsForever.add(tag.id());
            new PreferenceIDsForever(context).set(idsForever);
        }

        ITagInterface existing = everById(tag.id());
        if (existing != null) {
            tag.copyFromTag(existing);
        }

        tags.put(tag.id(), tag);
        // TODO: hardcoded reference to Default implementation
        new PreferenceTagDefault(context, tag.id()).set((ITagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.remember, tag));
    }

    @Override
    synchronized public boolean remembered(@NonNull String id) {
        return ids.contains(id);
    }

    @Override
    synchronized public void setAlertDelay(@NonNull String id, int delay) {
        ITagInterface tag = tags.get(id);
        if (tag == null) {
            return;
        }
        tag.setAlertDelay(delay);
        new PreferenceTagDefault(context, tag.id()).set((ITagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.change, tag));
    }

    @Override
    synchronized public void setAlertMode(@NonNull String id, TagAlertMode alertMode) {
        ITagInterface tag = tags.get(id);
        if (tag == null) {
            return;
        }
        tag.setAlertMode(alertMode);
        new PreferenceTagDefault(context, tag.id()).set((ITagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.change, tag));
    }

    @Override
    synchronized public void setShakingOnConnectDisconnect(@NonNull String id, Boolean shaking) {
        ITagInterface tag = tags.get(id);
        if (tag == null) {
            return;
        }
        tag.setShaking(shaking);
        //new PreferenceTagDefault(context, tag.id()).set((ITagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.change, tag));
    }

    @Override
    synchronized public void setPassivelyDisconnected(@NonNull String id, Boolean has_disconnected) {
        ITagInterface tag = tags.get(id);
        if (tag == null) {
            return;
        }
        tag.setPassivelyDisconnected(has_disconnected);
        //new PreferenceTagDefault(context, tag.id()).set((ITagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.change, tag));
    }

    @Override
    synchronized public void setReconnectMode(@NonNull String id, Boolean reconnect) {
        ITagInterface tag = tags.get(id);
        if (tag == null) {
            return;
        }
        tag.setReconnectMode(reconnect);
        //new PreferenceTagDefault(context, tag.id()).set((ITagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.change, tag));
    }

    @Override
    synchronized public void setConnectionMode(@NonNull String id, TagConnectionMode connectionMode) {
        ITagInterface tag = tags.get(id);
        if (tag == null) {
            return;
        }
        tag.setConnectionMode(connectionMode);
        new PreferenceTagDefault(context, tag.id()).set((ITagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.change, tag));
    }

    @Override
    synchronized public void setConnectMode(@NonNull String id, TagConnectionMode connectionMode) {
        ITagInterface tag = tags.get(id);
        if (tag == null) {
            return;
        }
        tag.setConnectionMode(connectionMode);
        new PreferenceTagDefault(context, tag.id()).set((ITagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.change, tag));
    }

    @Override
    synchronized public void setColor(@NonNull String id, @NonNull TagColor color) {
        ITagInterface tag = tags.get(id);
        if (tag == null) {
            return;
        }
        tag.setColor(color);
        new PreferenceTagDefault(context, tag.id()).set((ITagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.change, tag));
    }

    @Override
    synchronized public void setName(@NonNull String id, String name) {
        ITagInterface tag = tags.get(id);
        if (tag == null) {
            return;
        }
        tag.setName(name);
        new PreferenceTagDefault(context, tag.id()).set((ITagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.change, tag));
    }

    @Override
    synchronized public List<String> getIds() {
        return this.ids;
    }

    @Override
    synchronized public Map<String, ITagInterface> getTagMap(){
        return this.tags;
    }
}
