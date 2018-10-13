package solutions.s4y.itag.ble;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

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
        connect();
        return mBinder;
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(T, "onDestroy");
        }

        super.onDestroy();
    }

    public ITagGatt getGatt(@NotNull final String addr, boolean connect) {
        ITagGatt gatt = mGatts.get(addr);
        if (gatt == null) {
            gatt = new ITagGatt(addr);
            mGatts.put(addr, gatt);
            if (connect) gatt.connect(this);
        }
        return gatt;
    }

    public void connect() {
        for (ITagDevice device: ITagsDb.getDevices(this)) {
            getGatt(device.addr, true);
        }
    }

    public void alert(@NotNull final String addr) {
        ITagGatt gatt = mGatts.get(addr);
        gatt.alert();
    }
}
