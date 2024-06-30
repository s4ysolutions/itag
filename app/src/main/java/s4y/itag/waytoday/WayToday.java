package s4y.itag.waytoday;

import android.content.Context;
import android.os.Build;

import kotlin.Unit;
import s4y.gps.sdk.android.GPSUpdatesForegroundService;
import s4y.gps.sdk.dependencies.IGPSUpdatesProvider;
import solutions.s4y.rasat.Channel;
import solutions.s4y.rasat.Observable;
import solutions.s4y.waytoday.sdk.AndroidWayTodayClient;
public class WayToday {
    private static AndroidWayTodayClient instance;
    private static final Channel<String> trackIdChannel = new Channel<>();
    private static final Channel<IGPSUpdatesProvider.Status> gpsStatusChannel = new Channel<>();

    public static void init(Context context) {
        if (instance != null) {
            throw new IllegalStateException("WayToday is already initialized");
        }

        instance = new AndroidWayTodayClient(context, "iTagAndroid", "secret",  "iTagAndroid");
        instance.wtClient.addTrackIdChangeListener(trackIdChannel::broadcast);

        instance.gpsUpdatesManager.getStatus().addListener(status -> {
            gpsStatusChannel.broadcast(status);
            // TODO: ugly
            return Unit.INSTANCE;
        });

        GPSUpdatesForegroundService.setUpdatesManager(instance.gpsUpdatesManager);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            GPSUpdatesForegroundService.setNotificationChannelId("itag_gps_updates");
            GPSUpdatesForegroundService.setNotificationChannelName("ITag GPS Updates");
            // GPSUpdatesForegroundService.setNotificationId(22);
            GPSUpdatesForegroundService.setNotificationContentTitle("Stop iTag WayToday tracking");
            // GPSUpdatesForegroundService.setUseApplicationNotificationSmallIcon(true);
        }
    }
    public static AndroidWayTodayClient getInstance() {
        if (instance == null) {
            throw new IllegalStateException("WayToday is not initialized, call init() first");
        }
        return instance;
    }

    public static Observable<String> getTrackIdObservable() {
        return trackIdChannel.observable;
    }

    public static Observable<IGPSUpdatesProvider.Status> getGpsStatusObservable() {
        return gpsStatusChannel.observable;
    }
}
