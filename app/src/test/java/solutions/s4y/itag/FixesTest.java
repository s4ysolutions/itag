package solutions.s4y.itag;

import android.Manifest;
import android.app.Application;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Assert;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import androidx.test.core.app.ApplicationProvider;
import solutions.s4y.itag.ble.ITagsService;
import solutions.s4y.itag.ble.LeScanner;

import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class FixesTest {
    private MainActivity mainActivity;

    @Before
    public void setupActivity() {
        Application application = ApplicationProvider.getApplicationContext();
        ShadowApplication shadowApplication = shadowOf(application);
        shadowApplication.grantPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
        Intent intent = new Intent(application, ITagsService.class);
        ITagsService iTagsService = new ITagsService();
        shadowApplication.setComponentNameAndServiceForBindServiceForIntent(
                intent,
                new ComponentName(ITagsService.class.getPackage().getName(),ITagsService.class.getName()),
                iTagsService.onBind(null)
        );
        mainActivity = Robolectric.setupActivity(MainActivity.class);
    }

    @AfterClass
    public static void stopScanning() {
        LeScanner.isScanning = false;
    }

    @Test
    public void issue38_setupLeScanFragment() {
        mainActivity.onStartStopScan(null);
        final FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.content);
        Assert.assertTrue(fragment instanceof LeScanFragment);
    }
}
