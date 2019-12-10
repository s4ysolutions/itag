package s4y.itag;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.preference.PreferenceManager;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import s4y.itag.itag.ITag;
import s4y.waytoday.idservice.IDService;
import s4y.waytoday.locations.LocationsGPSUpdater;
import s4y.waytoday.locations.LocationsTracker;
import s4y.waytoday.locations.LocationsUpdater;
import s4y.waytoday.upload.UploadJobService;

import static s4y.itag.Notifications.EXTRA_STOP_SOUND;


public class ITagsService extends Service implements
        LocationsTracker.ILocationListener,
        IDService.IIDSeriviceListener {
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
        if (ITag.store.count() > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent(context));
            } else {
                context.startService(intent(context));
            }
        } else {
            stop(context);
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

        gpsLocatonUpdater = new LocationsGPSUpdater(this);
        LocationsTracker.addOnLocationListener(this);
        IDService.addOnTrackIDChangeListener(this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean wt = preferences.getBoolean("wt", false);
        int freq = preferences.getInt("freq", 0);
        if (wt && freq > 0) {
            startWayToday(freq);
        }
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "onDestroy");
        }
        LocationsTracker.removeOnLocationListener(this);

        LocationsTracker.stop();
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


    @Override
    public void onLocation(@NonNull Location location) {
        UploadJobService.enqueueUploadLocation(this, location);
    }

    @Override
    public void onTrackID(@NonNull String trackID) {
        if ("".equals(trackID))
            return;

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.edit().putString("tid", trackID).apply();
        sp.edit().putBoolean("wtfirst", false).apply();
    }

    private LocationsUpdater gpsLocatonUpdater;

    public void startWayToday(int frequency) {
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putBoolean("wt", true).apply();
        preferences.edit().putInt("freq", frequency).apply();
        final String tid = preferences.getString("tid", null);
        Handler handler = new Handler();
        if (tid == null) {
            ITagApplication.faWtNoTrackID();
            final IDService.IIDSeriviceListener listener = new IDService.IIDSeriviceListener() {
                @Override
                public void onTrackID(@NonNull String trackID) {
                    IDService.removeOnTrackIDChangeListener(this);
                    if (!"".equals(trackID)) {
                        // duplicate of ITagsService.onTrackID
                        preferences.edit().putString("tid", trackID).apply();
                        handler.post(() ->
                                LocationsTracker.requestStart(gpsLocatonUpdater, frequency));
                    }
                }
            };
            IDService.addOnTrackIDChangeListener(listener);
            IDService.enqueueRetrieveId(this, "");
        } else {
            LocationsTracker.requestStart(gpsLocatonUpdater, frequency);
        }
    }

    @SuppressWarnings("unused")
    public void stopWayToday() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putBoolean("wt", false).apply();
        LocationsTracker.stop();
    }

}
