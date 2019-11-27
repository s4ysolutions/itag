package s4y.observables;

public interface Observer<T> {
    void onNext(T t);
}
