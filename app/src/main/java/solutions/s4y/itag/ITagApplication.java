package solutions.s4y.itag;

import android.app.Application;
import android.widget.Toast;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import solutions.s4y.itag.ble.Db;

public final class ITagApplication extends Application {
    final static public PublishSubject<Throwable> errorNotifier = PublishSubject.create();
    final private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override
    public void onCreate() {
        super.onCreate();
        Db.load(this);
        mCompositeDisposable.add(errorNotifier
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        ex ->
                                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show(),
                        ex ->
                                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show(),
                        () -> {
                        }
                ));
    }
}
