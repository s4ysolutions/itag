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
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import solutions.s4y.itag.BuildConfig;
import solutions.s4y.itag.MainActivity;
import solutions.s4y.itag.R;
import solutions.s4y.itag.history.HistoryRecord;


public class ITagsService extends Service implements ITagGatt.ITagChangeListener, ITagsDb.DbListener {
    private static final int FOREGROUND_ID = 1;
    private static final String CHANNEL_ID = "itag3";
    private static final String CHANNEL_DISCONNECT_ID = "ditag1";
    private static final String RUN_IN_FOREGROUND = "run_in_foreground";
    private static final String STOP_SOUND = "stop_sound";
    private boolean mChannelCreated;
    private boolean mChannelDisconnectedCreated;

    private static final String LT = ITagsService.class.getName();

    @NonNull
    private HashMap<String, ITagGatt> mGatts = new HashMap<>(4);

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
            Intent intentActivity = new Intent(this, MainActivity.class);
            startActivity(intentActivity);
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // connectAll();
        return mBinder;
    }

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
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "onDestroy");
        }

        this.unregisterReceiver(mBluetoothReceiver);
        ITagGatt.removeOnITagChangeListener(this);
        ITagsDb.removeListener(this);
        for (ITagGatt gatt : mGatts.values()) {
            gatt.disconnect();
        }

        super.onDestroy();
    }

    @NotNull
    public ITagGatt getGatt(@NotNull final String addr, boolean connect) {
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
        if (ITagsDb.getDevices(context).size() == 0) {
            context.stopService(new Intent(context, ITagsService.class));
        }
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

    @Override
    public void onITagChange(@NotNull ITagGatt gatt) {
        ITagDevice device = ITagsDb.findByAddr(gatt.mAddr);
        // Sounds if disconnected
        if (gatt.isError() && device != null) {

            HistoryRecord.add(ITagsService.this, gatt.mAddr);
            if (device.linked) {
                MediaPlayerUtils.getInstance().startSoundDisconnected(this, gatt.mAddr);
                createNotificationChannel();
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
                builder
                        .setTicker(String.format(getString(R.string.notify_disconnect),
                                device.name == null || "".equals(device.name) ? "iTag" : device.name))
                        .setSmallIcon(R.drawable.noalert)
                        .setContentTitle(String.format(getString(R.string.notify_disconnect), device.name))
                        .setContentText(getString(R.string.click_to_silent))
                        .setPriority(Notification.PRIORITY_MAX)
                        .setAutoCancel(true);
                Intent intent = new Intent(this, ITagsService.class);
                intent.putExtra(STOP_SOUND, true);
                PendingIntent pendingIntent = PendingIntent.getService(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(pendingIntent);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createDisconnectNotificationChannel();
                    builder.setChannelId(CHANNEL_DISCONNECT_ID);
                }
                Notification notification = builder.build();
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.notify(0, notification);
                }
            }
        } else {
            if (gatt.mIsConnected) {
                HistoryRecord.clear(this, gatt.mAddr);
            }
            if (MediaPlayerUtils.getInstance().isSound(gatt.mAddr)) {
                MediaPlayerUtils.getInstance().stopSound(this);
            }
        }
    }

    @Override
    public void onITagClicked(@NotNull ITagGatt gatt) {
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
        MediaPlayerUtils.getInstance().startFindPhone(this, gatt.mAddr);
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

}
