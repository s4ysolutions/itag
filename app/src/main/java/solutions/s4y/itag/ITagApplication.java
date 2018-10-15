package solutions.s4y.itag;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public final class ITagApplication extends Application {
    private final static String LT = ITagApplication.class.getName();
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context=this;
    }

    static public void handleError(Throwable th) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (context==null) {
                Log.e(LT,"Attempt to handle error before application created", th);
            }else {
                Log.e(LT,"Toasted", th);
                Toast.makeText(context, th.getMessage(), Toast.LENGTH_LONG).show();
                Crashlytics.logException(th);
            }
        });
    }

}
