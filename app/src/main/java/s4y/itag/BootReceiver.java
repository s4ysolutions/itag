package s4y.itag;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import s4y.gps.sdk.android.GPSPermissionManager;
import s4y.gps.sdk.android.GPSUpdatesForegroundService;
import s4y.itag.itag.ITagsStoreDefault;
import s4y.itag.itag.ITagsStoreInterface;
import solutions.s4y.waytoday.sdk.AndroidWayTodayClient;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (
                Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                        ||
                        "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())
        ) {
            ITagsStoreInterface store = new ITagsStoreDefault(ITagApplication.context);
            if (store.isDisconnectAlertOn()) {
                ITagsService.start(context);
                // expected to create application and thus init waytooday
                // and enter foreground
            }
            if (AndroidWayTodayClient.isTrackingOn(context) && !GPSPermissionManager.needPermissionRequest(context, true)) {
                GPSUpdatesForegroundService.start(context);
            }
        }
    }
}
