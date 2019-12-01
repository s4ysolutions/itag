package s4y.itag.tag;

import android.os.Handler;
import android.os.HandlerThread;

class ITag {
    private static final HandlerThread thread = new HandlerThread("BLE Main Thread");
    static final Handler handler = new Handler(thread.getLooper());
    static final int BLE_TIMEOUT = 60;
}
