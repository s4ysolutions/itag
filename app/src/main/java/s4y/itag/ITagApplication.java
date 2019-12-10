package s4y.itag;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;

import androidx.annotation.NonNull;

import s4y.itag.itag.ITag;
import s4y.itag.waytoday.Waytoday;

public final class ITagApplication extends Application {
    private final static String LT = ITagApplication.class.getName();
    // context is not used outside Application so there's a hope there will be no memory leak
    @SuppressLint("StaticFieldLeak")
    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        faAppCreated();
        context = this;
        ITag.initITag(context);
        Waytoday.init();
    }

    @Override
    public void onTerminate() {
        Waytoday.done();
        try {
            ITag.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onTerminate();
    }

    static public void handleError(@NonNull Throwable th, boolean toast) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (context == null) {
                Log.e(LT, "Attempt to handle error before application created", th);
                Crashlytics.logException(th);
            } else {
                Log.e(LT, "Toasted", th);
                if (toast) {
                    Toast.makeText(context, th.getMessage(), Toast.LENGTH_LONG).show();
                }
                Crashlytics.logException(th);
            }
        });
    }

    static public void handleError(@NonNull Throwable th, int toast) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (context == null) {
                Log.e(LT, "Attempt to handle error before application created", th);
                Crashlytics.logException(th);
            } else {
                Log.e(LT, "Toasted", th);
                Toast.makeText(context, toast, Toast.LENGTH_LONG).show();
                Crashlytics.logException(th);
            }
        });
    }

    static public void handleError(@NonNull Throwable th) {
        handleError(th, BuildConfig.DEBUG);
    }

    private static FirebaseAnalytics sFirebaseAnalytics;

    static public void fa(@NonNull final String event, Bundle bundle) {
        if (context == null) return;
        if (sFirebaseAnalytics == null) {
            sFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        }
        sFirebaseAnalytics.logEvent(event, bundle);
        if (BuildConfig.DEBUG) {
            Log.d(LT, "FA log " + event);
        }
    }

    static public void fa(@NonNull final String event) {
        fa(event, null);
    }

    static public void faAppCreated() {
        fa("itag_app_created");
    }

    static public void faNoBluetooth() {
        fa("itag_no_bluetooth");
    }

    static public void faBluetoothDisable() {
        fa("itag_bluetooth_disable");
    }

    static public void faNotITag() {
        fa("itag_not_itag");
    }

    static public void faScanView(boolean empty) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "itag_scan_view_is_empty");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "First Scan");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "boolean");
        bundle.putBoolean(FirebaseAnalytics.Param.VALUE, empty);
        fa("itag_scan_view", bundle);
    }

    static public void faITagsView(int devices) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "itag_itags_view_device");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Remembered Devices");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "int");
        bundle.putInt(FirebaseAnalytics.Param.VALUE, devices);
        fa("itag_itags_view");
    }

    static public void faSuspiciousDisconnect5() {
        fa("itag_suspicious_disconnect_5");
    }

    static public void faSuspiciousDisconnect10() {
        fa("itag_suspicious_disconnect_10");
    }

    static public void faSuspiciousDisconnect30() {
        fa("itag_suspicious_disconnect_30");
    }

    static public void faSuspiciousDisconnectLong() {
        fa("itag_suspicious_disconnect_long");
    }

    static public void faRememberITag() {
        fa("itag_remember_itag");
    }

    static public void faForgetITag() {
        fa("itag_forget_itag");
    }

    static public void faNameITag() {
        fa("itag_set_name");
    }

    static public void faColorITag() {
        fa("itag_set_color");
    }

    static public void faMuteTag() {
        fa("itag_mute");
    }

    static public void faUnmuteTag() {
        fa("itag_unmute");
    }

    static public void faFindITag() {
        fa("itag_find_itag");
    }

    static public void faITagFound() {
        fa("itag_itag_found");
    }

    static public void faITagFindStopped() {
        fa("itag_itag_find_stopped");
    }

    static public void faITagLost(boolean error) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "itag_itag_lost_error");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Lost with error");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "boolean");
        bundle.putBoolean(FirebaseAnalytics.Param.VALUE, error);
        fa("itag_itag_lost");
    }

    static public void faFindPhone() {
        fa("itag_find_phone");
    }

    static public void faITagDisconnected() {
        fa("itag_user_disconnect");
    }

    static public void faITagConnected() {
        fa("itag_user_connect");
    }

    static public void faShowLastLocation() {
        fa("itag_show_last_location");
    }

    static public void faIssuedGpsRequest() {
        fa("itag_issued_gps_request");
    }
    static public void faRemovedGpsRequestBySuccess() {
        fa("itag_removed_gps_request_by_success");
    }
    static public void faRemovedGpsRequestByConnect() {
        fa("itag_removed_gps_request_by_connect");
    }
    static public void faRemovedGpsRequestByTimeout() {
        fa("itag_removed_gps_request_by_timeout");
    }
    static public void faGotGpsLocation() {
        fa("itag_got_gps_location");
    }
    static public void faGpsPermissionError() {
        fa("itag_gps_permission_error");
    }
    static public void faDisconnectDuringCall() {
        fa("itag_disconnect_during_call");
    }

    static public void faWtOff() {
        fa("itag_wt_off");
    }

    static public void faWtOn1() {
        fa("itag_wt_on1");
    }

    static public void faWtNoTrackID() {
        fa("itag_wt_no_track_id");
    }

    static public void faWtOn300() {
        fa("itag_wt_on300");
    }

    static public void faWtChangeID() {
        fa("itag_wt_change_id");
    }

    static public void faWtShare() {
        fa("itag_wt_share");
    }

    static public void faWtAbout() {
        fa("itag_wt_about");
    }

    static public void faWtVisit() {
        fa("itag_wt_visit");
    }

    static public void faWtPlaymarket() {
        fa("itag_wt_playmarket");
    }

    static public void faWtRemove() {
        fa("itag_wt_remove");
    }
}
