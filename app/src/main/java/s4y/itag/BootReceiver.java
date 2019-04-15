package s4y.itag;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import s4y.itag.ble.ITagsService;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences preferences =(context.getSharedPreferences("s4y.solutions.itags.prefs", Context.MODE_PRIVATE));
            if (preferences.getBoolean("loadOnBoot", true)) {
                ITagsService.startInForeground(context);
            }
        }
    }
}
