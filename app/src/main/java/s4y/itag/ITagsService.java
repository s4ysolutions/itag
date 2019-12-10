package s4y.itag;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import s4y.itag.itag.ITag;
import s4y.waytoday.locations.LocationsTracker;

import static s4y.itag.Notifications.EXTRA_STOP_SOUND;


public class ITagsService extends Service {
    private static final int FOREGROUND_ID = 1;
    static final String FOREGROUND_CHANNEL_ID = "itag3";

    private static final String LT = ITagsService.class.getName();

    class ITagBinder extends Binder {
        void removeFromForeground() {
            ITagsService.this.removeFromForeground();
        }
    }

    final private IBinder mBinder = new ITagBinder();

    static Intent intent(Context context) {
        return new Intent(context, ITagsService.class);
    }

    public static void start(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent(context));
        } else {
            context.startService(intent(context));
        }
    }

    public static void stop(@NonNull Context context) {
        context.stopService(intent(context));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Log.d(LT, "onCreate");
        }

    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "onDestroy");
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private boolean inForeground = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!inForeground) {
            putInForeground();
        }
        if (intent != null) {
            if (intent.getBooleanExtra(EXTRA_STOP_SOUND, false)) {
                MediaPlayerUtils.getInstance().stopSound(this);
            }
            /*
            boolean showActivity = intent.getBooleanExtra(EXTRA_SHOW_ACTIVITY, false);
            if (showActivity && !MainActivity.sIsShown) {
                Intent intentActivity = new Intent(this, MainActivity.class);
                intentActivity.addFlags(FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentActivity);
            }
             */

        }
        return START_REDELIVER_INTENT;
    }

    public void putInForeground() {
        if (inForeground) {
            return;
        }
        inForeground = true;
        startForeground(FOREGROUND_ID, createForegroundNotification(this));
    }

    public void removeFromForeground() {
        stopForeground(true);
        inForeground = false;
    }


    private static boolean createdForegroundChannel;

    private static void createForegroundNotificationChannel(Context context) {
        if (!createdForegroundChannel && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel channel = new NotificationChannel(FOREGROUND_CHANNEL_ID, name, importance);
            channel.setSound(null, null);
            channel.setShowBadge(false);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                createdForegroundChannel = true;
            }
        }
    }

    static Notification createForegroundNotification(Context context) {
        createForegroundNotificationChannel(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID);
        builder
                .setTicker(null)
                .setSmallIcon(R.drawable.app)
                .setContentTitle(context.getString(R.string.service_in_background))
                .setContentText(context.getString(R.string.service_description));
        Intent intent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(pendingIntent);
        return builder.build();
    }
}
