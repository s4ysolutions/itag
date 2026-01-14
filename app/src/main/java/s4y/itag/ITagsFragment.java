package s4y.itag;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.HashMap;
import java.util.Map;

import s4y.itag.ble.BLEConnectionInterface;
import s4y.itag.ble.BLEConnectionState;
import s4y.itag.ble.BLEState;
import s4y.itag.history.HistoryRecord;
import s4y.itag.itag.ITag;
import s4y.itag.itag.ITagInterface;
import s4y.itag.preference.WayTodayDisabled0Preference;
import s4y.itag.preference.VolumePreference;
import s4y.itag.waytoday.WayToday;
import solutions.s4y.rasat.DisposableBag;

import static s4y.itag.itag.ITag.ble;

public class ITagsFragment extends Fragment implements HistoryRecord.HistoryRecordListener {
    private static final String LT = ITagsFragment.class.getName();
    private Animation mLocationAnimation;
    private Animation mITagAnimationShakeIndefinitely;
    private Animation mITagAnimationShakeOnce;
    private String trackID = "";
    private final DisposableBag disposableBag = new DisposableBag();

    public ITagsFragment() {
        // Required empty public constructor
    }

    private final Map<String, ViewGroup> tagViews = new HashMap<>();

    public void updateTags(boolean firstTime){
        Log.d("ingo", "firstTime " + firstTime);
        for (Map.Entry<String, ViewGroup> entry : tagViews.entrySet()) {
            String id = entry.getKey();
            ViewGroup rootView = entry.getValue();
            ITagInterface itag = ITag.store.byId(id);
            BLEConnectionInterface connection = ble.connectionById(id);
            if (itag != null) {
                setupButtons(rootView, itag);
                updateITagImage(rootView, itag);
                updateITagImageAnimation(rootView, itag, connection);
                updateName(rootView, itag);
                updateAlertButton(rootView, itag.isConnectModeEnabled(), connection.isConnected());
            }
            updateRSSI(rootView, connection.rssi());
            updateState(rootView, id, connection.state());
            updateLocationImage(rootView, id);
        }

        updateWayToday();
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
        Log.d("ingo", "s je " + s);
        final int rid = s == 0 ? R.layout.itag_0 : s == 1 ? R.layout.itag_1 : s == 2 ? R.layout.itag_2 : s == 3 ? R.layout.itag_3 : R.layout.itag_4;
        tagsLayout = activity.getLayoutInflater().inflate(rid, root, false);
        root.addView(tagsLayout, index);
        tagViews.clear();

        int[] tag_ids = new int[]{R.id.tag_1, R.id.tag_2, R.id.tag_3, R.id.tag_4};
        for(int i = 0; i <= s; i++){
            ITagInterface itag = ITag.store.byPos(i);
            if (itag != null) {
                tagViews.put(itag.id(), root.findViewById(tag_ids[i]).findViewById(R.id.layout_itag));
            }
        }

        updateTags(true);
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
            if (itag == null) {
                Log.e(LT, "no itag in store in pos 0", new Exception("no itag in store in pos 0"));
                return;
            }
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
        if (WayToday.getInstance().isTrackingOn() && WayToday.getInstance().wtClient.hasTrackerId()) {
            String trackID = WayToday.getInstance().wtClient.getCurrentTrackerId();
            if (BuildConfig.DEBUG)
                Log.d(LT, "updateWayToday, updating=" + WayToday.getInstance().isTrackingOn() + " trackID=" + trackID);
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
            //imageLocation.startAnimation(mLocationAnimation);
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
        TextView rssiTextView = rootView.findViewById(R.id.text_rssi);
        if (rssiView != null) {
            rssiView.setRssi(rssi);
        }
        if (rssiTextView != null) {
            getActivity().runOnUiThread(() -> {
                rssiTextView.setText(String.format(getString(R.string.rssi), rssi));
            });
        }
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
        int statusDrawableTint = Color.BLACK;
        int statusTextId;
        boolean progressBar = false;
        if (ble.state() == BLEState.OK) {
            ITagInterface itag = ITag.store.byId(id);
            switch (state) {
                case connected:
                    statusDrawableId = R.drawable.bt;
                    statusDrawableTint = Color.GREEN;
                    statusTextId = R.string.bt;
                    break;
                case disconnected:
                    if (itag != null && itag.isConnectModeEnabled()) {
                        statusDrawableId = R.drawable.bt_connecting;
                        statusDrawableTint = Color.RED;
                        statusTextId = R.string.bt_disconnected;
                        Log.d("ingo", "disconnected");
                    } else {
                        statusDrawableId = R.drawable.bt_call;
                        statusDrawableTint = Color.LTGRAY;
                        statusTextId = R.string.bt_scanning;
                    }
                    break;
                case disconnecting:
                case connecting:
                    progressBar = true;
                    statusDrawableId = R.drawable.bt_setup;
                    statusDrawableTint = Color.parseColor("#FFA500"); // orange
                    if (state == BLEConnectionState.connecting) {
                        statusTextId = R.string.bt_connecting;
                    } else {
                        statusTextId = R.string.bt_disconnecting;
                    }
                    break;
                case writting:
                case reading:
                    statusDrawableId = R.drawable.bt_call;
                    statusTextId = R.string.bt_call;
                    break;
                default:
                    statusDrawableId = R.drawable.bt_disabled;
                    statusDrawableTint = Color.LTGRAY;
                    statusTextId = R.string.bt_disabled;
            }
            final ProgressBar progressBarView = rootView.findViewById(R.id.progressBar);
            if(progressBar){
                progressBarView.setVisibility(View.VISIBLE);
            } else {
                progressBarView.setVisibility(View.GONE);
            }
        } else {
            statusDrawableId = R.drawable.bt_disabled;
            statusDrawableTint = Color.LTGRAY;
            statusTextId = R.string.bt_disabled;
        }
        final ImageView imgStatus = rootView.findViewById(R.id.bt_status);
        imgStatus.setImageResource(statusDrawableId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            imgStatus.getDrawable().setTint(statusDrawableTint);
        }
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

    private void updateName(@NonNull ViewGroup rootView, ITagInterface itag) {
        Activity activity = getActivity();
        if (activity == null) return; //
        final TextView textName = rootView.findViewById(R.id.text_name);
        final TextView textId = rootView.findViewById(R.id.text_id);
        textName.setText(itag.name());
        textId.setText(itag.id());
    }

    private void updateAlertButton(@NonNull ViewGroup rootView, boolean isConnectModeEnabled, boolean isConnected) {
        Activity activity = getActivity();
        if (activity == null) return; //
        final ImageView btnAlert = rootView.findViewById(R.id.btn_alert);
        final TextView modeTextView = rootView.findViewById(R.id.text_mode);
        if (BuildConfig.DEBUG) {
            Log.d(LT, "updateAlertButton2 isConnectModeEnabled=" + isConnectModeEnabled + " isConnected=" + isConnected);
        }
        btnAlert.setImageResource(isConnectModeEnabled ? R.drawable.linked : R.drawable.keyfinder);
        modeTextView.setText(getString(isConnectModeEnabled || isConnected ? R.string.mode_active : R.string.mode_passive));
    }

    private void updateAlertButton(@NonNull String id) {
        Activity activity = getActivity();
        if (activity == null) return; //
        ViewGroup view = tagViews.get(id);
        if (view == null) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "updateAlertButton1 id=" + id + " view null");
            }
            return;
        }
        ITagInterface itag = ITag.store.byId(id);
        if (itag == null) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "updateAlertButton1 id=" + id + " itag null");
            }
            return;
        }
        if (BuildConfig.DEBUG) {
            Log.d(LT, "updateAlertButton connectionById " + id);
        }
        BLEConnectionInterface connection = ble.connectionById(id);
        boolean isConnected = connection.isConnected();
        boolean isConnectModeEnabled = itag.isConnectModeEnabled();
        if (BuildConfig.DEBUG) {
            Log.d(LT, "id = " + id + " updateAlertButton2 isAlertDisconnected=" + isConnectModeEnabled + " isConnected=" + isConnected);
        }
        updateAlertButton(view, isConnectModeEnabled, isConnected);
    }

    private void updateITagImageAnimation(@NonNull ViewGroup rootView, ITagInterface itag, BLEConnectionInterface connection) {
        Activity activity = getActivity();
        if (activity == null) return; //
        if (mITagAnimationShakeIndefinitely == null) {
            return;
        }

        Animation animShake = null;
        float alpha;

        if (BuildConfig.DEBUG) {
            Log.d(LT, "updateITagImageAnimation isFindMe:" + connection.isFindMe() +
                    " isAlerting:" + connection.isAlerting() +
                    " isConnectModeEnabled:" + itag.isConnectModeEnabled() +
                    " not connected:" + !connection.isConnected()
            );
        }
        if (connection.isAlerting() ||
                itag.isShaking() ||
                connection.isFindMe()) {
            animShake = mITagAnimationShakeIndefinitely;
        }
        if(connection.observableClick().value() == 1){
            animShake = mITagAnimationShakeOnce;
        }
        if(connection.isConnected()){
            alpha = 1.0f;
        } else {
            alpha = 0.3f;
        }
        final ITagImageView imageITag = rootView.findViewById(R.id.image_itag);
        imageITag.setOnClickListener(view -> {
            ((MainActivity) activity).onITagClick(itag);
        });
        imageITag.setOnLongClickListener(view -> {
            if(connection.state() == BLEConnectionState.connected || connection.state() == BLEConnectionState.connecting) {
                ITag.store.setReconnectMode(itag.id(), false);
                new Thread(connection::disconnect).start();
            } else if(connection.state() == BLEConnectionState.disconnected) {
                ITag.store.setShakingOnConnectDisconnect(itag.id(), false);
                connection.connect();
            }
            return true;
        });
        if (Looper.myLooper() == Looper.getMainLooper()) {
            imageITag.setAlpha(alpha);
        } else {
            float finalAlpha = alpha;
            getActivity().runOnUiThread(() -> {
                imageITag.setAlpha(finalAlpha);
            });
        }
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
            if (Looper.myLooper() == Looper.getMainLooper()) {
                imageITag.startAnimation(animShake);
            } else {
                final Animation anim = animShake;
                float finalAlpha = alpha;
                getActivity().runOnUiThread(() -> {
                    imageITag.startAnimation(anim);
                });
            }
        }
    }

    void updateITagImageAnimation(@NonNull ITagInterface itag, @NonNull BLEConnectionInterface connection) {
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

        final ITagImageView imageITag = rootView.findViewById(R.id.image_itag);
        imageITag.setImageResource(imageId);
        imageITag.setTag(itag);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        mLocationAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.shadow_location);
        mITagAnimationShakeIndefinitely = AnimationUtils.loadAnimation(getActivity(), R.anim.shake_itag_indefinitely);
        mITagAnimationShakeOnce = AnimationUtils.loadAnimation(getActivity(), R.anim.shake_itag_once);

        final VolumePreference mute = new VolumePreference(getContext());
        View root = inflater.inflate(R.layout.fragment_itags, container, false);
        if (root != null) {
            ViewGroup realRoot = root.findViewById(R.id.root);
            setupTags(realRoot);
            final ImageView imgMute = root.findViewById(R.id.btn_mute);
            int m = mute.get();
            imgMute.setImageResource(m == VolumePreference.MUTE ? R.drawable.mute : m == VolumePreference.LOUD ? R.drawable.nomute : R.drawable.vibration);
            imgMute.setOnClickListener(v -> {
                int volume = mute.get();
                volume++;
                if (volume > 2) {
                    volume = 0;
                }
                mute.set(volume);
                imgMute.setImageResource(volume == VolumePreference.MUTE ? R.drawable.mute : volume == VolumePreference.LOUD ? R.drawable.nomute : R.drawable.vibration);

                int toastId = volume == VolumePreference.MUTE ? R.string.soundmode_off : volume == VolumePreference.LOUD ? R.string.soundmode_on : R.string.soundmode_vibration;
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
            if (itag == null) {
                continue;
            }
            BLEConnectionInterface connection = ble.connectionById(itag.id());
            if (connection.state() == BLEConnectionState.connected) {
                connection.enableRSSI();
            } else {
                updateRSSI(itag.id(), -999);
            }
        }
    }

    private void stopRssi() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "stopRssi");
        }
        for (int i = 0; i < ITag.store.count(); i++) {
            ITagInterface itag = ITag.store.byPos(i);
            if (itag != null) {
                BLEConnectionInterface connection = ble.connectionById(itag.id());
                connection.disableRSSI();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (BuildConfig.DEBUG) {
            Log.d(LT, "onResume");
        }
        Activity activity = getActivity();
        if (activity == null) return;
        ITagApplication.faITagsView(ITag.store.count());

        disposableBag.add(ITag.store.observable().subscribe(event -> {
            Log.d("ingo", "setupTags");
            activity.runOnUiThread(() -> updateTags(false));
            //setupTags(root);
        }));
        for (int i = 0; i < ITag.store.count(); i++) {
            final ITagInterface itag = ITag.store.byPos(i);
            if (itag == null) {
                continue;
            }
            final String id = itag.id();
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onResume connectionById " + id);
            }
            final BLEConnectionInterface connection = ble.connectionById(id);
            disposableBag.add(connection.observableRSSI().subscribe(rssi -> updateRSSI(id, rssi)));
            disposableBag.add(connection.observableImmediateAlert().subscribe(state -> updateITagImageAnimation(itag, connection)));
            disposableBag.add(connection.observableState().subscribe(state -> getActivity().runOnUiThread(() -> {
                if (BuildConfig.DEBUG) {
                    Log.d(LT, "connection " + id + " state changed " + connection.state().toString());
                }
                updateAlertButton(id);
                updateState(id, state);
                updateITagImageAnimation(itag, connection);
                if (connection.state() == BLEConnectionState.connected) { //isConnected()) {
                    connection.enableRSSI();
                } else {
                    connection.disableRSSI();
                    updateRSSI(id, -999);
                }
            })));
            disposableBag.add(connection.observableClick().subscribe(event -> updateITagImageAnimation(itag, connection)));
        }
        HistoryRecord.addListener(this);

        boolean wt_disabled0 = WayTodayDisabled0Preference.get(getActivity());
        if (wt_disabled0) {
            ViewGroup root = (ViewGroup) requireView();
            root.findViewById(R.id.btn_waytoday).setVisibility(View.GONE);
        } else {
            disposableBag.add(
                    WayToday.getTrackIdObservable()
                            .subscribe(trackID -> activity.runOnUiThread(this::updateWayToday)));
            disposableBag.add(
                    WayToday
                            .getGpsStatusObservable()
                            .subscribe(status -> getActivity().runOnUiThread(this::updateWayToday)));
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
        super.onPause();
    }

    @Override
    public void onHistoryRecordChange() {
        Activity activity = getActivity();
        if (activity == null) return;
        Log.d(LT, "onHistoryRecordChange");
        activity.runOnUiThread(this::updateLocationImages);
    }
}
