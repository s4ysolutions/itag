package s4y.rasat;

public class Disposable<T> implements  AutoCloseable {
    private final Handler<T> handler;
    private final Observable<T> observable;

    Disposable(Handler<T> handler, Observable<T> observable) {
        this.handler = handler;
        this.observable = observable;
    }

    public void dispose(){
       observable.remove(handler);
    }

    @Override
    public void close() {
       dispose();
    }
}
