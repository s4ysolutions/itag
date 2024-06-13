package s4y.itag;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import s4y.gps.sdk.android.GPSPermissionManager;
import s4y.gps.sdk.android.GPSPowerManager;
import s4y.gps.sdk.android.GPSUpdatesForegroundService;
import s4y.itag.ble.AlertVolume;
import s4y.itag.ble.BLEConnectionInterface;
import s4y.itag.ble.BLEState;
import s4y.itag.history.HistoryRecord;
import s4y.itag.itag.ITag;
import s4y.itag.itag.ITagInterface;
import s4y.itag.itag.TagColor;
import s4y.itag.preference.WayTodayDisabled0Preference;
import s4y.itag.preference.WayTodayFirstPreference;
import s4y.itag.waytoday.WayToday;
import solutions.s4y.rasat.DisposableBag;

public class MainActivity extends FragmentActivity {
    static public final int REQUEST_ENABLE_BT = 1;
    static public final int REQUEST_ONSCAN = 2;
    public ITagsService iTagsService;
    public static boolean sIsShown = false;
    private static final String LT = MainActivity.class.getName();

    private final DisposableBag disposableBag = new DisposableBag();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            String gpsNotification = intent.getStringExtra(GPSUpdatesForegroundService.GPS_SERVICE_NOTIFICATION);
            if (Objects.equals(gpsNotification, GPSUpdatesForegroundService.ACTION_STOP_TRACKING)) {
                WayToday.getInstance().turnTrackingOff();
                Toast.makeText(this, R.string.wt_tracking_stopped, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupProgressBar() {
        ProgressBar pb = findViewById(R.id.progress);
        if (ITag.ble.scanner().isScanning()) {
            pb.setVisibility(View.VISIBLE);
            pb.setIndeterminate(false);
            pb.setMax(ITag.SCAN_TIMEOUT);
            pb.setProgress(ITag.ble.scanner().observableTimer().value());
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

    static class ITagServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ITagsService.ITagBinder itagService = (ITagsService.ITagBinder) service;
            itagService.removeFromForeground();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    private final ITagServiceConnection mServiceConnection = new ITagServiceConnection();

    /*
    TODO: error handling
    private final ErrorsObservable.IErrorListener mErrorListener = errorNotification -> {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, errorNotification.getMessage(), Toast.LENGTH_LONG).show());
        Log.e(LT, errorNotification.getMessage(), errorNotification.th);
    };
     */
    // TODO: private final IPermissionListener gpsPermissionListener = () -> PermissionHandling.requestPermissions(MainActivity.this);
    private int resumeCount = 0;

    @Override
    protected void onResume() {
        super.onResume();
        // TODO: ErrorsObservable.addErrorListener(mErrorListener);
        sIsShown = true;
        setupContent();
        // TODO: Waytoday.gpsLocationUpdater.addOnPermissionListener(gpsPermissionListener);
        disposableBag.add(ITag.ble.observableState().subscribe(event -> setupContent()));
        disposableBag.add(ITag.ble.scanner().observableActive().subscribe(
                event -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(LT, "ble.scanner activeEvent=" + event + " isScanning=" + ITag.ble.scanner().isScanning() + " thread=" + Thread.currentThread().getName());
                    }
                    setupContent();
                    setupProgressBar();
                }
        ));
        disposableBag.add(ITag.ble.scanner().observableTimer().subscribe(
                event -> setupProgressBar()
        ));
        disposableBag.add(ITag.store.observable().subscribe(event -> {
            switch (event.op) {
                case change:
                    setupContent();
                    break;
                case forget:
                    break;
            }
        }));
        bindService(ITagsService.intentBind(this), mServiceConnection, 0);
        // unconditionally stop background service
        if (WayToday.getInstance().isTrackingOn()) {
            if (GPSPermissionManager.needPermissionRequest(this)) {
                new Handler(getMainLooper()).post(() ->
                        GPSPermissionManager.requestPermissions(this)
                );
            } else if (GPSPowerManager.needRequestIgnoreOptimization(this)) {
                if (resumeCount++ > 1) {
                    new Handler(getMainLooper()).post(() ->
                            GPSPowerManager.requestIgnoreOptimization(this)
                    );
                }
            }
        }
    }

    @Override
    protected void onPause() {
        try {
            unbindService(mServiceConnection);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        disposableBag.dispose();
        sIsShown = false;
        if (!exitting) {
            if (ITag.store.isDisconnectAlert()) {
                ITagsService.start(this);
            } else {
                ITagsService.stop(this);
            }

            if (WayToday.getInstance().isTrackingOn() && !GPSPermissionManager.needPermissionRequest(this)) {
                GPSUpdatesForegroundService.start(this);
            }
        }

        // TODO: Waytoday.gpsLocationUpdater.removePermissionListener(gpsPermissionListener);
        super.onPause();
    }

    private boolean mHasFocus = false;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus != mHasFocus) {
            mHasFocus = hasFocus;
            if (mHasFocus) {
                GPSUpdatesForegroundService.removeFromForeground(this);
                if (iTagsService != null) {
                    iTagsService.removeFromForeground();
                }
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        //No call for super(). Bug on API Level > 11. issue #54
    }

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
        if (BuildConfig.DEBUG) {
            Log.d(LT, "setupContent isScanning=" + ITag.ble.scanner().isScanning() + " thread=" + Thread.currentThread().getName());
        }
        if (ITag.ble.scanner().isScanning()) {
            setupProgressBar();
            mEnableAttempts = 0;
            if (mSelectedFragment != FragmentType.SCANNER) {
                fragment = new ScanFragment();
                mSelectedFragment = FragmentType.SCANNER;
            }
        } else {
            setupProgressBar();
            if (ITag.ble.state() == BLEState.NO_ADAPTER) {
                fragment = new NoBLEFragment();
                mSelectedFragment = FragmentType.OTHER;
            } else {
                if (ITag.ble.state() == BLEState.OK) {
                    setNotFirstLaunch();
                    mEnableAttempts = 0;
                    if (mSelectedFragment != FragmentType.ITAGS) {
                        fragment = new ITagsFragment();
                        mSelectedFragment = FragmentType.ITAGS;
                    }
                } else {
                    if (mEnableAttempts < 60 && isFirstLaunch()) {
                        mEnableAttempts++;
                        if (BuildConfig.DEBUG) {
                            Log.d(LT, "setupContent BT disabled, enable attempt=" + mEnableAttempts);
                        }
                        if (mEnableAttempts == 1) {
                            Toast.makeText(this, R.string.try_enable_bt, Toast.LENGTH_LONG).show();
                        }
                        ITag.ble.enable();
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
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fragmentTransaction.replace(R.id.content, fragment);
            fragmentTransaction.commitAllowingStateLoss();
        }
    }

    @Override
    public void onBackPressed() {
        if (ITag.ble.scanner().isScanning()) {
            ITag.ble.scanner().stop();
        } else {
            super.onBackPressed();// your code.
        }
    }

    public void onForget(@NonNull View sender) {
        ITagInterface itag = (ITagInterface) sender.getTag();
        if (itag != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.confirm_forget)
                    .setTitle(R.string.confirm_title)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> ITag.store.forget(itag))
                    .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.cancel())
                    .show();
        }
    }

    public void onLocationClick(@NonNull View sender) {
        String id = (String) sender.getTag();
        if (id == null) {
            ITagApplication.handleError(new Exception("No Tag ID onLocationClick"));
            return;
        }

        HistoryRecord record = HistoryRecord.getHistoryRecords(this).get(id);
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

            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                ITagApplication.handleError(e);
                Toast.makeText(this, R.string.no_geo_activity, Toast.LENGTH_LONG).show();
            }

            /* NOTE: removed because of extra permission, it is not needed, because of Exception above
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
             */
        }
    }

