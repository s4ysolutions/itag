package s4y.itag;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import s4y.itag.itag.ITagsStoreDefault;
import s4y.itag.itag.ITagsStoreInterface;
import s4y.itag.waytoday.Waytoday;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (
                Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                        ||
                        "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())
        ) {
            ITagsStoreInterface store = new ITagsStoreDefault(ITagApplication.context);
            if (store.isDisconnectAlertOn() || Waytoday.tracker.isOn(context)) {
                ITagsService.start(context); // expected to create application and thus start waytooday
            }
        }
    }
}
