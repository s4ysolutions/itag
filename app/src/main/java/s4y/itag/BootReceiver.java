package s4y.itag;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import s4y.itag.itag.ITag;
import s4y.itag.itag.ITagsStoreDefault;
import s4y.itag.itag.ITagsStoreInterface;
import s4y.waytoday.locations.LocationsTracker;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (
                Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                        ||
                        "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())
        ) {
            SharedPreferences preferences = (context.getSharedPreferences("s4y.solutions.itags.prefs", Context.MODE_PRIVATE));
            ITagsStoreInterface store = new ITagsStoreDefault(ITagApplication.context);
            if (store.isDisconnectAlert() || preferences.getBoolean("wt", false)) {
                ITagsService.start(context);
            }
        }
    }
}