    public void onITagClick(@NonNull View sender) {
        ITagInterface itag = (ITagInterface) sender.getTag();
        if (itag == null) {
            return;
        }
        MediaPlayerUtils.getInstance().stopSound(this);
        final BLEConnectionInterface connection = ITag.ble.connectionById(itag.id());
        Notifications.cancelDisconnectNotification(this);
        if (connection.isFindMe()) {
            connection.resetFindeMe();
        } else if (connection.isConnected()) {
            new Thread(() -> {
                if (connection.isAlerting()) {
                    connection.writeImmediateAlert(AlertVolume.NO_ALERT, ITag.BLE_TIMEOUT);
                } else {
                    connection.writeImmediateAlert(AlertVolume.HIGH_ALERT, ITag.BLE_TIMEOUT);
                }
            }).start();
        } else {
            if (!itag.isAlertDisconnected()) {
                // there's no sense to communicate if the connection
                // in the connecting state
                ITag.connectAsync(connection, false, () -> {
                    if (connection.isAlerting()) {
                        connection.writeImmediateAlert(AlertVolume.NO_ALERT, ITag.BLE_TIMEOUT);
                    } else {
                        connection.writeImmediateAlert(AlertVolume.HIGH_ALERT, ITag.BLE_TIMEOUT);
                    }

                });
            }
        }
    }

