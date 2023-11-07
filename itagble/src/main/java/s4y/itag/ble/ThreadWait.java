package s4y.itag.ble;

class ThreadWait<T> {
    private T state;

    synchronized void setPayload(T state) {
        this.state = state;
        notifyAll();
    }

    synchronized void waitFor(Runnable runnable, long timeoutSec) {
        state = null;
        runnable.run();
        try {
            if (timeoutSec > 0) {
                wait(timeoutSec * 1000);
            } else {
                wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    boolean isTimedOut() {
        return state == null;
    }

    T payload() {
        return state;
    }
}
