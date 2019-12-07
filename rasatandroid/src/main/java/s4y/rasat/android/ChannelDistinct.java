package s4y.rasat.android;

import android.os.Handler;
import android.os.Looper;

public class ChannelDistinct<T> extends s4y.rasat.ChannelDistinct<T> {
    private Handler postHandler;

    public ChannelDistinct(T value) {
        this(value, new Handler(Looper.getMainLooper()));
    }

    public ChannelDistinct() {
        this(new Handler(Looper.getMainLooper()));
    }

    public ChannelDistinct(T value, Handler handler) {
        super(value);
        postHandler = handler;
    }

    public ChannelDistinct(Handler handler) {
        super();
        postHandler = handler;
    }

    public void setPostHandler(Handler handler) {
        this.postHandler = handler;
    }

    @Override
    public void broadcast(final T value) {
        if (postHandler == null)
            super.broadcast(value);
        else
            postHandler.post(() -> super.broadcast(value));
    }
}
