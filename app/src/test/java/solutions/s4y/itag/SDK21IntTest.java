package solutions.s4y.itag;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.test.core.app.ApplicationProvider;
import solutions.s4y.itag.ble.ITagsService;

import android.Manifest;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=Build.VERSION_CODES.LOLLIPOP)
public class SDK21IntTest {
    private MainActivity mainActivity;

    @Before
    public void setupActivity() {
        Application application = ApplicationProvider.getApplicationContext();
        ShadowApplication shadowApplication = shadowOf(application);
        shadowApplication.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        Intent intent = new Intent(application, ITagsService.class);
        ITagsService iTagsService = new ITagsService();
        shadowApplication.setComponentNameAndServiceForBindServiceForIntent(
                intent,
                new ComponentName(ITagApplication.class.getPackage().getName(),ITagsService.class.getName()),
                iTagsService.onBind(null)
        );
        mainActivity = Robolectric.setupActivity(MainActivity.class);
    }

    @Test
    public void reproduceResourceNotFound() {
        final FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.content);
        Assert.assertTrue(fragment instanceof ITagsFragment);
    }
}
