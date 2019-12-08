package s4y.itag.itag;

import android.content.Context;
import android.util.Log;

import s4y.itag.ITagApplication;
import s4y.itag.ble.BLEConnectionInterface;
import s4y.itag.ble.BLEConnectionState;
import s4y.itag.ble.BLEDefault;
import s4y.itag.ble.BLEInterface;

public class ITag {
    private static final String LT = ITag.class.getName();
    public static final int BLE_TIMEOUT = 30;
    public static final int SCAN_TIMEOUT = 25;
    public static BLEInterface ble;
    public static ITagsStoreInterface store;

    public static void initITag(Context context) {
        ble = BLEDefault.shared(ITagApplication.context);
        store = new ITagsStoreDefault(ITagApplication.context);
        for (int i = 0; i < store.count(); i++) {
            connectAsync(store.byPos(i),0);
        }
    }

    public static void close() throws Exception {
        ble.close();
    }

    private static int connectThreadsCount = 0;

    private static void connectAsync(ITagInterface itag, int timeout) {
        connectThreadsCount++;
        Log.d(LT, "BLE Connect thread started, count = " + connectThreadsCount);
        new Thread("BLE Connect " + itag.id() + " " + System.currentTimeMillis()) {
            @Override
            public void run() {
                BLEConnectionInterface connection = ble.connectionById(itag.id());
                do {
                    Log.d(LT, "Attempt to connect " + itag.id()+"/"+itag.name());
                    connection.connect(timeout);
                } while (timeout == 0 && !connection.isConnected());
                connectThreadsCount--;
                Log.d(LT, "BLE Connect thread finished, count = " + connectThreadsCount);
            }
        }.start();
    }
}
