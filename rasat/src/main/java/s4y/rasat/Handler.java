package s4y.rasat;

public interface Handler<T> {
    void handle(T event);
}
