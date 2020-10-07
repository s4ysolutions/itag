package s4y.itag;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import s4y.itag.ble.BLEScanResult;
import s4y.itag.ble.BuildConfig;
import s4y.itag.itag.ITag;
import solutions.s4y.rasat.DisposableBag;

/**
 * A simple {@link Fragment} subclass.
 */
public class ScanFragment extends Fragment {
    private static final String LT = ScanFragment.class.getName();
    private final DisposableBag disposableBag = new DisposableBag();
    private final List<BLEScanResult> scanResults = new ArrayList<>();
    private Adapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_le_scan, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.results_list);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        adapter = new Adapter();
        recyclerView.setAdapter(adapter);
        return view;
    }

    /*
        private ListView listView() {
            View root = getView();
            if (root == null) return null;
            return root.findViewById(R.id.results_list);
        }

        private Adapter adapter(ListView listView) {
            if (listView == null) {
                return null;
            }
            return ((Adapter) (listView.getAdapter()));
        }

        private Adapter adapter() {
            return adapter(listView());
        }
    */
    @Override
    public void onResume() {
        super.onResume();
        ITagApplication.faScanView(ITag.store.count() > 0);
        disposableBag.add(
                ITag.ble.scanner().observableScan().subscribe((result) -> {
                    if (ITag.store.remembered(result.id)) {
                        return;
                    }
                    if (adapter == null) {
                        return;
                    }

                    boolean found = false;
                    boolean modified = false;
                    for (BLEScanResult scanResult : scanResults) {
                        if (scanResult.id.equals(result.id)) {
                            if (scanResult.rssi != result.rssi) {
                                modified = true;
                                scanResult.rssi = result.rssi;
                            }
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        scanResults.add(result);
                    }
                    if (!found || modified) {
                        adapter.notifyDataSetChanged();
                    }
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
                    if (adapter == null) {
                        return;
                    }
                    scanResults.clear();
                    adapter.notifyDataSetChanged();
                })
        );
    }

    public ScanFragment() {
        // Required empty public constructor
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textName;
        final TextView textAddr;
        final TextView textRSSI;
        final RssiView rssiView;
        final ImageView btnRemember;
        final View btnRemember2;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            textName = rootView.findViewById(R.id.text_name);
            textAddr = rootView.findViewById(R.id.text_addr);
            textRSSI = rootView.findViewById(R.id.text_rssi);
            rssiView = rootView.findViewById(R.id.rssi);
            btnRemember = rootView.findViewById(R.id.btn_connect);
            btnRemember2 = rootView.findViewById(R.id.btn_connect_2);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_le_scan_item, parent, false);
            return new ViewHolder(root);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onBindViewHolder position=" + position + " thread=" + Thread.currentThread().getName());
            }
            BLEScanResult r = scanResults.get(position);
            assert r != null;

            holder.textName.setText(r.name);
            holder.textAddr.setText(r.id);
            holder.btnRemember.setTag(r);
            holder.btnRemember2.setTag(r);
            if (position % 2 == 1) {
                holder.itemView.setBackgroundColor(0xffe0e0e0);
            } else {
                holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            }
            holder.rssiView.setRssi(r.rssi);
            if (getActivity() != null && isAdded()) {
                // issue #38 Fragment not attached to Activity
                holder.textRSSI.setText(String.format(getString(R.string.rssi), r.rssi));
            } else if (ITagApplication.context != null) {
                holder.textRSSI.setText(String.format(ITagApplication.context.getString(R.string.rssi), r.rssi));
            } else {
                holder.textRSSI.setText("");
            }
        }

        @Override
        public int getItemCount() {
            return scanResults.size();
        }
/*
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "getView position=" + position + " thread=" + Thread.currentThread().getName());
            }
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_le_scan_item, parent, false);
            }
            BLEScanResult r = getItem(position);
            assert r != null;
            String addr = r.id;

            TextView tv;
            tv = convertView.findViewById(R.id.text_name);
            tv.setText(r.name);
            tv = convertView.findViewById(R.id.text_addr);
            tv.setText(addr);

            tv = convertView.findViewById(R.id.text_rssi);

            RssiView rssiView = convertView.findViewById(R.id.rssi);
            for (int i = 0; i < getCount(); i++) {
                final BLEScanResult result = getItem(i);
                if (result == null) {
                    continue;
                }
                if (addr.equals(result.id)) {
                    Integer rssiLast = id2rssi.get(result.id);
                    int rssi = rssiLast == null ? result.rssi : rssiLast;
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
            btn.setTag(r);

            if (position % 2 == 1) {
                convertView.findViewById(R.id.item_root).setBackgroundColor(0xffe0e0e0);
            } else {
                convertView.findViewById(R.id.item_root).setBackgroundColor(Color.TRANSPARENT);
            }
            return convertView;
        }

 */
    }

    @Override
    public void onPause() {
        disposableBag.dispose();
        super.onPause();
    }
/*
    private void updateResultsList() {
        View root = getView();
        if (root == null) return;
        final ListView listView = root.findViewById(R.id.results_list);
        final Adapter adapter = ((Adapter) (listView.getAdapter()));
        final TextView tv = root.findViewById(R.id.text_scanning);
        if (ITag.ble.scanner().observableTimer().value() > 0) {
            if (adapter.getCount() > 0) {
                tv.setText(R.string.scanning_more);
            } else if (ITag.store.count() > 0) {
                tv.setText(R.string.scanning_new);
            } else {
                tv.setText(R.string.scanning);
            }
        } else {
            tv.setText(R.string.scanning_stopped);
        }
    }
 */
}
