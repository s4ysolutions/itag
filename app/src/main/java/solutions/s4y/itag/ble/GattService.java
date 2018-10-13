package solutions.s4y.itag.ble;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;

import solutions.s4y.itag.BuildConfig;

public class GattService extends Service {
    private static final String T = GattService.class.getName();

    private HashMap<String, ITagGatt> mGatts = new HashMap<>(4);

    public class GattBinder extends Binder {
        public GattService getService() {
            return GattService.this;
        }
    }

    private IBinder mBinder = new GattBinder();

    public GattService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(T, "onDestroy");
        }

        super.onDestroy();
    }
}
