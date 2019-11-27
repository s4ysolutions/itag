package s4y.observables;

public class Subscription<T> implements  AutoCloseable {
    private final Observer<T> observer;
    private final Observable<T> observable;

    Subscription(Observer<T> observer, Observable<T> observable) {
        this.observer = observer;
        this.observable = observable;
    }

    public void dispose(){
       observable.remove(observer);
    }

    @Override
    public void close() {
       dispose();
    }
}
