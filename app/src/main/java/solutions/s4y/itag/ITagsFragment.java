package solutions.s4y.itag;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import solutions.s4y.itag.ble.ITagGatt;
import solutions.s4y.itag.ble.ITagsDb;
import solutions.s4y.itag.ble.ITagDevice;
import solutions.s4y.itag.ble.ITagsService;


/**
 * A simple {@link Fragment} subclass.
 */
public class ITagsFragment extends Fragment implements ITagsDb.DbListener, ITagGatt.ITagChangeListener {
    private static final String LT = ITagsFragment.class.getName();

    public ITagsFragment() {
        // Required empty public constructor
    }

    private final View.OnLongClickListener mOnLongClickListener = v -> {
        ITagDevice device = (ITagDevice) v.getTag();
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity.mITagsServiceBound) {
            ITagsService service = mainActivity.mITagsService;
            ITagGatt gatt = service.getGatt(device.addr, true);
            if (gatt.isAlert()) {
                gatt.stopAlert();
            } else {
                gatt.alert();
            }
        }
        return true;
    };

    private void setupTag(final ITagDevice device, final View itagLayout) {
        final View btnForget = itagLayout.findViewById(R.id.btn_forget);
        btnForget.setTag(device);
        final View btnColor = itagLayout.findViewById(R.id.btn_color);
        btnColor.setTag(device);
        final View btnSetName = itagLayout.findViewById(R.id.btn_set_name);
        btnSetName.setTag(device);

        MainActivity mainActivity = (MainActivity) getActivity();
        int statusId = R.drawable.bt_setup;
        Animation animShake = null;
        if (mainActivity.mITagsServiceBound) {
            ITagsService service = mainActivity.mITagsService;
            ITagGatt gatt = service.getGatt(device.addr, false);
            if (gatt.isConnecting() || gatt.isTransmitting()) {
                statusId = R.drawable.bt_call;
            } else if (gatt.isConnected()) {
                statusId = R.drawable.bt;
            }
            if (gatt.isAlert()) {
                animShake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake_itag);
            }
        }

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
        imageITag.setOnLongClickListener(mOnLongClickListener);
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
        imgStatus.setImageResource(statusId);

        final TextView textName = itagLayout.findViewById(R.id.text_name);
//        textName.setText(device.name!=null && device.name.length()>0 ?device.name:device.addr);
        textName.setText(device.name);
    }

    private void setupTags(ViewGroup root) {
        View tagsLayout = root.findViewById(R.id.tags);
        int index = -1;
        if (tagsLayout != null) {
            root.removeView(tagsLayout);
            index = root.indexOfChild(tagsLayout);
        }
        final int s = ITagsDb.getDevices(getActivity()).size();
        final int rid = s == 0 ? R.layout.itag_0 : s == 1 ? R.layout.itag_1 : s == 2 ? R.layout.itag_2 : s == 3 ? R.layout.itag_3 : R.layout.itag_4;
        tagsLayout = getActivity().getLayoutInflater().inflate(rid, root, false);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_itags, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        setupTags((ViewGroup) Objects.requireNonNull(getView()));
        ITagsDb.addListener(this);
        ITagGatt.addOnITagChangeListener(this);
    }

    @Override
    public void onPause() {
        ITagGatt.removeOnITagChangeListener(this);
        ITagsDb.removeListener(this);
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
        getActivity().runOnUiThread(() -> setupTags((ViewGroup) Objects.requireNonNull(getView())));
    }

    @Override
    public void onITagClicked(@NotNull ITagGatt gatt) {

    }
}
