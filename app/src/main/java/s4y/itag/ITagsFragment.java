package s4y.itag;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import s4y.itag.ble.BLEConnectionState;
import s4y.itag.ble.BLEState;
import s4y.itag.ble.ITagDevice;
import s4y.itag.ble.ITagGatt;
import s4y.itag.ble.ITagsDb;
import s4y.itag.ble.ITagsService;
import s4y.itag.history.HistoryRecord;
import s4y.itag.itag.ITag;
import s4y.itag.itag.ITagInterface;
import s4y.itag.itag.StoreOp;
import s4y.itag.itag.TagColor;
import s4y.rasat.DisposableBag;
import s4y.rasat.Handler;
import s4y.waytoday.idservice.IDService;
import s4y.waytoday.locations.LocationsTracker;


/**
 * A simple {@link Fragment} subclass.
 */
public class ITagsFragment extends Fragment
        implements
        MainActivity.ServiceBoundListener,
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
        BLEConnectionState state = ITag.ble.connections().getStates().get(itag.id());
        if (ITag.ble.state() == BLEState.OK && state != null) {
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
                    rssi = gatt.mRssi;
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
            if (ITag.ble.alert().isAlerting(itag.id()) ||
                    ITag.ble.findMe().isFindMe(itag.id()) ||
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
        if (s == 0) {
            boolean active = !"".equals(trackID) && LocationsTracker.isUpdating;
            View wtview = tagsLayout.findViewById(R.id.wt);
            wtview.setVisibility(
                    active
                            ? View.VISIBLE
                            : View.GONE
            );
            if (active) {
                TextView tid = wtview.findViewById(R.id.text_wt_id);
                tid.setText(trackID);
//                TextView tid2 = wtview.findViewById(R.id.text_wt_id2);
//                tid2.setText(trackID);
            }
        } else {
            setupTag(1, ITag.store.byPos(0), tagsLayout.findViewById(R.id.tag_1).findViewById(R.id.layout_itag));
            if (s > 1) {
                setupTag(2, ITag.store.byPos(1), tagsLayout.findViewById(R.id.tag_2).findViewById(R.id.layout_itag));
            }
            if (s > 2) {
                setupTag(3, ITag.store.byPos(2), tagsLayout.findViewById(R.id.tag_3).findViewById(R.id.layout_itag));
            }
            if (s > 3) {
                setupTag(4, ITag.store.byPos(3), tagsLayout.findViewById(R.id.tag_4).findViewById(R.id.layout_itag));
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        mLocationAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.shadow_location);
        mITagAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.shake_itag);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        trackID = sp.getString("tid", "");

        return inflater.inflate(R.layout.fragment_itags, container, false);
    }

    private final List<ITagGatt> mRssiGatt = new ArrayList<>(8);

    private boolean mIsRssiStarted;

    // TODO: ugly
    private void startRssi() {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (BuildConfig.DEBUG) {
            Log.d(LT, "startRssi, bound=" + (mainActivity != null && mainActivity.mITagsServiceBound));
        }
        stopRssi();
        if (mainActivity != null && mainActivity.mITagsServiceBound) {
            mIsRssiStarted = true;
            ITagsService service = mainActivity.iTagsService;
            for (ITagDevice device : ITagsDb.getDevices(getActivity())) {
                ITagGatt gatt = service.getGatt(device.addr, false);
                if (gatt.isConnected()) {
                    gatt.startListenRssi();
                    mRssiGatt.add(gatt);
                }// wait for ITagChange and start listen then
            }
        }
    }

    private void stopRssi() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "stopRssi");
        }
        for (ITagGatt gatt : mRssiGatt) {
            gatt.stopListenRssi();
        }
        mIsRssiStarted = false;
        mRssiGatt.clear();
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
        startRssi();
        ViewGroup root = (ViewGroup) Objects.requireNonNull(getView());
        setupTags(root);
        MainActivity.addServiceBoundListener(this);
        disposableBag.add(ITag.store.observable().subscribe(new Handler<StoreOp>() {
            @Override
            public void handle(StoreOp event) {

            }
        }));
        ITagGatt.addOnITagChangeListener(this);
        HistoryRecord.addListener(this);

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean wt_disabled = sp.getBoolean("wt_disabled", false);
        if (wt_disabled) {
            root.findViewById(R.id.btn_waytoday).setVisibility(View.GONE);
        } else {
            LocationsTracker.addOnTrackingStateListener(this);
            IDService.addOnTrackIDChangeListener(this);
        }
    }

    @Override
    public void onPause() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "onPause");
        }
        HistoryRecord.removeListener(this);
        disposableBag.dispose();
        LocationsTracker.removeOnTrackingStateListener(this);
        MainActivity.removeServiceBoundListener(this);
        IDService.removeOnTrackIDChangeListener(this);
        stopRssi();
        super.onPause();
    }

    @Override
    public void onDbChange() {
        setupTags((ViewGroup) Objects.requireNonNull(getView()));
    }

    @Override
    public void onDbAdd(ITagDevice device) {

    }

    @Override
    public void onDbRemove(ITagDevice device) {

    }

    @Override
    public void onITagChange(@NonNull ITagGatt gatt) {
        Activity activity = getActivity();
        if (activity == null)
            return;

        activity.runOnUiThread(() -> {
            // handle cases like "onBound", connectAll, etc
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onITagChange mIsRssiStarted=" + mIsRssiStarted + " addr=" + gatt.mAddr);
            }
            if (!mIsRssiStarted) {
                startRssi();
            }
            View view = getView();
            if (view != null)
                setupTags((ViewGroup) view);
        });
    }

    @Override
    public void onITagRssi(@NonNull ITagGatt gatt, int rssi) {
        final View view = getView();
        if (view != null) {
            Activity activity = getActivity();
            if (activity == null)
                return;

            activity.runOnUiThread(() -> setupTags((ViewGroup) view));
        }
    }

    @Override
    public void onITagClicked(@NonNull ITagGatt gatt) {
    }

    @Override
    public void onITagDoubleClicked(@NonNull ITagGatt gatt) {
    }

    @Override
    public void onITagFindingPhone(@NonNull ITagGatt gatt, boolean on) {
        View view = getView();
        if (view != null) {
            Activity activity = getActivity();
            if (activity == null)
                return;
            activity.runOnUiThread(() -> setupTags((ViewGroup) view));
        }
    }


    @Override
    public void onBoundingChanged(@NonNull MainActivity activity) {
        if (activity.mITagsServiceBound) {
            startRssi(); // nothing bad in extra call
        }
        final View view = getView();
        if (view != null)
            setupTags((ViewGroup) view);
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
