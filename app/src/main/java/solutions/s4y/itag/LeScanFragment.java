package solutions.s4y.itag;

import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import solutions.s4y.itag.ble.ITagsDb;
import solutions.s4y.itag.ble.LeScanResult;
import solutions.s4y.itag.ble.LeScanner;


/**
 * A simple {@link Fragment} subclass.
 */
public class LeScanFragment extends Fragment implements LeScanner.LeScannerListener {
    private class Adapter extends ArrayAdapter<LeScanResult> {
        Adapter() {
            super(getActivity(), R.layout.fragment_le_scan_item, LeScanner.results);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_le_scan_item, parent, false);
            }
            LeScanResult r = getItem(position);
            assert r != null;
            String addr = r.device.getAddress();

            TextView tv;
            tv = convertView.findViewById(R.id.text_name);
            tv.setText(r.device.getName());
            tv = convertView.findViewById(R.id.text_addr);
            tv.setText(addr);

            tv = convertView.findViewById(R.id.text_rssi);

            RssiView rssiView = convertView.findViewById(R.id.rssi);
            for (LeScanResult result : LeScanner.results) {
                if (addr.equals(result.device.getAddress())) {
                    int rssi = result.rssi;
                    rssiView.setRssi(rssi);
                    tv.setText(String.format(getString(R.string.rssi), rssi));
                    break;
                }
            }


            ImageView btn = convertView.findViewById(R.id.btn_connect);
            btn.setTag(r.device);

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

    @Override
    public void onResume() {
        super.onResume();
        ITagApplication.faScanView(ITagsDb.getDevices(getActivity()).size()>0);
        updateResultsList();
        LeScanner.addListener(this);
    }

    @Override
    public void onPause() {
        LeScanner.removeListener(this);
        super.onPause();
    }

    private void updateResultsList() {
        View root = getView();
        if (root==null) return;
        final ListView listView = root.findViewById(R.id.results_list);
        final Adapter adapter = ((Adapter) (listView.getAdapter()));
        final int index = listView.getFirstVisiblePosition();
        adapter.notifyDataSetChanged();
        listView.smoothScrollToPosition(index);
        final TextView tv = root.findViewById(R.id.text_scanning);
        if (LeScanner.results.size() > 0) {
            tv.setText(R.string.scanning_more);
        } else if (ITagsDb.getDevices(getActivity()).size() > 0) {
            tv.setText(R.string.scanning_new);
        } else {
            tv.setText(R.string.scanning);
        }
    }

    @Override
    public void onStartScan() {
        updateResultsList();
    }

    @Override
    public void onNewDeviceScanned(LeScanResult result) {
        updateResultsList();
    }

    @Override
    public void onTick(int tick, int max) {
        updateResultsList();
    }

    @Override
    public void onStopScan() {
        updateResultsList();
    }

}
