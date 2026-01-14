package s4y.itag;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class Notifications {
    private static final int NOTIFICATION_DISCONNECT_ID = 2;
    private static final int NOTIFICATION_CONNECT_ID = 3;
    private static final String CHANNEL_DISCONNECT_ID = "ditag1";
    private static final String CHANNEL_CONNECT_ID = "ditag2";
    static final String EXTRA_STOP_SOUND = "stop_sound";
    private static boolean createdChannelDisconnected;
    private static boolean createdChannelConnected;

    private static void createDisconnectNotificationChannel() {
        if (!createdChannelDisconnected && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = ITagApplication.context.getString(R.string.channel_disconnect);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_DISCONNECT_ID, name, importance);
            channel.setSound(null, null);
            channel.setShowBadge(false);
            channel.enableVibration(true);
            NotificationManager notificationManager = ITagApplication.context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                createdChannelDisconnected = true;
            }
        }
    }

    private static void createConnectNotificationChannel() {
        if (!createdChannelConnected && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = ITagApplication.context.getString(R.string.channel_connect);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_CONNECT_ID, name, importance);
            channel.setSound(null, null);
            channel.setShowBadge(false);
            channel.enableVibration(true);
            NotificationManager notificationManager = ITagApplication.context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                createdChannelConnected = true;
            }
        }
    }

    // TODO: i do not understand what's is the reason of the warning to supress
    @SuppressLint("LaunchActivityFromNotification")
    public static void sendDisconnectNotification(Context context, String name) {
        createDisconnectNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ITagsService.FOREGROUND_CHANNEL_ID);
        builder
                .setTicker(String.format(context.getString(R.string.notify_disconnect),
                        name == null || "".equals(name) ? "iTag" : name))
                .setSmallIcon(R.drawable.noalert)
                .setContentTitle(String.format(context.getString(R.string.notify_disconnect), name))
                .setContentText(context.getString(R.string.click_to_silent))
                .setPriority(Notification.PRIORITY_MAX)
                .setAutoCancel(true);

        Intent intent = ITagsService.intentStart(context);
        intent.putExtra(EXTRA_STOP_SOUND, true);
        PendingIntent pendingIntent =
                PendingIntent.getService(context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        builder.setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createDisconnectNotificationChannel();
            builder.setChannelId(CHANNEL_DISCONNECT_ID);
        }
        Notification notification = builder.build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_DISCONNECT_ID, notification);
        }
    }

    public static void sendConnectNotification(Context context, String name) {
        createConnectNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ITagsService.FOREGROUND_CHANNEL_ID);
        builder
                .setTicker(String.format(context.getString(R.string.notify_connect),
                        name == null || "".equals(name) ? "iTag" : name))
                .setSmallIcon(R.drawable.noalert)
                .setContentTitle(String.format(context.getString(R.string.notify_connect), name))
                .setContentText(context.getString(R.string.click_to_silent))
                .setPriority(Notification.PRIORITY_MAX)
                .setAutoCancel(true);

        Intent intent = ITagsService.intentStart(context);
        intent.putExtra(EXTRA_STOP_SOUND, true);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createConnectNotificationChannel();
            builder.setChannelId(CHANNEL_CONNECT_ID);
        }
        Notification notification = builder.build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_CONNECT_ID, notification);
        }
    }

    public static void cancelDisconnectNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_DISCONNECT_ID);
        }
    }

    public static void cancelConnectNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_CONNECT_ID);
        }
    }
}
