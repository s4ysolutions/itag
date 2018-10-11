package solutions.s4y.itag;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.CompositeDisposable;
import solutions.s4y.itag.ble.Db;
import solutions.s4y.itag.ble.Device;
import solutions.s4y.itag.ble.LeScanResult;
import solutions.s4y.itag.ble.LeScanner;


/**
 * A simple {@link Fragment} subclass.
 */
public class LeScanFragment extends Fragment {
    private class Adapter extends ArrayAdapter<LeScanResult> {
        Adapter() {
            super(getActivity(), R.layout.fragment_le_scan_item, LeScanner.results);
        }

        @Override
        public View getView(@NonNull int position, View convertView, ViewGroup parent) {
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

            ProgressBar pb = convertView.findViewById(R.id.pb_rssi);
            tv = convertView.findViewById(R.id.text_rssi);

            for(LeScanResult result: LeScanner.results) {
                if (addr.equals(result.device.getAddress())){
                    int rssi=result.rssi;
                    tv.setText(String.format(getString(R.string.rssi), rssi));
                    pb.setIndeterminate(false);
                    pb.setMax(120);
                    pb.setProgress(120 + rssi);
                    break;
                }
            }

            ImageView btn = convertView.findViewById(R.id.btn_connect);
            btn.setTag(r.device);
            return convertView;
        }
    }

    private CompositeDisposable mCompositeDisposable;

    public LeScanFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_le_scan, container, false);
        ListView listView = view.findViewById(R.id.results_list);
        listView.setAdapter(new Adapter());
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {
            if (mCompositeDisposable != null) {
                ITagApplication.errorNotifier.onNext(new Exception("LeScanFragment has not null mCompositeDisposable"));
                mCompositeDisposable.dispose();
            }
        }
        mCompositeDisposable = new CompositeDisposable();
        mCompositeDisposable.add(LeScanner.subject.subscribe(ignored -> {
            updateResultsList();
            if (LeScanner.isScanning && LeScanner.lastScanResult != null) {
                MainActivity activity = ((MainActivity) (getActivity()));
                if (activity.mGattBound) {
                    /*
                    activity.mGattService.connect(
                            LeScanner.lastScanResult.device.getAddress(),
                            false);
                            */
                }
            }
        }));
        /*
        mCompositeDisposable.add(GattService.subjectRssi
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(rssiData -> {
                    if (getView() != null) {
                        updateResultsList();
                    }
                }));
          */
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            if (mCompositeDisposable == null) {
                ITagApplication.errorNotifier.onNext(new Exception("LeScanFragment has null mCompositeDisposable"));
            }
        }
        if (mCompositeDisposable != null) {
            mCompositeDisposable.dispose();
            mCompositeDisposable = null;
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateResultsList();
    }

    private void updateResultsList() {
        final ListView listView = Objects.requireNonNull(getView()).findViewById(R.id.results_list);
        final Adapter adapter = ((Adapter) (listView.getAdapter()));
        final int index = listView.getFirstVisiblePosition();
        adapter.notifyDataSetChanged();
        listView.smoothScrollToPosition(index);
        final TextView tv = getView().findViewById(R.id.text_scanning);
        if (LeScanner.results.size() > 0) {
            tv.setText(R.string.scanning_more);
        } else if (Db.devices.size() > 0) {
            tv.setText(R.string.scanning_new);
        } else {
            tv.setText(R.string.scanning);
        }
    }
}
