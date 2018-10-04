package solutions.s4y.itag;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import solutions.s4y.itag.ble.LeScanObservable;
import solutions.s4y.itag.ble.LeScanResult;
import solutions.s4y.itag.ble.LeScanner;

public class MainActivity extends Activity {
    static public final int REQUEST_ENABLE_BT = 1;
    public BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*
        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();
        String appLinkAction = appLinkIntent.getAction();
        Uri appLinkData = appLinkIntent.getData();
        */
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null)
            mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    private void setupContent() {
        ProgressBar pb = findViewById(R.id.progress);

        final FragmentManager fragmentManager = getFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment fragment;
        if (LeScanner.isScanning) {
            pb.setVisibility(View.VISIBLE);
            fragment = new LeScanFragment();
        } else {
            pb.setVisibility(View.GONE);
            if (mBluetoothAdapter == null) {
                fragment = new NoBLEFragment();
            } else {
                if (mBluetoothAdapter.isEnabled()) {
                    fragment = new BLEFragment();
                } else {
                    fragment = new DisabledBLEFragment();
                }
            }
        }
        fragmentTransaction.replace(R.id.content, fragment);
        fragmentTransaction.commit();
    }

    private CompositeDisposable mDisposables;

    @Override
    protected void onResume() {
        super.onResume();
        setupContent();
        if (BuildConfig.DEBUG) {
            if (mDisposables != null) {
                ITagApplication.errorNotifier.onNext(new Exception("MainActivity has not null mDisposables"));
                mDisposables.dispose();
            }
        }
        mDisposables = new CompositeDisposable();
        mDisposables.add(LeScanner.subject.subscribe(ignored -> setupContent()));
    }

    @Override
    protected void onPause() {
        if (BuildConfig.DEBUG) {
            if (mDisposables == null) {
                ITagApplication.errorNotifier.onNext(new Exception("MainActivity has null mDisposables"));
            }
        }
        if (mDisposables != null) {
            mDisposables.dispose();
            mDisposables = null;
        }
        super.onPause();
    }

    public void onStartStopScan(View ignored) {
        if (LeScanner.isScanning) {
            LeScanner.stopScan();
        } else {
            if (mBluetoothAdapter != null) {
                LeScanner.startScan(mBluetoothAdapter);
            }
        }
    }

    public void onEnableBLEClick(View ignored) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, MainActivity.REQUEST_ENABLE_BT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            setupContent();
        }
    }
}
