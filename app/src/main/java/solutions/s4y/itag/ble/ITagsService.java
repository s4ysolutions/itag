package solutions.s4y.itag.ble;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;

import solutions.s4y.itag.BuildConfig;
import solutions.s4y.itag.ITagApplication;
import solutions.s4y.itag.MainActivity;
import solutions.s4y.itag.R;

public class ITagsService extends Service implements ITagGatt.ITagChangeListener, ITagsDb.DbListener {
    private static final int FOREGROUND_ID = 1;
    private static final String CHANNEL_ID = "itag1";
    private static final String RUN_IN_FOREGROUND = "run_in_foreground";
    private boolean mChannelCreated;

    private static final String T = ITagsService.class.getName();

    private HashMap<String, ITagGatt> mGatts = new HashMap<>(4);

    public class GattBinder extends Binder {
        public ITagsService getService() {
            return ITagsService.this;
        }
    }

    private IBinder mBinder = new GattBinder();

    public ITagsService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getBooleanExtra(RUN_IN_FOREGROUND, false)) {
            addToForeground();
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        connect();
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Log.d(T, "onCreate");
        }
        ITagGatt.addOnITagChangeListener(this);
        ITagsDb.addListener(this);
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(T, "onDestroy");
        }
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
            if (connect) gatt.connect(this);
        }
        return gatt;
    }

    public void connect() {
        for (ITagDevice device : ITagsDb.getDevices(this)) {
            getGatt(device.addr, true);
        }
    }

    public void addToForeground() {
        Notification.Builder builder = new Notification.Builder(this);
        builder
                .setSmallIcon(R.drawable.app)
                .setContentTitle(getString(R.string.service_in_background));
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
//            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            //          channel.setDescription(description);
            channel.setSound(null, null);
            channel.setShowBadge(false);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                mChannelCreated = true;
            }
        }
    }

    public static boolean start(Context context, boolean foreground) {
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

    public static boolean start(Context context) {
        return start(context, false);
    }

    public static void startInForeground(Context context) {
        start(context, true);
    }

    public static void stop(Context context) {
        if (ITagsDb.getDevices(context).size() == 0) {
            context.stopService(new Intent(context, ITagsService.class));
        }
    }

    private final MediaPlayer mPlayerButton = new MediaPlayer();

    @Override
    public void onITagChange(@NotNull ITagGatt gatt) {
        ITagDevice device = ITagsDb.findByAddr(gatt.mAddr);
        if (gatt.isError() && device != null && device.linked) {
            AssetFileDescriptor afd = null;
            try {
                afd = getAssets().openFd("lost.mp3");
                mPlayerButton.reset();
                mPlayerButton.setLooping(true);
                mPlayerButton.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                mPlayerButton.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayerButton.prepare();
                mPlayerButton.start();
            } catch (IOException e) {
                ITagApplication.handleError(e);
            } finally {
                if (afd != null) {
                    try {
                        afd.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            mPlayerButton.stop();
            mPlayerButton.reset();
        }
    }

    @Override
    public void onITagClicked(@NotNull ITagGatt gatt) {
        if (gatt.isAlert()) {
            gatt.stopAlert();
        } else if (isSound()) {
            stopSound();
        }
    }


    @Override
    public void onITagDoubleClicked(@NonNull ITagGatt gatt) {
        if (gatt.isAlert()) {
            gatt.stopAlert();
        } else if (isSound()) {
            stopSound();
        } else {
            AssetFileDescriptor afd = null;
            try {
                afd = getAssets().openFd("alarm.mp3");
                mPlayerButton.reset();
                mPlayerButton.setLooping(false);
                mPlayerButton.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                mPlayerButton.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayerButton.prepare();
                mPlayerButton.start();
            } catch (IOException e) {
                ITagApplication.handleError(e);
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
    }


    @Override
    public void onITagRssi(@NonNull ITagGatt gatt, int rssi) {

    }

    public boolean isSound() {
        return mPlayerButton.isPlaying();
    }

    public void stopSound() {
        mPlayerButton.stop();
        mPlayerButton.reset();
    }

    @Override
    public void onDbChange() {

    }

    @Override
    public void onDbAdd(ITagDevice device) {
        getGatt(device.addr, true);
    }

    @Override
    public void onDbRemove(ITagDevice device) {
        ITagGatt toRemove = mGatts.get(device.addr);
        if (toRemove != null) {
            toRemove.disconnect();
        }
        mGatts.remove(device.addr);
    }

}
