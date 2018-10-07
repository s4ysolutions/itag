package solutions.s4y.itag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ProgressBar;

import io.reactivex.disposables.CompositeDisposable;
import solutions.s4y.itag.ble.Db;
import solutions.s4y.itag.ble.Device;
import solutions.s4y.itag.ble.LeScanner;

public class MainActivity extends Activity {
    static public final int REQUEST_ENABLE_BT = 1;
    static public final int REQUEST_ENABLE_LOCATION = 2;
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

    private void setupProgressBar() {
        ProgressBar pb = findViewById(R.id.progress);
        if (LeScanner.isScanning) {
            pb.setVisibility(View.VISIBLE);
            pb.setIndeterminate(false);
            pb.setMax(LeScanner.TIMEOUT);
            pb.setProgress(LeScanner.tick);
        } else {
            pb.setVisibility(View.GONE);
        }
    }

    private void setupContent() {
        final FragmentManager fragmentManager = getFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment fragment;
        if (LeScanner.isScanning) {
            setupProgressBar();
            fragment = new LeScanFragment();
        } else {
            setupProgressBar();
            if (mBluetoothAdapter == null) {
                fragment = new NoBLEFragment();
            } else {
                if (mBluetoothAdapter.isEnabled()) {
                    fragment = new ITagsFragment();
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
        mDisposables.add(LeScanner.subjectTimer.subscribe(ignored -> setupProgressBar()));
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

    public void onRemember(View sender) {
        BluetoothDevice device = (BluetoothDevice) sender.getTag();
        if (device == null) {
            ITagApplication.errorNotifier.onNext(new Exception("No BLE device"));
            return;
        }
        if (!Db.has(device)) {
            Db.remember(this, device);
            LeScanner.stopScan();
        }
    }

    public void onForget(View sender) {
        Device device = (Device) sender.getTag();
        if (device == null) {
            ITagApplication.errorNotifier.onNext(new Exception("No device"));
            return;
        }
        if (Db.has(device)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.confirm_forget)
                    .setTitle(R.string.confirm_title)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> Db.forget(this, device))
                    .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.cancel())
                    .show();
            ;
        }
    }

    public void onStartStopScan(View ignored) {
        if (LeScanner.isScanning) {
            LeScanner.stopScan();
        } else {
            if (mBluetoothAdapter != null) {
                LeScanner.startScan(mBluetoothAdapter, this);
            }
        }
    }

    public void onChangeColor(View sender) {
        final Device device = (Device) sender.getTag();
        if (device == null) {
            ITagApplication.errorNotifier.onNext(new Exception("No device"));
            return;
        }
        final PopupMenu popupMenu = new PopupMenu(this, sender);
        popupMenu.inflate(R.menu.color);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.black:
                        device.color=Device.Color.BLACK;
                        break;
                    case R.id.white:
                        device.color=Device.Color.WHITE;
                        break;
                    case R.id.red:
                        device.color=Device.Color.RED;
                        break;
                    case R.id.green:
                        device.color=Device.Color.GREEN;
                        break;
                    case R.id.blue:
                        device.color=Device.Color.BLUE;
                        break;
                }
                Db.save(MainActivity.this);
                Db.subject.onNext(device);
                return true;
            }
        });
        popupMenu.show();
    }

    public void onEnableBLEClick(View ignored) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, MainActivity.REQUEST_ENABLE_BT);
    }


    public void onSetName(View sender) {
        final Device device = (Device) sender.getTag();
        if (device == null) {
            ITagApplication.errorNotifier.onNext(new Exception("No device"));
            return;
        }

        SetNameDialogFragment.device = device;
        new SetNameDialogFragment().show(getFragmentManager(),"setname");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_ENABLE_BT:
                    setupContent();
                    break;
                case REQUEST_ENABLE_LOCATION:
                    if (mBluetoothAdapter != null) {
                        LeScanner.startScan(mBluetoothAdapter, this);
                    }
                    break;
            }
        }

    }

}
