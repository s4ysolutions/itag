package s4y.rasat;

public class Channel<T> {
    final private Observable<T> observable;

    public Channel(T value) {
        this.observable = new Observable<>(value);
    }

    public Channel() {
        this.observable = new Observable<>();
    }

    public void broadcast(T value) {
        observable.value = value;
        for (Handler<T> handler : observable.handlers) {
            handler.handle(value);
        }
    }
}
