package s4y.itag;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import s4y.itag.ble.ITagDevice;
import s4y.itag.ble.ITagGatt;
import s4y.itag.ble.ITagsDb;
import s4y.itag.ble.ITagsService;
import s4y.itag.ble.LeScanResult;
import s4y.itag.ble.LeScanner;
import s4y.itag.ble.MediaPlayerUtils;
import s4y.itag.history.HistoryRecord;
import solutions.s4y.waytoday.idservice.IDService;
import solutions.s4y.waytoday.locations.LocationsGPSUpdater;

public class MainActivity extends FragmentActivity implements
        LeScanner.LeScannerListener,
        ITagsDb.DbListener,
        LocationsGPSUpdater.RequestGPSPermissionListener {
    static public final int REQUEST_ENABLE_LOCATION = 2;
    public BluetoothAdapter mBluetoothAdapter;
    public ITagsService mITagsService;
    public boolean mITagsServiceBound;
    public static boolean sIsShown = false;

    private static final String LT = ITagsService.class.getName();

    public interface ServiceBoundListener {
        void onBoundingChanged(@NonNull final MainActivity activity);
    }

    private static final List<ServiceBoundListener> mServiceBoundListeners =
            new ArrayList<>(2);

    public static void addServiceBoundListener(ServiceBoundListener listener) {
        if (BuildConfig.DEBUG) {
            if (mServiceBoundListeners.contains(listener)) {
                ITagApplication.handleError(new Error("Add duplicate ITagChangeListener listener"));
            }
        }
        mServiceBoundListeners.add(listener);
    }

    public static void removeServiceBoundListener(ServiceBoundListener listener) {
        if (BuildConfig.DEBUG) {
            if (!mServiceBoundListeners.contains(listener)) {
                ITagApplication.handleError(new Error("Remove nonexisting ITagChangeListener listener"));
            }
        }
        mServiceBoundListeners.remove(listener);
    }

    private void notifyServiceBoundChanged() {
        for (ServiceBoundListener listener : mServiceBoundListeners) {
            listener.onBoundingChanged(this);
        }
    }

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
        SharedPreferences preferences = (getSharedPreferences("s4y.solutions.itags.prefs", Context.MODE_PRIVATE));
        preferences.edit().putBoolean("loadOnBoot", true).apply();
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
        ITAGS,
        SCANNER
    }

    private FragmentType mSelectedFragment;

    private int mEnableAttempts = 0;

    private boolean isFirstLaunch() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getBoolean("first", true);
    }

    private void setNotFirstLaunch() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sharedPref.edit();
        ed.putBoolean("first", false);
        ed.apply();
    }

    private void setupContent() {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment fragment = null;
        if (LeScanner.isScanning) {
            setupProgressBar();
            mEnableAttempts = 0;
            if (mSelectedFragment != FragmentType.SCANNER) {
                fragment = new LeScanFragment();
                mSelectedFragment = FragmentType.SCANNER;
            }
        } else {
            setupProgressBar();
            if (mBluetoothAdapter == null) {
                fragment = new NoBLEFragment();
                mSelectedFragment = FragmentType.OTHER;
            } else {
                if (mBluetoothAdapter.isEnabled()) {
                    setNotFirstLaunch();
                    mEnableAttempts = 0;
                    if (mSelectedFragment != FragmentType.ITAGS) {
                        fragment = new ITagsFragment();
                        mSelectedFragment = FragmentType.ITAGS;
                    }
                } else {
                    if (mEnableAttempts < 3 && isFirstLaunch()) {
                        mBluetoothAdapter.enable();
                        mEnableAttempts++;
                        if (BuildConfig.DEBUG) {
                            Log.d(LT, "setupContent BT disabled, enable attempt=" + mEnableAttempts);
                        }
                        if (mEnableAttempts == 1) {
                            Toast.makeText(this, R.string.try_enable_bt, Toast.LENGTH_LONG).show();
                        }
                        try {
                            // A bit against rules but ok in this situation
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        setupContent();
                    } else {
                        if (BuildConfig.DEBUG) {
                            Log.d(LT, "setupContent BT disabled, auto enable failed");
                        }
                        fragment = new DisabledBLEFragment();
                        mSelectedFragment = FragmentType.OTHER;
                    }
                }
            }
        }
        if (fragment != null) {
            fragmentTransaction.replace(R.id.content, fragment);
            fragmentTransaction.commitAllowingStateLoss();
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

    @NonNull
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       @NonNull IBinder binder) {
            setBinder(binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mITagsServiceBound = false;
            notifyServiceBoundChanged();
        }
    };

    protected void setBinder(@NonNull IBinder binder) {
        mITagsService = ((ITagsService.GattBinder) binder).getService();
        mITagsServiceBound = true;
//            mITagsService.connectAll();
        mITagsService.removeFromForeground();
        setupContent();
        notifyServiceBoundChanged();
    }


    private boolean mHasFocus = false;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus != mHasFocus) {
            mHasFocus = hasFocus;
            if (mIsServiceStartedUnbind) {
                if (mHasFocus && mITagsService != null) {
                    mITagsService.removeFromForeground();
                }
            }
        }
    }

    private boolean mIsServiceStartedUnbind;

    @Override
    protected void onResume() {
        super.onResume();
        sIsShown = true;
        setupContent();
        Intent intent = new Intent(this, ITagsService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        LeScanner.addListener(this);
        ITagsDb.addListener(this);
        mIsServiceStartedUnbind = ITagsService.start(this, !mHasFocus);
        // run the service on the activity start if there are remebered itags
    }


    @Override
    protected void onPause() {
        sIsShown = false;
        if (ITagsDb.getDevices(this).size() > 0) {
            if (!mIsServiceStartedUnbind) {
                ITagsService.start(this, true);
            } else {
                if (mITagsServiceBound) {
                    mITagsService.addToForeground();
                }
            }
        } else {
            ITagsService.stop(this);
        }
        try {
            unbindService(mConnection);
        } catch (IllegalArgumentException e) {
            ITagApplication.handleError(e, false);
        }
        ITagsDb.removeListener(this);
        LeScanner.removeListener(this);
        super.onPause();
    }

    public void onRemember(@NonNull View sender) {
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

    public void onForget(@NonNull View sender) {
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

    public void onLocationClick(@NonNull View sender) {
        ITagDevice device = (ITagDevice) sender.getTag();
        if (device == null) {
            ITagApplication.handleError(new Exception("No device"));
            return;
        }

        HistoryRecord record = HistoryRecord.getHistoryRecords(this).get(device.addr);
        if (record != null) {
            ITagApplication.faShowLastLocation();
            long ts;
            String unit;
            long d = (System.currentTimeMillis() - record.ts) / 1000;
            if (d < 60) {
                ts = d;
                unit = getString(R.string.uint_sec);
            } else if (d < 3600) {
                ts = d / 60;
                unit = getString(R.string.uint_min);
            } else {
                ts = (d / 3600) + 1;
                unit = getString(R.string.uint_hour);
            }
            String uri = String.format(
                    Locale.ENGLISH, "geo:%f,%f?q=%f,%f(%s)",
                    record.latitude,
                    record.longitude,
                    record.latitude,
                    record.longitude,
                    String.format(Locale.ENGLISH, getString(R.string.last_seen), ts, unit));
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));

            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
            boolean isIntentSafe = activities.size() > 0;

            if (isIntentSafe) {
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    ITagApplication.handleError(e);
                    Toast.makeText(this, R.string.no_geo_activity, Toast.LENGTH_LONG).show();
                }
            } else {
                ITagApplication.handleError(new Exception("No Activity for geo"));
                Toast.makeText(this, R.string.no_geo_activity, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void onITagClick(@NonNull View sender) {
        if (!mITagsServiceBound)
            return;
        ITagDevice device = (ITagDevice) sender.getTag();
        if (device == null) {
            ITagApplication.handleError(new Exception("No device"));
            return;
        }
        // NOTE: will reconnect if not connected
        //       ergo error reset here
        ITagGatt gatt = mITagsService.getGatt(device.addr, false);
        boolean needNotify = true;
        if (gatt.isFindingITag()) {
            gatt.stopFindITag();
            needNotify = false;
        }
        if (gatt.isError()) {
            gatt.connect(this);
            needNotify = false;
        }
        if (gatt.isFindingPhone()) {
            gatt.stopFindPhone();
        }
        if (mITagsServiceBound && MediaPlayerUtils.getInstance().isSound()) {
            MediaPlayerUtils.getInstance().stopSound(this);
        }
        if (needNotify) {
            Toast.makeText(this, R.string.help_longpress, Toast.LENGTH_SHORT).show();
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

    public void onAppMenu(@NonNull View sender) {
        final PopupMenu popupMenu = new PopupMenu(this, sender);
        popupMenu.inflate(R.menu.app);
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.exit:
                    SharedPreferences preferences = (getSharedPreferences("s4y.solutions.itags.prefs", Context.MODE_PRIVATE));
                    preferences.edit().putBoolean("loadOnBoot", false).apply();
                    ITagsService.stop(this);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        finishAndRemoveTask();
                    } else {
                        finishAffinity();
                    }
                    break;
            }
            return true;
        });
        popupMenu.show();
    }

    public void onWaytoday(@NonNull View sender) {
        if (!mITagsServiceBound || mITagsService == null)
            return;
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String tid = sp.getString("tid", "");
        boolean on = sp.getBoolean("wt", false);
        boolean first = sp.getBoolean("wtfirst", true);
        int freq = sp.getInt("freq", -1);
        final PopupMenu popupMenu = new PopupMenu(this, sender);
        popupMenu.inflate(R.menu.waytoday);
        popupMenu.getMenu().findItem(R.id.wt_about_first).setVisible(first);
        popupMenu.getMenu().findItem(R.id.wt_sec_1).setChecked(on && freq == 1);
        popupMenu.getMenu().findItem(R.id.wt_min_5).setChecked(on && freq == 300000);
        popupMenu.getMenu().findItem(R.id.wt_hour_1).setChecked(on && freq == 3600000);
        popupMenu.getMenu().findItem(R.id.wt_off).setVisible(on);
        popupMenu.getMenu().findItem(R.id.wt_new_tid).setVisible(!("".equals(tid) && first));
        popupMenu.getMenu().findItem(R.id.wt_about).setVisible(!first);
        popupMenu.getMenu().findItem(R.id.wt_share).setVisible(!"".equals(tid));
        popupMenu.setOnMenuItemClickListener(item -> {
            AlertDialog.Builder builder;
            switch (item.getItemId()) {
                case R.id.wt_sec_1:
                    mITagsService.startWayToday(1);
                    ITagApplication.faWtOn1();
                    break;
                case R.id.wt_min_5:
                    mITagsService.startWayToday(300000);
                    break;
                case R.id.wt_hour_1:
                    mITagsService.startWayToday(3600000);
                    ITagApplication.faWtOn300();
                    break;
                case R.id.wt_off:
                    mITagsService.stopWayToday();
                    ITagApplication.faWtOff();
                    break;
                case R.id.wt_new_tid:
                    ITagApplication.faWtChangeID();
                    IDService.enqueueRetrieveId(this);
                    break;
                case R.id.wt_share:
                    ITagApplication.faWtShare();
                    if (!"".equals(tid)) {
                        String txt = String.format(getResources().getString(R.string.share_link), tid);
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, txt);
                        sendIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share_subj));
                        // sendIntent.setType("message/rfc822");
                        sendIntent.setType("text/plain");
                        startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.share_title)));
                    }
                    break;
                case R.id.wt_about_first:
                case R.id.wt_about:
                    ITagApplication.faWtAbout();
                    builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.about_wt)
                            .setMessage(R.string.about_message)
                            .setPositiveButton(R.string.about_ok, (dialog, id) -> {
                                ITagApplication.faWtPlaymarket();
                                final String appPackageName = "solutions.s4y.waytoday";
                                try {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                                } catch (ActivityNotFoundException anfe) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                                }
                            })
                            .setNegativeButton(R.string.about_cancel, (dialog, id) -> {
                                // User cancelled the dialog
                            });
                    // Create the AlertDialog object and return it
                    builder.create().show();
                    break;
                case R.id.wt_disable:
                    builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.disable_wt_ok)
                            .setMessage(R.string.disable_wt_msg)
                            .setPositiveButton(R.string.disable_wt_ok, (dialog, id) -> {
                                ITagApplication.faWtRemove();
                                mITagsService.stopWayToday();
                                sp.edit().putBoolean("wt_disabled", true).apply();
                                final View v = findViewById(R.id.btn_waytoday);
                                if (v != null) {
                                    v.setVisibility(View.GONE);
                                }
                            })
                            .setNegativeButton(R.string.about_cancel, (dialog, id) -> {
                                // User cancelled the dialog
                            });
                    // Create the AlertDialog object and return it
                    builder.create().show();
                    break;
            }
            return true;
        });
        popupMenu.show();
    }

    public void onChangeColor(@NonNull View sender) {
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
            ITagApplication.faColorITag();
            return true;
        });
        popupMenu.show();
    }

    public void onLink(@NonNull View sender) {
        ITagDevice device = (ITagDevice) sender.getTag();
        device.linked = !device.linked;
        ITagsDb.save(MainActivity.this);
        ITagsDb.notifyChange();
        if (device.linked)
            ITagApplication.faUnmuteTag();
        else
            ITagApplication.faMuteTag();
        if (mITagsServiceBound && MediaPlayerUtils.getInstance().isSound() && !device.linked) {
            MediaPlayerUtils.getInstance().stopSound(this);
        }
    }


    public void onSetName(@NonNull View sender) {
        final ITagDevice device = (ITagDevice) sender.getTag();
        if (device == null) {
            ITagApplication.handleError(new Exception("No device"));
            return;
        }

        SetNameDialogFragment.device = device;
        new SetNameDialogFragment().show(getSupportFragmentManager(), "setname");
    }

    /*
    1. do not want to use appcompat
    2. v28 of appcompat is not compatible with com.google.firebase:firebase-core:16.0.4
        @Override
        protected void onRequestPermissionsResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    case REQUEST_ENABLE_BT:
                        setupContent();
                        break;
                    case REQUEST_ENABLE_LOCATION:
                        if (LeScanner.isScanRequestAbortedBecauseOfPermission && mBluetoothAdapter != null) {
                            LeScanner.startScan(mBluetoothAdapter, this);
                        }
                        break;
                }
            }

        }
    */

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

    public void onOpenBTSettings(View ignored) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivity(enableBtIntent);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11. issue #54
    }

    @Override
    public void onGPSPermissionRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.request_location_permission)
                        .setTitle(R.string.request_permission_title)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MainActivity.REQUEST_ENABLE_LOCATION))
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                        .show();
            } else {
                // isScanRequestAbortedBecauseOfPermission=true;
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MainActivity.REQUEST_ENABLE_LOCATION);
            }
        }
    }
}
