package solutions.s4y.itag;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import solutions.s4y.itag.ble.ITagGatt;
import solutions.s4y.itag.ble.ITagsDb;
import solutions.s4y.itag.ble.ITagDevice;
import solutions.s4y.itag.ble.ITagsService;
import solutions.s4y.itag.ble.LeScanResult;
import solutions.s4y.itag.ble.LeScanner;

public class MainActivity extends Activity implements LeScanner.LeScannerListener, ITagsDb.DbListener {
    static public final int REQUEST_ENABLE_BT = 1;
    static public final int REQUEST_ENABLE_LOCATION = 2;
    public BluetoothAdapter mBluetoothAdapter;
    public ITagsService mITagsService;
    public boolean mITagsServiceBound;

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


    private enum FragmentType {
        OTHER,
        SCANNER
    }

    private FragmentType mSelectedFragment;

    private void setupContent() {
        final FragmentManager fragmentManager = getFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment fragment = null;
        if (LeScanner.isScanning) {
            setupProgressBar();
            if (mSelectedFragment != FragmentType.SCANNER) {
                fragment = new LeScanFragment();
                mSelectedFragment = FragmentType.SCANNER;
            }
        } else {
            mSelectedFragment = FragmentType.OTHER;
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
        if (fragment != null) {
            fragmentTransaction.replace(R.id.content, fragment);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (LeScanner.isScanning && mBluetoothAdapter != null) {
            LeScanner.stopScan(mBluetoothAdapter);
        } else {
            super.onBackPressed();// your code.
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            mITagsService = ((ITagsService.GattBinder) binder).getService();
            mITagsServiceBound = true;
            mITagsService.connect();
            mITagsService.removeFromForeground();
            setupContent();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mITagsServiceBound = false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        setupContent();
        Intent intent = new Intent(this, ITagsService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        LeScanner.addListener(this);
        ITagsDb.addListener(this);
        // run the service on the activity start if there are remebered itags
        ITagsService.start(this);
    }

    @Override
    protected void onPause() {
        if (ITagsDb.getDevices(this).size()>0){
            if (mITagsServiceBound) {
                mITagsService.addToForeground();
            }
        }else{
            ITagsService.stop(this);
        }
        unbindService(mConnection);
        ITagsDb.removeListener(this);
        LeScanner.removeListener(this);
        super.onPause();
    }

    public void onRemember(View sender) {
        BluetoothDevice device = (BluetoothDevice) sender.getTag();
        if (device == null) {
            ITagApplication.handleError(new Exception("No BLE device"));
            return;
        }
        if (!ITagsDb.has(device)) {
            if (BuildConfig.DEBUG) {
                if (mBluetoothAdapter == null) {
                    ITagApplication.handleError(new Exception("onRemember mBluetoothAdapter=null"));
                    return;
                }
            }
            ITagsDb.remember(this, device);
            LeScanner.stopScan(mBluetoothAdapter);
        }
    }

    public void onForget(View sender) {
        ITagDevice device = (ITagDevice) sender.getTag();
        if (device == null) {
            ITagApplication.handleError(new Exception("No device"));
            return;
        }
        if (ITagsDb.has(device)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.confirm_forget)
                    .setTitle(R.string.confirm_title)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> ITagsDb.forget(this, device))
                    .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.cancel())
                    .show();
        }
    }

    public void onITagClick(View sender) {
        if (!mITagsServiceBound)
            return;
        ITagDevice device = (ITagDevice) sender.getTag();
        if (device == null) {
            ITagApplication.handleError(new Exception("No device"));
            return;
        }
        ITagGatt gatt = mITagsService.getGatt(device.addr, true);
        if (gatt.isAlert()){
            gatt.stopAlert();
        }else{
            Toast.makeText(this,R.string.help_longpress,Toast.LENGTH_SHORT).show();
        }
    }

    public void onStartStopScan(View ignored) {
        if (BuildConfig.DEBUG) {
            if (mBluetoothAdapter == null) {
                ITagApplication.handleError(new Exception("onStartStopScan mBluetoothAdapter=null"));
                return;
            }
        }

        if (LeScanner.isScanning) {
            LeScanner.stopScan(mBluetoothAdapter);
        } else {
            LeScanner.startScan(mBluetoothAdapter, this);
        }
    }

    public void onChangeColor(View sender) {
        final ITagDevice device = (ITagDevice) sender.getTag();
        if (device == null) {
            ITagApplication.handleError(new Exception("No device"));
            return;
        }
        final PopupMenu popupMenu = new PopupMenu(this, sender);
        popupMenu.inflate(R.menu.color);
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.black:
                    device.color = ITagDevice.Color.BLACK;
                    break;
                case R.id.white:
                    device.color = ITagDevice.Color.WHITE;
                    break;
                case R.id.red:
                    device.color = ITagDevice.Color.RED;
                    break;
                case R.id.green:
                    device.color = ITagDevice.Color.GREEN;
                    break;
                case R.id.blue:
                    device.color = ITagDevice.Color.BLUE;
                    break;
            }
            ITagsDb.save(MainActivity.this);
            ITagsDb.notifyChange();
            return true;
        });
        popupMenu.show();
    }

    public void onEnableBLEClick(View ignored) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, MainActivity.REQUEST_ENABLE_BT);
    }


    public void onSetName(View sender) {
        final ITagDevice device = (ITagDevice) sender.getTag();
        if (device == null) {
            ITagApplication.handleError(new Exception("No device"));
            return;
        }

        SetNameDialogFragment.device = device;
        new SetNameDialogFragment().show(getFragmentManager(), "setname");
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

    @Override
    public void onStartScan() {
        setupContent();
    }

    @Override
    public void onNewDeviceScanned(LeScanResult result) {
        setupContent();
    }

    @Override
    public void onTick(int tick, int max) {
        setupProgressBar();
    }

    @Override
    public void onStopScan() {
        setupContent();
    }


    @Override
    public void onDbChange() {
        setupContent();
    }

    @Override
    public void onDbAdd(ITagDevice device) {

    }

    @Override
    public void onDbRemove(ITagDevice device) {

    }

}
