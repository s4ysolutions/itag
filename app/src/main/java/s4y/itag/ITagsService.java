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

import s4y.itag.ble.BLEConnectionInterface;
import s4y.itag.itag.ITag;
import s4y.itag.itag.ITagInterface;
import s4y.rasat.DisposableBag;
import s4y.waytoday.idservice.IDService;
import s4y.waytoday.locations.LocationsGPSUpdater;
import s4y.waytoday.locations.LocationsTracker;
import s4y.waytoday.locations.LocationsUpdater;
import s4y.waytoday.upload.UploadJobService;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;


public class ITagsService extends Service implements
        LocationsTracker.ILocationListener,
        IDService.IIDSeriviceListener {
    private static final String LT = ITagsService.class.getName();
    private static final int FOREGROUND_ID = 1;
    private static final int NOTIFICATION_DISCONNECT_ID = 2;
    private static final String MAIN_CHANNEL_ID = "itag3";
    private static final String CHANNEL_DISCONNECT_ID = "ditag1";
    private static final String EXTRA_STOP_SOUND = "stop_sound";
    private static final String EXTRA_SHOW_ACTIVITY = "show_activity";

    private final DisposableBag disposablesStore = new DisposableBag();
    private final DisposableBag disposableDisconnect = new DisposableBag();


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

        subscribeDisconnections();
        disposablesStore.add(ITag.store.observable().subscribe(event -> {
                    disposableDisconnect.dispose();
                    subscribeDisconnections();
                }
        ));

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

    private void subscribeDisconnections() {
        disposableDisconnect.dispose();
        for (int i = 0; i < ITag.store.count(); i++) {
            final ITagInterface itag = ITag.store.byPos(i);
            if (itag != null && itag.isAlertDisconnected()) {
                BLEConnectionInterface connection = ITag.ble.connectionById(itag.id());
                disposableDisconnect.add(connection.observableState().subscribe(event -> {
                            if (itag.isAlertDisconnected() && connection.isDisconnected()) {
                                sendDisconnectNotification(itag.name());
                            } else {
                                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                if (notificationManager != null) {
                                    notificationManager.cancel(NOTIFICATION_DISCONNECT_ID);
                                }
                            }
                        }
                ));
            }
        }
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "onDestroy");
        }
        disposableDisconnect.dispose();
        disposablesStore.dispose();
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
            boolean showActivity = intent.getBooleanExtra(EXTRA_SHOW_ACTIVITY, false);
            if (showActivity && !MainActivity.sIsShown) {
                Intent intentActivity = new Intent(this, MainActivity.class);
                intentActivity.addFlags(FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentActivity);
            }

        }
        return START_REDELIVER_INTENT;
    }

    public void putInForeground() {
        if (inForeground) {
            return;
        }
        inForeground = true;
        createMainNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MAIN_CHANNEL_ID);
        builder
                .setTicker(null)
                .setSmallIcon(R.drawable.app)
                .setContentTitle(getString(R.string.service_in_background))
                .setContentText(getString(R.string.service_description));
        Intent intent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(pendingIntent);
        Notification notification = builder.build();
        startForeground(FOREGROUND_ID, notification);
    }

    public void removeFromForeground() {
        stopForeground(true);
        inForeground = false;
    }

    private boolean createdChannel;

    private void createMainNotificationChannel() {
        if (!createdChannel && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel channel = new NotificationChannel(MAIN_CHANNEL_ID, name, importance);
            channel.setSound(null, null);
            channel.setShowBadge(false);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                createdChannel = true;
            }
        }
    }

    private boolean createdChannelDisconnected;

    private void createDisconnectNotificationChannel() {
        if (!createdChannelDisconnected && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_DISCONNECT_ID, name, importance);
            channel.setSound(null, null);
            channel.setShowBadge(false);
            channel.enableVibration(true);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                createdChannelDisconnected = true;
            }
        }
    }

    private void sendDisconnectNotification(String name) {
        // 1. change icon of them main notification
        createMainNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MAIN_CHANNEL_ID);
        builder
                .setTicker(String.format(getString(R.string.notify_disconnect),
                        name == null || "".equals(name) ? "iTag" : name))
                .setSmallIcon(R.drawable.noalert)
                .setContentTitle(String.format(getString(R.string.notify_disconnect), name))
                .setContentText(getString(R.string.click_to_silent))
                .setPriority(Notification.PRIORITY_MAX)
                .setAutoCancel(true);

        Intent intent = new Intent(this, ITagsService.class);
        intent.putExtra(EXTRA_STOP_SOUND, true);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createDisconnectNotificationChannel();
            builder.setChannelId(CHANNEL_DISCONNECT_ID);
        }
        Notification notification = builder.build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_DISCONNECT_ID, notification);
        }
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

    public void stopWayToday() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putBoolean("wt", false).apply();
        LocationsTracker.stop();
    }

}
