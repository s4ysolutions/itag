package solutions.s4y.itag;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import solutions.s4y.itag.ble.ITagGatt;
import solutions.s4y.itag.ble.ITagsDb;
import solutions.s4y.itag.ble.ITagDevice;
import solutions.s4y.itag.ble.ITagsService;


/**
 * A simple {@link Fragment} subclass.
 */
public class ITagsFragment extends Fragment implements ITagsDb.DbListener, ITagGatt.ITagChangeListener, MainActivity.ServiceBoundListener {
    private static final String LT = ITagsFragment.class.getName();

    public ITagsFragment() {
        // Required empty public constructor
    }

    private void setupTag(@NonNull final ITagDevice device, final View itagLayout) {
        final View btnForget = itagLayout.findViewById(R.id.btn_forget);
        btnForget.setTag(device);
        final View btnColor = itagLayout.findViewById(R.id.btn_color);
        btnColor.setTag(device);
        final View btnSetName = itagLayout.findViewById(R.id.btn_set_name);
        btnSetName.setTag(device);
        final ImageView btnAlert = itagLayout.findViewById(R.id.btn_alert);
        btnAlert.setImageResource(device.linked ? R.drawable.alert : R.drawable.noalert);
        btnAlert.setTag(device);

        MainActivity mainActivity = (MainActivity) getActivity();
        int statusDrawableId = R.drawable.bt_disabled;
        int statusTextId = R.string.bt_disabled;
        Animation animShake = null;
        RssiView rssiView = itagLayout.findViewById(R.id.rssi);
        int rssi = -1000;
        if (mainActivity.mITagsServiceBound && mainActivity.mBluetoothAdapter.enable()) {
            ITagsService service = mainActivity.mITagsService;
            ITagGatt gatt = service.getGatt(device.addr, false);
            if (gatt.isError()) {
                statusDrawableId = R.drawable.bt_setup;
                statusTextId = R.string.bt_setup;
            } else if (gatt.isConnecting()) {
                statusDrawableId = R.drawable.bt_connecting;
                statusTextId = R.string.bt_connecting;
            } else if (gatt.isTransmitting()) {
                statusDrawableId = R.drawable.bt_call;
                statusTextId = R.string.bt_call;
            } else if (gatt.isConnected()) {
                statusDrawableId = R.drawable.bt;
                statusTextId = R.string.bt;
                rssi = gatt.mRssi;
            } else {
                statusTextId = R.string.bt_unk;
            }
            if (gatt.isFindingITag() ||
                    gatt.isFindingPhone() ||
                    gatt.isError() && device.linked && mainActivity.mITagsServiceBound) {
                animShake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake_itag);
            }
        }
        rssiView.setRssi(rssi);

        int imageId;
        switch (device.color) {
            case BLACK:
                imageId = R.drawable.itag_black;
                break;
            case RED:
                imageId = R.drawable.itag_red;
                break;
            case GREEN:
                imageId = R.drawable.itag_green;
                break;
            case BLUE:
                imageId = R.drawable.itag_blue;
                break;
            default:
                imageId = R.drawable.itag_white;
        }

        final ImageView imageITag = itagLayout.findViewById(R.id.image_itag);
        // imageITag.setOnLongClickListener(mOnLongClickListener);
        imageITag.setImageResource(imageId);
        imageITag.setTag(device);
        if (animShake == null) {
            animShake = imageITag.getAnimation();
            if (animShake != null) {
                animShake.cancel();
            }
        } else {
            imageITag.startAnimation(animShake);
        }

        final ImageView imgStatus = itagLayout.findViewById(R.id.bt_status);
        imgStatus.setImageResource(statusDrawableId);

        final TextView textName = itagLayout.findViewById(R.id.text_name);
//        textName.setText(device.name!=null && device.name.length()>0 ?device.name:device.addr);
        textName.setText(device.name);

        final TextView textStatus = itagLayout.findViewById(R.id.text_status);
        textStatus.setText(statusTextId);
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
        final int s = ITagsDb.getDevices(getActivity()).size();
        final int rid = s == 0 ? R.layout.itag_0 : s == 1 ? R.layout.itag_1 : s == 2 ? R.layout.itag_2 : s == 3 ? R.layout.itag_3 : R.layout.itag_4;
        tagsLayout = activity.getLayoutInflater().inflate(rid, root, false);
        root.addView(tagsLayout, index);
        if (s > 0) {
            setupTag(ITagsDb.getDevices(getActivity()).get(0), tagsLayout.findViewById(R.id.tag_1).findViewById(R.id.layout_itag));
        }
        if (s > 1) {
            setupTag(ITagsDb.getDevices(getActivity()).get(1), tagsLayout.findViewById(R.id.tag_2).findViewById(R.id.layout_itag));
        }
        if (s > 2) {
            setupTag(ITagsDb.getDevices(getActivity()).get(2), tagsLayout.findViewById(R.id.tag_3).findViewById(R.id.layout_itag));
        }
        if (s > 3) {
            setupTag(ITagsDb.getDevices(getActivity()).get(3), tagsLayout.findViewById(R.id.tag_4).findViewById(R.id.layout_itag));
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

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
            ITagsService service = mainActivity.mITagsService;
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
        ITagApplication.faITagsView(ITagsDb.getDevices(getActivity()).size());
        startRssi();
        setupTags((ViewGroup) Objects.requireNonNull(getView()));
        MainActivity.addServiceBoundListener(this);
        ITagsDb.addListener(this);
        ITagGatt.addOnITagChangeListener(this);
    }

    @Override
    public void onPause() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "onPause");
        }
        ITagGatt.removeOnITagChangeListener(this);
        ITagsDb.removeListener(this);
        MainActivity.removeServiceBoundListener(this);
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
    public void onITagChange(@NotNull ITagGatt gatt) {
        getActivity().runOnUiThread(() -> {
            // handle cases like "onBound", connect, etc
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
        if (view != null)
            getActivity().runOnUiThread(() -> setupTags((ViewGroup) view));
    }

    @Override
    public void onITagClicked(@NotNull ITagGatt gatt) {
    }

    @Override
    public void onITagDoubleClicked(@NonNull ITagGatt gatt) {
    }

    @Override
    public void onITagFindingPhone(@NonNull ITagGatt gatt, boolean on) {
        View view = getView();
        if (view != null)
            getActivity().runOnUiThread(() -> setupTags((ViewGroup) view));
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
}
