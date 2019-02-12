package solutions.s4y.itag.ble;

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
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import solutions.s4y.itag.BuildConfig;
import solutions.s4y.itag.ITagApplication;
import solutions.s4y.itag.MainActivity;
import solutions.s4y.itag.R;
import solutions.s4y.itag.history.HistoryRecord;
import solutions.s4y.waytoday.idservice.IDService;
import solutions.s4y.waytoday.locations.LocationsGPSUpdater;
import solutions.s4y.waytoday.locations.LocationsTracker;
import solutions.s4y.waytoday.locations.LocationsUpdater;
import solutions.s4y.waytoday.upload.UploadJobService;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;


public class ITagsService extends Service implements
        ITagGatt.ITagChangeListener,
        ITagsDb.DbListener,
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
    private HashMap<String, ITagGatt> mGatts = new HashMap<>(4);

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
                    new Handler().postDelayed(() -> connectAll(), 1000);
                } else if (bluetoothState == BluetoothAdapter.STATE_OFF) {
                    if (BuildConfig.DEBUG) {
                        Log.d(LT, "ACTION_STATE_CHANGED STATE_OFF");
                    }
                    for (ITagGatt gatt : mGatts.values()) {
                        if (gatt != null)
                            gatt.disconnect(true);
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
        ITagGatt.addOnITagChangeListener(this);
        ITagsDb.addListener(this);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(mBluetoothReceiver, filter);
        connectAll();
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
        ITagGatt.removeOnITagChangeListener(this);
        ITagsDb.removeListener(this);
        for (ITagGatt gatt : mGatts.values()) {
            gatt.disconnect();
        }

        LocationsTracker.stop();
        super.onDestroy();
    }

    @NonNull
    public ITagGatt getGatt(@NonNull final String addr, boolean connect) {
        ITagGatt gatt = mGatts.get(addr);
        if (gatt == null) {
            gatt = new ITagGatt(addr);
            mGatts.put(addr, gatt);

        }
        if (connect) {
            if (!gatt.isConnected() && !gatt.isConnecting()) {
                gatt.connect(this);
            }
        }
        return gatt;
    }

    public void connectAll() {
        for (ITagDevice device : ITagsDb.getDevices(this)) {
            getGatt(device.addr, true);
        }
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
        if (ITagsDb.getDevices(context).size() > 0) {
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

    private class Disconnection {
        final ITagGatt gatt;
        final String name;
        final long ts;

        public Disconnection(final ITagGatt gatt, final ITagDevice device) {
            this.gatt = gatt;
            this.name = device.name;
            ts = System.currentTimeMillis();
        }
    }

    private Map<String, Disconnection> mDisconnections = new HashMap<>(4);

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

    @Override
    public void onITagChange(@NonNull ITagGatt gatt) {
        ITagDevice device = ITagsDb.findByAddr(gatt.mAddr);
        // Sounds if disconnected
        if (gatt.isError() && device != null) {
            HistoryRecord.add(ITagsService.this, gatt);
            if (device.linked) {
                MediaPlayerUtils.getInstance().startSoundDisconnected(this, gatt);
            }
            mDisconnections.put(gatt.mAddr, new Disconnection(gatt, device));
        } else {
            if (gatt.mIsConnected) {
                HistoryRecord.clear(this, gatt.mAddr);
                Disconnection disconnection = mDisconnections.get(gatt.mAddr);
                if (disconnection != null) {
                    mDisconnections.remove(gatt.mAddr);
                    long duration = System.currentTimeMillis() - disconnection.ts;
                    if (duration < 5000) {
                        ITagApplication.faSuspiciousDisconnect5();
                    } else if (duration < 10000) {
                        ITagApplication.faSuspiciousDisconnect10();
                    } else if (duration < 30000) {
                        ITagApplication.faSuspiciousDisconnect30();
                    } else {
                        ITagApplication.faSuspiciousDisconnectLong();
                    }
                }
            }
        }
        if (mDisconnections.size() <= 0) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(NOTIFICATION_DISCONNECT_ID);
            }
            MediaPlayerUtils.getInstance().stopSound(this);
        } else {
            String name = null;
            long ts = 0;

            for (Disconnection disconnection : mDisconnections.values()) {
                if (disconnection.ts > ts) name = disconnection.name;
            }
            if (name != null)
                createDisconnectNotification(name);
        }
    }

    @Override
    public void onITagClicked(@NonNull ITagGatt gatt) {
        if (gatt.isFindingITag()) {
            gatt.stopFindITag();
        }
        gatt.stopFindPhone();
        MediaPlayerUtils.getInstance().stopSound(this);
    }

    @Override
    public void onITagDoubleClicked(@NonNull ITagGatt gatt) {
        if (gatt.isFindingITag()) {
            gatt.stopFindITag();
        }
        gatt.startFindPhone();
        MediaPlayerUtils.getInstance().startFindPhone(this, gatt);
    }

    @Override
    public void onITagFindingPhone(@NonNull ITagGatt gatt, boolean on) {

    }

    @Override
    public void onITagRssi(@NonNull ITagGatt gatt, int rssi) {

    }

    @Override
    public void onDbChange() {

    }

    @Override
    public void onDbAdd(@NonNull ITagDevice device) {
        getGatt(device.addr, true);
    }

    @Override
    public void onDbRemove(@NonNull ITagDevice device) {
        ITagGatt toRemove = mGatts.get(device.addr);
        if (toRemove != null) {
            if (toRemove.isConnected() || toRemove.isConnecting()) {
                toRemove.disconnect();
            }
        }
        mGatts.remove(device.addr);
    }

    public void startWayToday(int frequency) {
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putBoolean("wt", true).apply();
        preferences.edit().putInt("freq", frequency).apply();
        String tid = preferences.getString("tid", null);
        Handler handler = new Handler();
        if (tid == null) {
            ITagApplication.faWtNoTrackID();
            final IDService.IIDSeriviceListener listener = new IDService.IIDSeriviceListener() {
                @Override
                public void onTrackID(@NonNull String trackID) {
                    IDService.removeOnTrackIDChangeListener(this);
                    if (!"".equals(trackID)) {
                        // duplicate of ITagsService.onTrackID
                        preferences.edit().putString("tid", tid).apply();
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
