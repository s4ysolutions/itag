package s4y.itag.ble;

import android.os.Handler;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import s4y.observables.Observable;

public class BLEFindMeDefault implements BLEFindMeInterface, BLEFindMeControl {
    private static final int CLICK_INTERVAL = 600;
    private static final int CLICK_COUNT = 2;

    private final Observable<FindMe> observable = new Observable<>();
    private final Set<String> findMes = new HashSet<>();

    private Handler handler = new Handler();

    private class Click {
        int count = 0;
        Runnable waitNext = null;

        boolean done() {
            return count >= CLICK_COUNT;
        }
    }

    private final Map<String, Click> clicks = new HashMap<>();

    @NonNull
    private Click incClick(String id) {
        Click click;
        synchronized (clicks) {
            click = clicks.get(id);
            if (click == null) {
                click = new Click();
                clicks.put(id, click);
            }
            click.count++;
        }
        return click;
    }

    private void clearClick(String id) {
        synchronized (clicks) {
            clicks.remove(id);
        }
    }

    @Override
    public Observable<FindMe> observable() {
        return observable;
    }

    @Override
    public boolean isFindMe(String id) {
        synchronized (findMes) {
            return findMes.contains(id);
        }
    }

    @Override
    public void cancel(String id) {
        synchronized (findMes) {
            findMes.remove(id);
        }
        observable.onNext(new FindMe(id, false));
    }

    @Override
    public void onClick(String id) {
        Click click = incClick(id);
        if (click.waitNext != null) {
            handler.removeCallbacks(click.waitNext);
        }
        click.waitNext = () -> {
            clearClick(id);
            if (click.done()) {
                synchronized (findMes) {
                    findMes.add(id);
                }
                observable.onNext(new FindMe(id, true));
            } else {
                cancel(id);
            }
        };
        handler.postDelayed(click.waitNext, CLICK_INTERVAL);
    }
}
