package solutions.s4y.itag;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Objects;

import solutions.s4y.itag.ble.Db;
import solutions.s4y.itag.ble.Device;
import solutions.s4y.itag.ble.LeScanResult;


/**
 * A simple {@link Fragment} subclass.
 */
public class ITagsFragment extends Fragment {
    public ITagsFragment() {
        // Required empty public constructor
    }

    private void setupTags(ViewGroup root) {
        View tagsLayout = root.findViewById(R.id.tags);
        int index = -1;
        if (tagsLayout != null) {
            root.removeView(tagsLayout);
            index = root.indexOfChild(tagsLayout);
        }
        int s = Db.devices.size();
        int rid = s == 0 ? R.layout.itag_0 : s == 1 ? R.layout.itag_1 : s == 2 ? R.layout.itag_2 : s == 3 ? R.layout.itag_3 : R.layout.itag_4;
        tagsLayout = getActivity().getLayoutInflater().inflate(rid, root, false);
        root.addView(tagsLayout,index);
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
        setupTags((ViewGroup)Objects.requireNonNull(getView()));
    }

    @Override
    public void onPause() {
        super.onPause();
    }

}
