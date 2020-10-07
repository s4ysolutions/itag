package s4y.itag.itag;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import s4y.itag.preference.BooleanPreference;
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

        ids = new PreferenceIDs(context).get();
        idsForever = new PreferenceIDsForever(context).get();

        for (String id : idsForever) {
            // TODO: hardcoded reference to Default implementation
            ITagInterface tag = new PreferenceTagDefault(context, id).get();
            if (tag != null) {
                tags.put(id, tag);
            }
        }

        if (ids.size() == 0) {
            BooleanPreference prefUpgradeDone = new BooleanPreference(context, "upgradeDone", false);
            if (!prefUpgradeDone.get()) {
                List<ITagInterface> itags = ITagFileStore.load(context);
                if (itags.size() > 0) {
                    for (ITagInterface itag : itags) {
                        remember(itag);
                    }
                }
                prefUpgradeDone.set(true);
            }
        }
    }

    @Override
    public int count() {
        return ids.size();
    }

    @Override
    public boolean isDisconnectAlert() {
        for (String id: ids){
            ITagInterface itag = tags.get(id);
            if (itag != null && itag.isAlertDisconnected()) {
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
    public ITagInterface byId(@NonNull String id) {
        return tags.get(id);
    }

    @Override
    public ITagInterface byPos(int pos) {
        if (pos >= ids.size()) {
            return null;
        }
        return tags.get(ids.get(pos));
    }

    @Override
    public ITagInterface everById(@NonNull String id) {
        ITagInterface active = byId(id);
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
            ITagInterface tag = tags.get(id);
            if (tag != null) {
                channel.broadcast(new StoreOp(StoreOpType.forget, tag));
            }
        }
    }

    @Override
    public void forget(@NonNull ITagInterface tag) {
        forget(tag.id());
    }

    @Override
    public void remember(@NonNull ITagInterface tag) {
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
    public boolean remembered(@NonNull String id) {
        return ids.contains(id);
    }

    @Override
    public void setAlertDelay(@NonNull String id, int delay) {
        ITagInterface tag = tags.get(id);
        if (tag == null) {
            return;
        }
        tag.setAlertDelay(delay);
        new PreferenceTagDefault(context, tag.id()).set((ITagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.change, tag));
    }

    @Override
    public void setAlert(@NonNull String id, boolean alert) {
        ITagInterface tag = tags.get(id);
        if (tag == null) {
            return;
        }
        tag.setAlertDisconnected(alert);
        new PreferenceTagDefault(context, tag.id()).set((ITagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.change, tag));
    }

    @Override
    public void setColor(@NonNull String id, @NonNull TagColor color) {
        ITagInterface tag = tags.get(id);
        if (tag == null) {
            return;
        }
        tag.setColor(color);
        new PreferenceTagDefault(context, tag.id()).set((ITagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.change, tag));
    }

    @Override
    public void setName(@NonNull String id, String name) {
        ITagInterface tag = tags.get(id);
        if (tag == null) {
            return;
        }
        tag.setName(name);
        new PreferenceTagDefault(context, tag.id()).set((ITagDefault) tag);
        channel.broadcast(new StoreOp(StoreOpType.change, tag));
    }
}
