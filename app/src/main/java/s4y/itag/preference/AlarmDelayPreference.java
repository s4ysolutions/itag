package s4y.itag.preference;

import android.content.Context;

import s4y.itag.ble.ITagDevice;
import s4y.itag.ble.ITagGatt;

class AlarmDelayPreference extends IntPreference {
    protected AlarmDelayPreference(Context context, String addr) {
        super(context, "ad_" + addr, 300);
    }

    public AlarmDelayPreference(Context context, ITagDevice device) {
        this(context, device.addr);
    }

    public AlarmDelayPreference(Context context, ITagGatt gatt) {
        this(context, gatt.mAddr);
    }
}
