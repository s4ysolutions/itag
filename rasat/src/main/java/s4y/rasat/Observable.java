package s4y.rasat;

import java.util.HashSet;
import java.util.Set;

public class Observable<T> {
    final Set<Handler<T>> handlers = new HashSet<>();
    T value;

    public Observable(T value) {
        this.value = value;
    }

    public Observable() {
        this.value = null;
    }

    void remove(Handler<T> handler) {
        synchronized (handlers) {
            handlers.remove(handler);
        }
    }

    public Disposable<T> subscribe(Handler<T> handler) {
        synchronized (handlers) {
            handlers.add(handler);
        }
        return new Disposable<>(handler, this);
    }

    public T value() {
        return value;
    }
}
