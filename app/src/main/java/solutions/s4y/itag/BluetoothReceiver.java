package solutions.s4y.itag;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import solutions.s4y.itag.ble.ITagsService;

public class BluetoothReceiver extends BroadcastReceiver {
    private static final String LT = BluetoothReceiver.class.getName();

    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            Log.d(LT, "bluetooth change state: " + bluetoothState);

            if (bluetoothState == BluetoothAdapter.STATE_ON) {
                ITagsService.start(context);
            } else if (bluetoothState == BluetoothAdapter.STATE_OFF) {
                ITagsService.stop(context);
            }
        }
    }
}
