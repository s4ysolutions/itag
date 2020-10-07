package s4y.rasat.android;

import android.os.Handler;
import android.os.Looper;

public class Channel<T> extends solutions.s4y.rasat.Channel<T> {
    private Handler postHandler;

    public Channel(T value, Handler handler) {
        super(value);
        postHandler = handler;
    }

    public Channel(Handler handler) {
        super();
        postHandler = handler;
    }

    public Channel(T value) {
        this(value, new Handler(Looper.getMainLooper()));
    }

    public Channel() {
        this(new Handler(Looper.getMainLooper()));
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
