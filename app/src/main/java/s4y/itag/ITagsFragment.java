package s4y.itag;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;

import s4y.itag.ble.BLEConnectionInterface;
import s4y.itag.ble.BLEConnectionState;
import s4y.itag.ble.BLEState;
import s4y.itag.history.HistoryRecord;
import s4y.itag.itag.ITag;
import s4y.itag.itag.ITagInterface;
import s4y.itag.preference.VolumePreference;
import s4y.itag.waytoday.Waytoday;
import solutions.s4y.rasat.DisposableBag;
import solutions.s4y.waytoday.sdk.id.ITrackIDChangeListener;
import solutions.s4y.waytoday.sdk.id.TrackIDJobService;
import solutions.s4y.waytoday.sdk.tracker.ITrackingStateListener;
import solutions.s4y.waytoday.sdk.tracker.TrackerState;

import static s4y.itag.itag.ITag.ble;

public class ITagsFragment extends Fragment
        implements
        HistoryRecord.HistoryRecordListener,
        ITrackingStateListener,
        ITrackIDChangeListener {
    private static final String LT = ITagsFragment.class.getName();
    private Animation mLocationAnimation;
    private Animation mITagAnimation;
    private String trackID = "";
    private final DisposableBag disposableBag = new DisposableBag();

    public ITagsFragment() {
        // Required empty public constructor
    }

    private final Map<String, ViewGroup> tagViews = new HashMap<>();

    private void setupTags(@NonNull ViewGroup root) {
        Activity activity = getActivity();
        if (activity == null) return; //

        View tagsLayout = root.findViewById(R.id.tags);
        int index = -1;
        if (tagsLayout != null) {
            root.removeView(tagsLayout);
            index = root.indexOfChild(tagsLayout);
        }
        final int s = ITag.store.count();
        final int rid = s == 0 ? R.layout.itag_0 : s == 1 ? R.layout.itag_1 : s == 2 ? R.layout.itag_2 : s == 3 ? R.layout.itag_3 : R.layout.itag_4;
        tagsLayout = activity.getLayoutInflater().inflate(rid, root, false);
        root.addView(tagsLayout, index);
        tagViews.clear();

        if (s > 0) {
            ITagInterface itag = ITag.store.byPos(0);
            tagViews.put(itag.id(), root.findViewById(R.id.tag_1).findViewById(R.id.layout_itag));
        }

        if (s > 1) {
            ITagInterface itag = ITag.store.byPos(1);
            tagViews.put(itag.id(), root.findViewById(R.id.tag_2).findViewById(R.id.layout_itag));
        }

        if (s > 2) {
            ITagInterface itag = ITag.store.byPos(2);
            tagViews.put(itag.id(), root.findViewById(R.id.tag_3).findViewById(R.id.layout_itag));
        }

        if (s > 3) {
            ITagInterface itag = ITag.store.byPos(3);
            tagViews.put(itag.id(), root.findViewById(R.id.tag_4).findViewById(R.id.layout_itag));
        }

        for (Map.Entry<String, ViewGroup> entry : tagViews.entrySet()) {
            String id = entry.getKey();
            ViewGroup rootView = entry.getValue();
            ITagInterface itag = ITag.store.byId(id);
            BLEConnectionInterface connection = ble.connectionById(id);
            if (itag != null) {
                setupButtons(rootView, itag);
                updateITagImage(rootView, itag);
                updateITagImageAnimation(rootView, itag, connection);
                updateName(rootView, itag.name());
                updateAlertButton(rootView, itag.isAlertDisconnected(), connection.isConnected());
            }
            updateRSSI(rootView, connection.rssi());
            updateState(rootView, id, connection.state());
            updateLocationImage(rootView, id);
        }

        updateWayToday();
    }

    private void setupButtons(@NonNull ViewGroup rootView, @NonNull final ITagInterface itag) {
        Activity activity = getActivity();
        if (activity == null) return; //
        final View btnForget = rootView.findViewById(R.id.btn_forget);
        btnForget.setTag(itag);
        final View btnColor = rootView.findViewById(R.id.btn_color);
        btnColor.setTag(itag);
        final View btnSetName = rootView.findViewById(R.id.btn_set_name);
        btnSetName.setTag(itag);
        final ImageView btnAlert = rootView.findViewById(R.id.btn_alert);
        btnAlert.setTag(itag);
    }

    private void updateWayToday() {
        Activity activity = getActivity();
        if (activity == null) return; //
        if (BuildConfig.DEBUG) Log.d(LT, "updateWayToday, count=" + ITag.store.count());
        View rootView;
        if (ITag.store.count() > 0) {
            ITagInterface itag = ITag.store.byPos(0);
            rootView = tagViews.get(itag.id());
        } else {
            rootView = getView();
        }
        if (rootView == null) {
            Log.e(LT, "no rootView for waytoday", new Exception("no rootView for waytoday"));
            return;
        }
        View waytoday = rootView.findViewById(R.id.wt);
        if (waytoday == null) {
            Log.e(LT, "no waytoday imageview", new Exception("no waytoday imagegview"));
            return;
        }
        if (BuildConfig.DEBUG)
            Log.d(LT, "updateWayToday, updating=" + Waytoday.tracker.isUpdating + " trackID=" + trackID);
        if (Waytoday.tracker.isUpdating && !"".equals(trackID)) {
            waytoday.setVisibility(View.VISIBLE);
            TextView wtid = waytoday.findViewById(R.id.text_wt_id);
            wtid.setText(trackID);
        } else {
            waytoday.setVisibility(View.GONE);
        }
    }

    private void updateLocationImage(@NonNull ViewGroup rootView, @NonNull String id) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        final ImageView imageLocation = rootView.findViewById(R.id.location);
        imageLocation.setTag(id);

        Map<String, HistoryRecord> records = HistoryRecord.getHistoryRecords(activity);

        if (records.get(id) == null) {
            Log.d(LT, "updateLocationImage off:" + id);
            mLocationAnimation.cancel();
            imageLocation.setVisibility(View.GONE);
        } else {
            Log.d(LT, "updateLocationImage on:" + id);
            imageLocation.startAnimation(mLocationAnimation);
            imageLocation.setVisibility(View.VISIBLE);
        }
    }

    private void updateLocationImages() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        for (Map.Entry<String, ViewGroup> entry : tagViews.entrySet()) {
            String id = entry.getKey();
            ViewGroup rootView = entry.getValue();
            if (rootView != null) {
                updateLocationImage(rootView, id);
            }
        }
    }

    private void updateRSSI(@NonNull ViewGroup rootView, int rssi) {
        Activity activity = getActivity();
        if (activity == null) return; //
        RssiView rssiView = rootView.findViewById(R.id.rssi);
        if (rssiView == null) {
            return;
        }
        rssiView.setRssi(rssi);
    }

    private void updateRSSI(@NonNull String id, int rssi) {
        ViewGroup view = tagViews.get(id);
        if (view == null) {
            return;
        }
        updateRSSI(view, rssi);
    }

    private void updateState(@NonNull ViewGroup rootView, @NonNull String id, @NonNull BLEConnectionState state) {
        Activity activity = getActivity();
        if (activity == null) return; //
        int statusDrawableId;
        int statusTextId;
        if (ble.state() == BLEState.OK) {
            switch (state) {
                case connected:
                    statusDrawableId = R.drawable.bt;
                    statusTextId = R.string.bt;
                    break;
                case connecting:
                case disconnecting:
                    ITagInterface itag = ITag.store.byId(id);
                    if (itag != null && itag.isAlertDisconnected()) {
                        statusDrawableId = R.drawable.bt_connecting;
                        statusTextId = R.string.bt_lost;
                    } else {
                        statusDrawableId = R.drawable.bt_setup;
                        if (state == BLEConnectionState.connecting)
                            statusTextId = R.string.bt_connecting;
                        else
                            statusTextId = R.string.bt_disconnecting;
                    }
                    break;
                case writting:
                case reading:
                    statusDrawableId = R.drawable.bt_call;
                    statusTextId = R.string.bt_call;
                    break;
                case disconnected:
                default:
                    statusDrawableId = R.drawable.bt_disabled;
                    statusTextId = R.string.bt_disabled;
            }
        } else {
            statusDrawableId = R.drawable.bt_disabled;
            statusTextId = R.string.bt_disabled;
        }

        final ImageView imgStatus = rootView.findViewById(R.id.bt_status);
        imgStatus.setImageResource(statusDrawableId);
        final TextView textStatus = rootView.findViewById(R.id.text_status);
        textStatus.setText(statusTextId);
    }

    private void updateState(@NonNull String id, @NonNull BLEConnectionState state) {
        ViewGroup view = tagViews.get(id);
        if (view == null) {
            return;
        }
        updateState(view, id, state);
    }

    private void updateName(@NonNull ViewGroup rootView, String name) {
        Activity activity = getActivity();
        if (activity == null) return; //
        final TextView textName = rootView.findViewById(R.id.text_name);
        textName.setText(name);
    }

    private void updateAlertButton(@NonNull ViewGroup rootView, boolean isAlertDisconnected, boolean isConnected) {
        Activity activity = getActivity();
        if (activity == null) return; //
        final ImageView btnAlert = rootView.findViewById(R.id.btn_alert);
        btnAlert.setImageResource(isAlertDisconnected || isConnected ? R.drawable.linked : R.drawable.keyfinder);
    }

    private void updateAlertButton(@NonNull String id) {
        Activity activity = getActivity();
        if (activity == null) return; //
        ViewGroup view = tagViews.get(id);
        if (view == null) {
            return;
        }
        ITagInterface itag = ITag.store.byId(id);
        if (itag == null) {
            return;
        }
        BLEConnectionInterface connection = ble.connectionById(id);
        updateAlertButton(view, itag.isAlertDisconnected(), connection.isConnected());
    }

    private void updateITagImageAnimation(@NonNull ViewGroup rootView, ITagInterface itag, BLEConnectionInterface connection) {
        Activity activity = getActivity();
        if (activity == null) return; //
        if (mITagAnimation == null) {
            return;
        }

        Animation animShake = null;

        if (BuildConfig.DEBUG) {
            Log.d(LT, "updateITagImageAnimation isFindMe:" + connection.isFindMe() +
                    " isAlerting:" + connection.isAlerting() +
                    " isAlertDisconnected:" + itag.isAlertDisconnected() +
                    " not conencted:" + !connection.isConnected()
            );
        }
        if (connection.isAlerting() ||
                connection.isFindMe() ||
                itag.isAlertDisconnected() && !connection.isConnected()) {
            animShake = mITagAnimation;//AnimationUtils.loadAnimation(getActivity(), R.anim.shake_itag);
        }
        final ImageView imageITag = rootView.findViewById(R.id.image_itag);
        if (animShake == null) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "updateITagImageAnimation: No animations appointed");
            }
            animShake = imageITag.getAnimation();
            if (animShake != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(LT, "updateITagImageAnimation: Stop previous animation");
                }
                animShake.cancel();
            }
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "updateITagImageAnimation: Start animation");
            }
            imageITag.startAnimation(animShake);
        }
    }

    private void updateITagImageAnimation(@NonNull ITagInterface itag, @NonNull BLEConnectionInterface connection) {
        Activity activity = getActivity();
        if (activity == null) return; //
        ViewGroup view = tagViews.get(itag.id());
        if (view == null) {
            return;
        }
        updateITagImageAnimation(view, itag, connection);
    }

    private void updateITagImage(@NonNull ViewGroup rootView, ITagInterface itag) {
        Activity activity = getActivity();
        if (activity == null) return; //
        int imageId;
        switch (itag.color()) {
            case black:
                imageId = R.drawable.itag_black;
                break;
            case red:
                imageId = R.drawable.itag_red;
                break;
            case green:
                imageId = R.drawable.itag_green;
                break;
            case gold:
                imageId = R.drawable.itag_gold;
                break;
            case blue:
                imageId = R.drawable.itag_blue;
                break;
            default:
                imageId = R.drawable.itag_white;
                break;
        }


        final ImageView imageITag = rootView.findViewById(R.id.image_itag);
        imageITag.setImageResource(imageId);
        imageITag.setTag(itag);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        mLocationAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.shadow_location);
        mITagAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.shake_itag);
        Context context = getContext();
        if (context != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            trackID = sp.getString("tid", "");
        }

        final VolumePreference mute = new VolumePreference(getContext());
        View root = inflater.inflate(R.layout.fragment_itags, container, false);
        if (root != null) {
            final ImageView imgMute = root.findViewById(R.id.btn_mute);
            int m = mute.get();
            imgMute.setImageResource(
                    m == VolumePreference.MUTE
                            ? R.drawable.mute
                            : m == VolumePreference.LOUD
                            ? R.drawable.nomute
                            : R.drawable.vibration);
            imgMute.setOnClickListener(v -> {
                int volume = mute.get();
                volume++;
                if (volume > 2) {
                    volume = 0;
                }
                mute.set(volume);
                imgMute.setImageResource(
                        volume == VolumePreference.MUTE
                                ? R.drawable.mute
                                : volume == VolumePreference.LOUD
                                ? R.drawable.nomute
                                : R.drawable.vibration);

                int toastId = volume == VolumePreference.MUTE
                        ? R.string.soundmode_off
                        : volume == VolumePreference.LOUD
                        ? R.string.soundmode_on
                        : R.string.soundmode_vibration;
                Toast.makeText(getContext(), toastId, Toast.LENGTH_SHORT).show();
            });
        }


        return root;
    }

    // TODO: ugly
    private void startRssi() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "startRssi");
        }
        for (int i = 0; i < ITag.store.count(); i++) {
            ITagInterface itag = ITag.store.byPos(i);
            BLEConnectionInterface connection = ble.connectionById(itag.id());
            if (connection.isConnected()) {
                connection.enableRSSI();
            }
        }
    }

    private void stopRssi() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "stopRssi");
        }
        for (int i = 0; i < ITag.store.count(); i++) {
            ITagInterface itag = ITag.store.byPos(i);
            BLEConnectionInterface connection = ble.connectionById(itag.id());
            connection.disableRSSI();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (BuildConfig.DEBUG) {
            Log.d(LT, "onResume");
        }
        Activity activity = getActivity();
        if (activity == null)
            return;
        ITagApplication.faITagsView(ITag.store.count());
        final ViewGroup root = (ViewGroup) requireView();
        setupTags(root);
        disposableBag.add(ITag.store.observable().subscribe(event -> setupTags(root)));
        for (int i = 0; i < ITag.store.count(); i++) {
            final ITagInterface itag = ITag.store.byPos(i);
            if (itag == null) {
                continue;
            }
            final String id = itag.id();
            final BLEConnectionInterface connection = ble.connectionById(id);
            disposableBag.add(connection.observableRSSI().subscribe(rssi -> updateRSSI(id, rssi)));
            disposableBag.add(connection.observableImmediateAlert().subscribe(state -> updateITagImageAnimation(itag, connection)));
            disposableBag.add(connection.observableState().subscribe(state -> {
                getActivity().runOnUiThread(() -> {
                    updateAlertButton(id);
                    updateState(id, state);
                    updateITagImageAnimation(itag, connection);
                    if (connection.isConnected()) {
                        connection.enableRSSI();
                    } else {
                        connection.disableRSSI();
                        updateRSSI(id, -999);
                    }
                });
            }));
            disposableBag.add(connection.observableClick().subscribe(event -> updateITagImageAnimation(itag, connection)));
        }
        HistoryRecord.addListener(this);

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean wt_disabled = sp.getBoolean("wt_disabled0", false);
        if (wt_disabled) {
            root.findViewById(R.id.btn_waytoday).setVisibility(View.GONE);
        } else {
            Waytoday.tracker.addOnTrackingStateListener(this);
            TrackIDJobService.addOnTrackIDChangeListener(this);
        }
        startRssi();
    }

    @Override
    public void onPause() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "onPause");
        }
        stopRssi();
        HistoryRecord.removeListener(this);
        disposableBag.dispose();
        Waytoday.tracker.removeOnTrackingStateListener(this);
        TrackIDJobService.removeOnTrackIDChangeListener(this);
        super.onPause();
    }

    @Override
    public void onHistoryRecordChange() {
        Activity activity = getActivity();
        if (activity == null)
            return;
        Log.d(LT, "onHistoryRecordChange");
        activity.runOnUiThread(this::updateLocationImages);
    }

    @Override
    public void onStateChange(@NonNull TrackerState state) {
        final View view = getView();
        if (view != null) {
            Activity activity = getActivity();
            if (activity == null)
                return;

            activity.runOnUiThread(this::updateWayToday);
        }
    }

    @Override
    public void onTrackID(@NonNull String trackID) {
        if (BuildConfig.DEBUG) Log.d(LT, "onTrackID: " + trackID);
        this.trackID = trackID;
        Activity activity = getActivity();
        if (activity == null)
            return;
        activity.runOnUiThread(this::updateWayToday);
    }
}
