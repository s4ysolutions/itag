package s4y.itag;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import s4y.itag.ble.BLEConnectionInterface;
import s4y.itag.ble.BLEConnectionState;
import s4y.itag.ble.BLEConnectionsInterface;
import s4y.itag.ble.BLEState;
import s4y.itag.history.HistoryRecord;
import s4y.itag.itag.ITag;
import s4y.itag.itag.ITagInterface;
import s4y.rasat.DisposableBag;
import s4y.waytoday.idservice.IDService;
import s4y.waytoday.locations.LocationsTracker;

import static s4y.itag.itag.ITag.ble;


/**
 * A simple {@link Fragment} subclass.
 */
public class ITagsFragment extends Fragment
        implements
        HistoryRecord.HistoryRecordListener,
        LocationsTracker.ITrackingStateListener,
        IDService.IIDSeriviceListener {
    private static final String LT = ITagsFragment.class.getName();
    private Animation mLocationAnimation;
    private Animation mITagAnimation;
    private String trackID = "";
    private DisposableBag disposableBag = new DisposableBag();

    public ITagsFragment() {
        // Required empty public constructor
    }

    private void setupTag(int n, @NonNull final ITagInterface itag, final View itagLayout) {
        final View btnForget = itagLayout.findViewById(R.id.btn_forget);
        btnForget.setTag(itag);
        final View btnColor = itagLayout.findViewById(R.id.btn_color);
        btnColor.setTag(itag);
        final View btnSetName = itagLayout.findViewById(R.id.btn_set_name);
        btnSetName.setTag(itag);
        final ImageView btnAlert = itagLayout.findViewById(R.id.btn_alert);
        btnAlert.setImageResource(itag.isAlertDisconnected() ? R.drawable.alert : R.drawable.noalert);
        btnAlert.setTag(itag);

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null)
            return;
        Animation animShake = null;
        RssiView rssiView = itagLayout.findViewById(R.id.rssi);
        int rssi = -1000;
        int statusDrawableId;
        int statusTextId;
        BLEConnectionState state = ble.connections().getStates().get(itag.id());
        if (ble.state() == BLEState.OK && state != null) {
            switch (state) {
                case unknown:
                case disconnected:
                case disconnecting:
                    if (itag.isAlertDisconnected()) {
                        statusDrawableId = R.drawable.bt_connecting;
                        statusTextId = R.string.bt_unk;
                    } else {
                        statusDrawableId = R.drawable.bt_disabled;
                        statusTextId = R.string.bt_disabled;
                    }
                    break;
                case connected:
                    statusDrawableId = R.drawable.bt;
                    statusTextId = R.string.bt;
                    rssi = 0;
                    break;
                case connecting:
                case discovering:
                case discoveringServices:
                case discoveringCharacteristics:
                    if (itag.isAlertDisconnected()) {
                        statusDrawableId = R.drawable.bt_connecting;
                        statusTextId = R.string.bt_unk;
                    } else {
                        statusDrawableId = R.drawable.bt_setup;
                        statusTextId = R.string.bt_connecting;
                    }
                    break;
                case writting:
                case reading:
                    statusDrawableId = R.drawable.bt_call;
                    statusTextId = R.string.bt_call;
                    break;
                default:
                    statusDrawableId = R.drawable.bt_disabled;
                    statusTextId = R.string.bt_disabled;
            }
            if (ble.alert().isAlerting(itag.id()) ||
                    ble.findMe().isFindMe(itag.id()) ||
                    itag.isAlertDisconnected() && state != BLEConnectionState.connected
            ) {
                Log.d(LT, "Start animate because gatt.isFindingITag");
                animShake = mITagAnimation;//AnimationUtils.loadAnimation(getActivity(), R.anim.shake_itag);
            }
        } else {
            statusDrawableId = R.drawable.bt_disabled;
            statusTextId = R.string.bt_disabled;
        }
        rssiView.setRssi(rssi);

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

        final ImageView imageITag = itagLayout.findViewById(R.id.image_itag);
        // imageITag.setOnLongClickListener(mOnLongClickListener);
        imageITag.setImageResource(imageId);
        imageITag.setTag(itag);
        if (animShake == null) {
            Log.d(LT, "No animations appointed");
            animShake = imageITag.getAnimation();
            if (animShake != null) {
                Log.d(LT, "Stop previous animation");
                animShake.cancel();
            }
        } else {
            Log.d(LT, "Start animation");
            imageITag.startAnimation(animShake);
        }

        final ImageView imgStatus = itagLayout.findViewById(R.id.bt_status);
        imgStatus.setImageResource(statusDrawableId);

        final TextView textName = itagLayout.findViewById(R.id.text_name);
//        textName.setText(device.name!=null && device.name.length()>0 ?device.name:device.addr);
        textName.setText(itag.name());

        final TextView textStatus = itagLayout.findViewById(R.id.text_status);
        textStatus.setText(statusTextId);

        final ImageView imageLocation = itagLayout.findViewById(R.id.location);
        imageLocation.setTag(itag);

        Map<String, HistoryRecord> records = HistoryRecord.getHistoryRecords(getActivity());

        if (records.get(itag.id()) == null) {
            mLocationAnimation.cancel();
            imageLocation.setVisibility(View.GONE);
        } else {
            imageLocation.startAnimation(mLocationAnimation);
            imageLocation.setVisibility(View.VISIBLE);
        }

        View waytoday = itagLayout.findViewById(R.id.wt);
        if (n == 1 && LocationsTracker.isUpdating && !"".equals(trackID)) {
            waytoday.setVisibility(View.VISIBLE);
            TextView wtid = waytoday.findViewById(R.id.text_wt_id);
            wtid.setText(trackID);
        } else {
            waytoday.setVisibility(View.GONE);
        }
    }

    private final Map<ITagInterface, ViewGroup> tagViews = new HashMap<>();

    private void updateTags(@NonNull ViewGroup root) {
        int n = 1;
        for (Map.Entry<ITagInterface, ViewGroup> entry : tagViews.entrySet()) {
            setupTag(n, entry.getKey(), entry.getValue());
        }
    }

    private void updateRSSI(@NonNull ITagInterface itag,int rssi) {
        ViewGroup view = tagViews.get(itag);
        if (view == null) {
            return;
        }
        RssiView rssiView = view.findViewById(R.id.rssi);
        if (rssiView == null) {
            return;
        }
        rssiView.setRssi(rssi);
    }

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
            tagViews.put(itag, root.findViewById(R.id.tag_1).findViewById(R.id.layout_itag));
        }
        if (s > 1) {
            ITagInterface itag = ITag.store.byPos(1);
            tagViews.put(itag, root.findViewById(R.id.tag_2).findViewById(R.id.layout_itag));
        }
        if (s > 2) {
            ITagInterface itag = ITag.store.byPos(2);
            tagViews.put(itag, root.findViewById(R.id.tag_3).findViewById(R.id.layout_itag));
        }
        if (s > 3) {
            ITagInterface itag = ITag.store.byPos(3);
            tagViews.put(itag, root.findViewById(R.id.tag_4).findViewById(R.id.layout_itag));
        }
        updateTags(root);
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

        return inflater.inflate(R.layout.fragment_itags, container, false);
    }

    // TODO: ugly
    private void startRssi() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "startRssi");
        }
        for (int i = 0; i < ITag.store.count(); i++) {
            ITagInterface itag = ITag.store.byPos(i);
            ble.connections().enableRSSI(itag.id());
        }
    }

    private void stopRssi() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "stopRssi");
        }
        for (int i = 0; i < ITag.store.count(); i++) {
            ITagInterface itag = ITag.store.byPos(i);
            ble.connections().disableRSSI(itag.id());
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
        final ViewGroup root = (ViewGroup) Objects.requireNonNull(getView());
        setupTags(root);
        disposableBag.add(ITag.store.observable().subscribe(event -> {
            setupTags(root);
        }));
        for (int i=0; i< ITag.store.count();i++) {
            ITagInterface itag = ITag.store.byPos(i);
            BLEConnectionInterface connection = ITag.ble.connections().byId(itag.id());
            disposableBag.add(connection.observableRSSI().subscribe(rssi -> {
                updateRSSI(itag, rssi);
            }));
        }
        HistoryRecord.addListener(this);

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean wt_disabled = sp.getBoolean("wt_disabled", false);
        if (wt_disabled) {
            root.findViewById(R.id.btn_waytoday).setVisibility(View.GONE);
        } else {
            LocationsTracker.addOnTrackingStateListener(this);
            IDService.addOnTrackIDChangeListener(this);
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
        LocationsTracker.removeOnTrackingStateListener(this);
        IDService.removeOnTrackIDChangeListener(this);
        super.onPause();
    }

    @Override
    public void onHistoryRecordChange() {

        final View view = getView();
        if (view != null) {
            Activity activity = getActivity();
            if (activity == null)
                return;

            activity.runOnUiThread(() -> setupTags((ViewGroup) view));
        }
    }

    @Override
    public void onStateChange(@NonNull LocationsTracker.TrackingState state) {
        final View view = getView();
        if (view != null) {
            Activity activity = getActivity();
            if (activity == null)
                return;

            activity.runOnUiThread(() -> setupTags((ViewGroup) view));
        }
    }

    @Override
    public void onTrackID(@NonNull String trackID) {
        this.trackID = trackID;
        final View view = getView();
        if (view != null) {
            Activity activity = getActivity();
            if (activity == null)
                return;

            activity.runOnUiThread(() -> setupTags((ViewGroup) view));
        }
    }
}
