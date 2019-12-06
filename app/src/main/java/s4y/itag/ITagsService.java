package s4y.itag;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.preference.PreferenceManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import s4y.itag.history.HistoryRecord;
import s4y.itag.itag.ITag;
import s4y.waytoday.idservice.IDService;
import s4y.waytoday.locations.LocationsGPSUpdater;
import s4y.waytoday.locations.LocationsTracker;
import s4y.waytoday.locations.LocationsUpdater;
import s4y.waytoday.upload.UploadJobService;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;


class ITagsService extends Service implements
        LocationsTracker.ILocationListener,
        IDService.IIDSeriviceListener {
    private static final int FOREGROUND_ID = 1;
    private static final int NOTIFICATION_DISCONNECT_ID = 2;
    private static final String CHANNEL_ID = "itag3";
    private static final String CHANNEL_DISCONNECT_ID = "ditag1";
    private static final String RUN_IN_FOREGROUND = "run_in_foreground";
    private static final String STOP_SOUND = "stop_sound";
    private boolean mChannelCreated;
    private boolean mChannelDisconnectedCreated;

    private static final String LT = ITagsService.class.getName();

    @NonNull

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

    public class GattBinder extends Binder {
        @NonNull
        public ITagsService getService() {
            return ITagsService.this;
        }
    }

    @NonNull
    private IBinder mBinder = new GattBinder();

    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                Log.d(LT, "bluetooth change state: " + bluetoothState);
                if (bluetoothState == BluetoothAdapter.STATE_ON) {
                    if (BuildConfig.DEBUG) {
                        Log.d(LT, "ACTION_STATE_CHANGED STATE_ON");
                    }
                } else if (bluetoothState == BluetoothAdapter.STATE_OFF) {
                    if (BuildConfig.DEBUG) {
                        Log.d(LT, "ACTION_STATE_CHANGED STATE_OFF");
                    }
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getBooleanExtra(RUN_IN_FOREGROUND, false)) {
            addToForeground();
        }
        if (intent != null && intent.getBooleanExtra(STOP_SOUND, false)) {
            MediaPlayerUtils.getInstance().stopSound(this);
            if (!MainActivity.sIsShown) {
                Intent intentActivity = new Intent(this, MainActivity.class);
                intentActivity.addFlags(FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentActivity);
            }
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // connectAll();
        return mBinder;
    }

    private LocationsUpdater gpsLocatonUpdater;

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Log.d(LT, "onCreate");
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(mBluetoothReceiver, filter);
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
        this.unregisterReceiver(mBluetoothReceiver);

        LocationsTracker.stop();
        super.onDestroy();
    }


    public void addToForeground() {
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
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
    }

    private void createNotificationChannel() {
        if (!mChannelCreated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setSound(null, null);
            channel.setShowBadge(false);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                mChannelCreated = true;
            }
        }
    }

    public static boolean start(@NonNull Context context, boolean foreground) {
        if (ITag.store.count() > 0) {
            Intent intent = new Intent(context, ITagsService.class);
            if (foreground) {
                intent.putExtra(RUN_IN_FOREGROUND, true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent);
                } else {
                    context.startService(intent);
                }
            } else {
                context.startService(intent);
            }
            return true;
        }
        return false;
    }

    public static boolean start(@NonNull Context context) {
        return start(context, false);
    }

    public static void startInForeground(@NonNull Context context) {
        start(context, true);
    }

    public static void stop(@NonNull Context context) {
        context.stopService(new Intent(context, ITagsService.class));
    }

    private void createDisconnectNotificationChannel() {
        if (!mChannelDisconnectedCreated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_DISCONNECT_ID, name, importance);
            channel.setSound(null, null);
            channel.setShowBadge(false);
            channel.enableVibration(true);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                mChannelDisconnectedCreated = true;
            }
        }
    }


    private void createDisconnectNotification(String name) {
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder
                .setTicker(String.format(getString(R.string.notify_disconnect),
                        name == null || "".equals(name) ? "iTag" : name))
                .setSmallIcon(R.drawable.noalert)
                .setContentTitle(String.format(getString(R.string.notify_disconnect), name))
                .setContentText(getString(R.string.click_to_silent))
                .setPriority(Notification.PRIORITY_MAX)
                .setAutoCancel(true);
        Intent intent = new Intent(this, ITagsService.class);
        intent.putExtra(STOP_SOUND, true);
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
