package solutions.s4y.itag.ble;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;

import solutions.s4y.itag.BuildConfig;

public class ITagsService extends Service {
    private static final String T = ITagsService.class.getName();

    private HashMap<String, ITagGatt> mGatts = new HashMap<>(4);

    public class GattBinder extends Binder {
        public ITagsService getService() {
            return ITagsService.this;
        }
    }

    private IBinder mBinder = new GattBinder();

    public ITagsService() {
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
