package solutions.s4y.itag;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Objects;

import solutions.s4y.itag.ble.ITagsDb;
import solutions.s4y.itag.ble.ITagDevice;


/**
 * A simple {@link Fragment} subclass.
 */
public class ITagsFragment extends Fragment implements ITagsDb.DbListener {
    public ITagsFragment() {
        // Required empty public constructor
    }

    private void setupTag(final ITagDevice device, final View itagLayout) {
        final View btnForget = itagLayout.findViewById(R.id.btn_forget);
        btnForget.setTag(device);
        final View btnColor = itagLayout.findViewById(R.id.btn_color);
        btnColor.setTag(device);
        final View btnSetName = itagLayout.findViewById(R.id.btn_set_name);
        btnSetName.setTag(device);
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
        imageITag.setImageResource(imageId);
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
        final int s = ITagsDb.devices.size();
        final int rid = s == 0 ? R.layout.itag_0 : s == 1 ? R.layout.itag_1 : s == 2 ? R.layout.itag_2 : s == 3 ? R.layout.itag_3 : R.layout.itag_4;
        tagsLayout = getActivity().getLayoutInflater().inflate(rid, root, false);
        root.addView(tagsLayout, index);
        if (s > 0) {
            setupTag(ITagsDb.devices.get(0), tagsLayout.findViewById(R.id.tag_1).findViewById(R.id.layout_itag));
        }
        if (s > 1) {
            setupTag(ITagsDb.devices.get(1), tagsLayout.findViewById(R.id.tag_2).findViewById(R.id.layout_itag));
        }
        /*
        if (s>2) {
            View itagLayout = tagsLayout.findViewById(R.id.tag_3);
            View btnForget = itagLayout.findViewById(R.id.btn_forget);
            btnForget.setTag(Db.devices.get(2));
        }
        if (s>3) {
            View itagLayout = tagsLayout.findViewById(R.id.tag_4);
            View btnForget = itagLayout.findViewById(R.id.btn_forget);
            btnForget.setTag(Db.devices.get(3));
        }
*/
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
    }

    @Override
    public void onPause() {
        ITagsDb.removeListener(this);
        super.onPause();
    }

    @Override
    public void onChange() {
        setupTags((ViewGroup) Objects.requireNonNull(getView()));
    }
}
