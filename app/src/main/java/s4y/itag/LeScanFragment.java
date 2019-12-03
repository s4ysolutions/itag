package s4y.itag;

import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import s4y.itag.ble.BLEScanResult;
import s4y.itag.itag.ITag;
import s4y.rasat.DisposableBag;

/**
 * A simple {@link Fragment} subclass.
 */
public class LeScanFragment extends Fragment {
    private final DisposableBag disposableBag = new DisposableBag();

    private class Adapter extends ArrayAdapter<BLEScanResult> {
        Adapter() {
            //noinspection ConstantConditions
            super(getActivity(), R.layout.fragment_le_scan_item, new ArrayList<>());
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_le_scan_item, parent, false);
            }
            BLEScanResult r = getItem(position);
            assert r != null;
            String addr = r.peripheral.address();

            TextView tv;
            tv = convertView.findViewById(R.id.text_name);
            tv.setText(r.peripheral.name());
            tv = convertView.findViewById(R.id.text_addr);
            tv.setText(addr);

            tv = convertView.findViewById(R.id.text_rssi);

            RssiView rssiView = convertView.findViewById(R.id.rssi);
            for (int i = 0; i < getCount(); i++) {
                final BLEScanResult result = getItem(i);
                if (result == null) {
                    continue;
                }
                if (addr.equals(result.peripheral.address())) {
                    int rssi = result.rssi;
                    rssiView.setRssi(rssi);
                    if (getActivity() != null && isAdded()) {
                        // issue #38 Fragment not attached to Activity
                        tv.setText(String.format(getString(R.string.rssi), rssi));
                    } else if (ITagApplication.context != null) {
                        tv.setText(String.format(ITagApplication.context.getString(R.string.rssi), rssi));
                    } else {
                        tv.setText("");
                    }
                    break;
                }
            }


            ImageView btn = convertView.findViewById(R.id.btn_connect);
            btn.setTag(r.peripheral);

            if (position % 2 == 1) {
                convertView.findViewById(R.id.item_root).setBackgroundColor(0xffe0e0e0);
            } else {
                convertView.findViewById(R.id.item_root).setBackgroundColor(Color.TRANSPARENT);
            }
            return convertView;
        }
    }

    public LeScanFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_le_scan, container, false);
        ListView listView = view.findViewById(R.id.results_list);
        listView.setAdapter(new Adapter());
        return view;
    }

    private ListView listView() {
        View root = getView();
        if (root == null) return null;
        return root.findViewById(R.id.results_list);
    }

    private Adapter adapter(ListView listView){
        if (listView == null) {
            return null;
        }
        return ((Adapter) (listView.getAdapter()));
    }

    private Adapter adapter() {
        return adapter(listView());
    }

    @Override
    public void onResume() {
        super.onResume();
        ITagApplication.faScanView(ITag.store.count() > 0);
        updateResultsList();
        disposableBag.add(
                ITag.ble.scanner().observableScan().subscribe((result) -> {
                    Adapter adapter = adapter();
                    if (adapter == null) {
                        return;
                    }

                    for (int i=0; i < adapter.getCount(); i++){
                        BLEScanResult a = adapter.getItem(i);
                        if (a == null) {
                            return;
                        }

                        if (a.peripheral.address().equals(result.peripheral.address())) {
                           return;
                        }
                    }
                    adapter.add(result);
                    adapter.notifyDataSetChanged();
                    updateResultsList();
                })
        );
        /*
        disposableBag.add(
                ITag.ble.scanner().observableTimer().subscribe((Integer tick) -> updateResultsList())
        );
         */
        disposableBag.add(
                ITag.ble.scanner().observableActive().subscribe((Boolean active) -> {
                    if (!active) {
                        return;
                    }
                    Adapter adapter = adapter();
                    if (adapter == null) {
                        return;
                    }
                    adapter.clear();
                    adapter.notifyDataSetChanged();
                    updateResultsList();
                })
        );
    }

    @Override
    public void onPause() {
        disposableBag.dispose();
        super.onPause();
    }

    private void updateResultsList() {
        View root = getView();
        if (root == null) return;
        final ListView listView = root.findViewById(R.id.results_list);
        final Adapter adapter = ((Adapter) (listView.getAdapter()));
        final TextView tv = root.findViewById(R.id.text_scanning);
        if (adapter.getCount() > 0) {
            tv.setText(R.string.scanning_more);
        } else if (ITag.store.count() > 0) {
            tv.setText(R.string.scanning_new);
        } else {
            tv.setText(R.string.scanning);
        }
    }
}
