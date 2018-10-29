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
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import solutions.s4y.itag.BuildConfig;
import solutions.s4y.itag.ITagApplication;
import solutions.s4y.itag.MainActivity;
import solutions.s4y.itag.R;


public class ITagsService extends Service implements ITagGatt.ITagChangeListener, ITagsDb.DbListener {
    private static final int FOREGROUND_ID = 1;
    private static final String CHANNEL_ID = "itag3";
    private static final String CHANNEL_DISCONNECT_ID = "ditag1";
    private static final String RUN_IN_FOREGROUND = "run_in_foreground";
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
                    new Handler().postDelayed(() -> connectAll(),1000);
                } else if (bluetoothState == BluetoothAdapter.STATE_OFF) {
                    if (BuildConfig.DEBUG) {
                        Log.d(LT, "ACTION_STATE_CHANGED STATE_OFF");
                    }
                    for (ITagGatt gatt : mGatts.values()) {
                        if (gatt != null)
                            gatt.disconnect();
                    }
                }
            }
        }
    };

    public ITagsService() {
    }

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        if (intent.getBooleanExtra(RUN_IN_FOREGROUND, false)) {
            addToForeground();
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
            gatt.close();
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
        Notification.Builder builder = new Notification.Builder(this);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            builder.setChannelId(CHANNEL_ID);
        }
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

    private final MediaPlayer mPlayer = new MediaPlayer();
    @NonNull
    Set<String> mSoundingITags = new HashSet<>(4);

    public void stopSound() {
        mSoundingITags.clear();
        mPlayer.stop();
        mPlayer.reset();
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

    private void startSoundDisconnected(String addr) {
        stopSound();
        AssetFileDescriptor afd = null;
        try {
            afd = getAssets().openFd("lost.mp3");
            mPlayer.reset();
            mPlayer.setLooping(true);
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.prepare();
            mPlayer.start();
            mSoundingITags.add(addr);
        } catch (IOException e) {
            ITagApplication.handleError(e, true);
        } finally {
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startFindPhone(String addr) {
        AssetFileDescriptor afd = null;
        stopSound();
        try {
            afd = getAssets().openFd("alarm.mp3");
            mPlayer.reset();
            mPlayer.setLooping(false);
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.prepare();
            mPlayer.start();
            mSoundingITags.add(addr);
        } catch (IOException e) {
            ITagApplication.handleError(e, true);
        } finally {
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onITagChange(@NotNull ITagGatt gatt) {
        ITagDevice device = ITagsDb.findByAddr(gatt.mAddr);
        // Sounds if disconnected
        if (gatt.isError() && device != null && device.linked) {
            startSoundDisconnected(gatt.mAddr);
            Notification.Builder builder = new Notification.Builder(this);
            builder
                    .setTicker(String.format(getString(R.string.notify_disconnect),
                            device.name == null || "".equals(device.name) ? "iTag" : device.name))
                    .setSmallIcon(R.drawable.app)
                    .setContentTitle(String.format(getString(R.string.notify_disconnect), device.name))
                    .setContentText(getString(R.string.click_to_silent))
                    .setAutoCancel(true);
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createDisconnectNotificationChannel();
                builder.setChannelId(CHANNEL_DISCONNECT_ID);
            }
            Notification notification = builder.build();
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(0, notification);
            }
        } else {
            if (isSound(gatt.mAddr)) {
                stopSound();
            }
        }
    }

    @Override
    public void onITagClicked(@NotNull ITagGatt gatt) {
        if (gatt.isFindingITag()) {
            gatt.stopFindITag();
        }
        gatt.stopFindPhone();
        stopSound();
    }

    @Override
    public void onITagDoubleClicked(@NonNull ITagGatt gatt) {
        if (gatt.isFindingITag()) {
            gatt.stopFindITag();
        }
        gatt.startFindPhone();
        startFindPhone(gatt.mAddr);
    }

    @Override
    public void onITagFindingPhone(@NonNull ITagGatt gatt, boolean on) {

    }

    @Override
    public void onITagRssi(@NonNull ITagGatt gatt, int rssi) {

    }

    public boolean isSound(String addr) {
        return mPlayer.isPlaying() && mSoundingITags.contains(addr);
    }

    public boolean isSound() {
        return mPlayer.isPlaying();
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
