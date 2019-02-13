package solutions.s4y.waytoday.idservice;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.JobIntentService;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import solutions.s4y.waytoday.errors.ErrorsObservable;
import solutions.s4y.waytoday.grpc.GRPCChannelProvider;
import solutions.s4y.waytoday.grpc.Keys;
import solutions.s4y.waytoday.grpc.TrackerGrpc;
import solutions.s4y.waytoday.grpc.TrackerOuterClass;
import solutions.s4y.waytoday.wsse.Wsse;


public class IDService extends JobIntentService {
    @VisibleForTesting
    public static final List<IIDSeriviceListener> sListeners =
            new ArrayList<>(2);
    private static final String EXTRA_PREVID = "previd";
    public static boolean sFailed = false;
    static private boolean sProgress = false;
    public GRPCChannelProvider grpcChannelProvider = GRPCChannelProvider.getInstance();
    protected ManagedChannel ch = null;

    static synchronized public boolean isProgress() {
        return sProgress;
    }

    static synchronized public void setProgress(boolean progress) {
        sProgress = progress;
    }

    public static void enqueueRetrieveId(Context context, String prevID) {
        if (!isProgress()) {
            Intent intent = new Intent(context, IDService.class);
            intent.putExtra(EXTRA_PREVID, prevID);
            enqueueWork(context, IDService.class, 1000, intent);
        }
    }

    public static void enqueueRetrieveId(Context context) {
        if (!isProgress()) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            String prevID = sp.getString("tid", "");
            enqueueRetrieveId(context, prevID);
        }
    }

    public static void addOnTrackIDChangeListener(IIDSeriviceListener listener) {
        sListeners.add(listener);
    }

    public static void removeOnTrackIDChangeListener(IIDSeriviceListener listener) {
        sListeners.remove(listener);
    }

    private static void notifyTrack(@NonNull String trackID) {
        for (IIDSeriviceListener listener : new ArrayList<>(sListeners)) {
            listener.onTrackID(trackID);
        }
    }

    private void reportFail(Throwable e) {
        ErrorsObservable.notify(e);
    }

    protected boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnected();
    }

    @VisibleForTesting()
    public TrackerGrpc.TrackerBlockingStub getGrpcStub() {
        return TrackerGrpc.newBlockingStub(ch);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        setProgress(true);
        sFailed = false;
        if (isConnected()) {
            String id = intent.getStringExtra(EXTRA_PREVID);

            try {
                // intent service can be handle work few times
                // without destory, te use the channel then
                if (ch == null)
                    ch = grpcChannelProvider.channel();
                TrackerGrpc.TrackerBlockingStub grpcStub = getGrpcStub();

                Metadata headers = new Metadata();
                Metadata.Key<String> key = Keys.wsseKey;
                String token = Wsse.getToken();
                headers.put(key, token);
                grpcStub = MetadataUtils.attachHeaders(grpcStub, headers);

                TrackerOuterClass.GenerateTrackerIDRequest req = TrackerOuterClass.
                        GenerateTrackerIDRequest.
                        newBuilder()
                        .setPrevTid(id == null ? "" : id)
                        .build();

                TrackerOuterClass.GenerateTrackerIDResponse response = grpcStub.generateTrackerID(req);
                final String tid = response.getTid();
                notifyTrack(tid);
            } catch (final Exception e) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                String oldtid = preferences.getString("tid", null);
                sFailed = oldtid != null;
                reportFail(e);
                notifyTrack("");
            }
        } else {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            String oldtid = preferences.getString("tid", null);
            sFailed = oldtid != null;
            notifyTrack("");
            // TODO: handle the case
        }
        setProgress(false);
    }

    @VisibleForTesting()
    public void destoryGrpcChannel() {
        if (ch != null) {
            try {
                ManagedChannel tmp = ch;
                ch = null;
                tmp.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        destoryGrpcChannel();
        super.onDestroy();
    }

    public interface IIDSeriviceListener {
        void onTrackID(@NonNull String trackID);
    }

}