    public void onStartStopScan(View ignored) {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "onStartStopScan isScanning=" + ITag.ble.scanner().isScanning() + " thread=" + Thread.currentThread().getName());
        }
        if (ITag.ble.scanner().isScanning()) {
            ITag.ble.scanner().stop();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(R.string.request_location_permission)
                                .setTitle(R.string.request_permission_title)
                                .setPositiveButton(android.R.string.ok,
                                        (dialog, which) ->
                                                requestAllPermissions(MainActivity.REQUEST_ONSCAN))
                                .setNegativeButton(android.R.string.cancel,
                                        (dialog, which) ->
                                                dialog.cancel())
                                .show();
                    } else {
                        // isScanRequestAbortedBecauseOfPermission=true;
                        requestAllPermissions(MainActivity.REQUEST_ONSCAN);
                    }
                    return;
                }
            }
            ITag.ble.scanner().start(ITag.SCAN_TIMEOUT, new String[]{});
        }
    }

    private void requestAllPermissions(int code) {
        ArrayList<String> permissionsList = new ArrayList<>();
        permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissionsList.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissionsList.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        String[] permissions = permissionsList.toArray(new String[0]);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
            ITagApplication.faWtRequestBlePermissions();

        requestPermissions(permissions, code);
    }

    private Boolean exitting = false;

    public void onAppMenu(@NonNull View sender) {
        final PopupMenu popupMenu = new PopupMenu(this, sender);
        popupMenu.inflate(R.menu.app);
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.exit) {
                exitting = true;
                ITag.close();
                WayToday.getInstance().gpsUpdatesManager.stop();
                ITagsService.stop(this);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAndRemoveTask();
                } else {
                    finishAffinity();
                }
                new Handler(getMainLooper()).postDelayed(() -> System.exit(0), 5000);
            }
            return true;
        });
        popupMenu.show();
    }

    public void onWaytoday(@NonNull View sender) {
        boolean first = WayTodayFirstPreference.get(this);
        String tid = WayToday.getInstance().wtClient.getCurrentTrackerId();
        boolean on = WayToday.getInstance().isTrackingOn() && !GPSPermissionManager.needPermissionRequest(this);
        int freq = WayToday.getInstance().gpsUpdatesManager.getIntervalSec();
        final PopupMenu popupMenu = new PopupMenu(this, sender);
        popupMenu.inflate(R.menu.waytoday);
        popupMenu.getMenu().findItem(R.id.wt_about_first).setVisible(first);
        popupMenu.getMenu().findItem(R.id.wt_sec_1).setChecked(on && freq == 1);
        popupMenu.getMenu().findItem(R.id.wt_min_5).setChecked(on && freq == 300);
        popupMenu.getMenu().findItem(R.id.wt_hour_1).setChecked(on && freq == 3600);
        popupMenu.getMenu().findItem(R.id.wt_off).setVisible(on);
        popupMenu.getMenu().findItem(R.id.wt_new_tid).setVisible(!(tid.isEmpty()));
        popupMenu.getMenu().findItem(R.id.wt_about).setVisible(!first);
        popupMenu.getMenu().findItem(R.id.wt_share).setVisible(!tid.isEmpty());
        popupMenu.getMenu().findItem(R.id.wt_map).setVisible(!tid.isEmpty());
        popupMenu.setOnMenuItemClickListener(item -> {
            AlertDialog.Builder builder;
            int id = item.getItemId();
            if (id == R.id.wt_sec_1) {
                WayToday.getInstance().gpsUpdatesManager.setIntervalSec(1);
                ITagApplication.faWtOn1();
            } else if (id == R.id.wt_min_5) {
                WayToday.getInstance().gpsUpdatesManager.setIntervalSec(5 * 60);
                ITagApplication.faWtOn5();
            } else if (id == R.id.wt_hour_1) {
                WayToday.getInstance().gpsUpdatesManager.setIntervalSec(60 * 60);
                ITagApplication.faWtOn3600();
            } else if (id == R.id.wt_off) {
                WayToday.getInstance().turnTrackingOff();
                WayToday.getInstance().gpsUpdatesManager.stop();
                ITagApplication.faWtOff();
            } else if (id == R.id.wt_new_tid) {
                ITagApplication.faWtChangeID();
                WayToday.getInstance().enqueueTrackIdWorkRequest(this);
            } else if (id == R.id.wt_map) {
                if (!tid.isEmpty()) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://way.today/#" + tid)));
                }
            } else if (id == R.id.wt_share) {
                ITagApplication.faWtShare();
                if (!tid.isEmpty()) {
                    String txt = String.format(getResources().getString(R.string.share_link), tid);
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, txt);
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share_subj));
                    // sendIntent.setType("message/rfc822");
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.share_title)));
                }
            } else if (id == R.id.wt_about_first || id == R.id.wt_about) {
                ITagApplication.faWtAbout();
                WayTodayFirstPreference.set(this, false);
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.about_wt)
                        .setMessage(R.string.about_message)
                        .setPositiveButton(R.string.about_ok, (dialog, ignored) -> {
                            ITagApplication.faWtPlaymarket();
                            final String appPackageName = "s4y.waytoday";
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                            } catch (ActivityNotFoundException anfe) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                            }
                        })
                        .setNegativeButton(R.string.about_cancel, (dialog, ignoored) -> {
                            // User cancelled the dialog
                        });
                // Create the AlertDialog object and return it
                builder.create().show();
            } else if (id == R.id.wt_disable) {
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.disable_wt_ok)
                        .setMessage(R.string.disable_wt_msg)
                        .setPositiveButton(R.string.disable_wt_ok, (dialog, ignored) -> {
                            ITagApplication.faWtRemove();
                            // TODO: why don not stop it?
                            // iTagsService.stopWayToday();
                            WayTodayDisabled0Preference.set(this, true);
                            final View v = findViewById(R.id.btn_waytoday);
                            if (v != null) {
                                v.setVisibility(View.GONE);
                            }
                        })
                        .setNegativeButton(R.string.about_cancel, (dialog, ignored) -> {
                            // User cancelled the dialog
                        });
                // Create the AlertDialog object and return it
                builder.create().show();
            }
            if (id == R.id.wt_sec_1 || id == R.id.wt_min_5 || id == R.id.wt_hour_1) {
                if (GPSPermissionManager.needPermissionRequest(this)) {
                    WayToday.getInstance().enableTrackingOn();
                    GPSPermissionManager.requestPermissions(this);
                } else {
                    WayToday.getInstance().turnTrackingOn();
                }
            }
            return true;
        });
        popupMenu.show();
    }

    public void onChangeColor(@NonNull View sender) {
        ITagInterface itag = (ITagInterface) sender.getTag();
        if (itag == null) {
            ITagApplication.handleError(new Exception("No itag"));
            return;
        }
        final PopupMenu popupMenu = new PopupMenu(this, sender);
        popupMenu.inflate(R.menu.color);
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.black) {
                ITag.store.setColor(itag.id(), TagColor.black);
            } else if (id == R.id.white) {
                ITag.store.setColor(itag.id(), TagColor.white);
            } else if (id == R.id.red) {
                ITag.store.setColor(itag.id(), TagColor.red);
            } else if (id == R.id.green) {
                ITag.store.setColor(itag.id(), TagColor.green);
            } else if (id == R.id.gold) {
                ITag.store.setColor(itag.id(), TagColor.gold);
            } else if (id == R.id.blue) {
                ITag.store.setColor(itag.id(), TagColor.blue);
            }
            ITagApplication.faColorITag();
            return true;
        });
        popupMenu.show();
    }

    public void onDisconnectAlert(@NonNull View sender) {
        ITagInterface itag = (ITagInterface) sender.getTag();
        if (itag == null) {
            ITagApplication.handleError(new Exception("No itag"));
            return;
        }
        BLEConnectionInterface connection = ITag.ble.connectionById(itag.id());
        if (itag.isAlertDisconnected()) {
            ITag.store.setAlert(itag.id(), false);
            new Thread(connection::disconnect).start();
        } else {
            if (connection.isConnected()) {
                new Thread(connection::disconnect).start();
            } else {
                ITag.store.setAlert(itag.id(), true);
                ITag.connectAsync(connection);
            }
        }
        if (itag.isAlertDisconnected()) {
            Toast.makeText(this, R.string.mode_alertdisconnect, Toast.LENGTH_SHORT).show();
            ITagApplication.faUnmuteTag();
            if (GPSPermissionManager.needPermissionRequest(this)) {
                GPSPermissionManager.requestPermissions(this, getString(R.string.gps_permission_request));
            }
        } else {
            Toast.makeText(this, R.string.mode_keyfinder, Toast.LENGTH_SHORT).show();
            ITagApplication.faMuteTag();
        }
    }

    public void onSetName(@NonNull View sender) {
        ITagInterface itag = (ITagInterface) sender.getTag();
        if (itag == null) {
            ITagApplication.handleError(new Exception("No itag"));
            return;
        }

        SetNameDialogFragment.iTag = itag;
        new SetNameDialogFragment().show(getSupportFragmentManager(), "setname");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            //noinspection SwitchStatementWithTooFewBranches
            switch (requestCode) {
                case REQUEST_ENABLE_BT:
                    setupContent();
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case REQUEST_ENABLE_BT:
                    setupContent();
                    break;
                case REQUEST_ONSCAN:
                    onStartStopScan(null);
                    break;
            }
        }
        GPSPermissionManager
                .handleOnRequestPermissionsResult(
                        requestCode,
                        WayToday.getInstance().gpsUpdatesManager,
                        WayToday.getInstance().isTrackingOn());
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void onOpenBTSettings(View ignored) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        try {
            startActivity(enableBtIntent);
        } catch (SecurityException e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }
}
