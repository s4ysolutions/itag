package s4y.rasat;

public class Channel<T> {
    private final T initialValue;
    public final Observable<T> observable;
    public Channel(T value) {
        this.observable = new Observable<>(value);
        this.initialValue = this.observable.value;
    }

    public Channel() {
        this.observable = new Observable<>();
        this.initialValue = this.observable.value;
    }

    public void reset() {
       this.observable.value = initialValue;
    }

    public void broadcast(T value) {
        observable.value = value;
        synchronized (observable.handlers) {
            for (Handler<T> handler : observable.handlers) {
                handler.handle(value);
            }
        }
    }
}
