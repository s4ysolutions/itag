package s4y.observables;


import java.util.HashSet;
import java.util.Set;

public class Observable<T> {
    private final Set<Observer<T>> observers = new HashSet<>();
    private T value;

    public Observable(T value) {
        this.value = value;
    }

    public Observable() {
        this.value = null;
    }

    void remove(Observer<T> observer) {
        observers.remove(observer);
    }

    public Subscription<T> subscribe(Observer<T> observer) {
        observers.add(observer);
        return new Subscription<>(observer,this);
    };

    public void onNext(T value) {
        this.value = value;
        for (Observer<T> observer : observers) {
            observer.onNext(value);
        }
    }

    public T value() {
        return value;
    }
}
