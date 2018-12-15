package solutions.s4y.itag;

import android.Manifest;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;

import org.junit.Before;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import solutions.s4y.itag.ble.ITagsService;

import static org.robolectric.Shadows.shadowOf;

public class RobolectricActivityInt {
    protected MainActivity mainActivity;

    @Before
    public void setupActivity() {
        Application application = RuntimeEnvironment.application;
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

}
