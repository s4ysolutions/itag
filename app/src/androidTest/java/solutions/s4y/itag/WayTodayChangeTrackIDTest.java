package solutions.s4y.itag;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import solutions.s4y.waytoday.idservice.IDService;
import solutions.s4y.waytoday.locations.LocationsTracker;
import solutions.s4y.waytoday.upload.UploadJobService;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class WayTodayChangeTrackIDTest {
    private static IDService.IIDSeriviceListener mockIDListener =
            mock(IDService.IIDSeriviceListener.class);
    private static UploadJobService.IUploadListener mockUploadListener =
            mock(UploadJobService.IUploadListener.class);
    private static LocationsTracker.ITrackingStateListener mockTrackingStateListener
            = mock(LocationsTracker.ITrackingStateListener.class);
    private static LocationsTracker.ILocationListener mockLocationListener
            = mock(LocationsTracker.ILocationListener.class);

    @BeforeClass
    public static void beforeClass() {
        IDService.addOnTrackIDChangeListener(mockIDListener);
        UploadJobService.addOnITagChangeListener(mockUploadListener);
        LocationsTracker.addOnTrackingStateListener(mockTrackingStateListener);
        LocationsTracker.addOnLocationListener(mockLocationListener);
    }

    @AfterClass
    public static void afterClass() {
        IDService.removeOnTrackIDChangeListener(mockIDListener);
        UploadJobService.removeOnITagChangeListener(mockUploadListener);
        LocationsTracker.removeOnTrackingStateListener(mockTrackingStateListener);
        LocationsTracker.removeOnLocationListener(mockLocationListener);
    }

    @Before
    public void before() {
        Application application = ApplicationProvider.getApplicationContext();
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(application);
        p.edit().remove("freq").apply();
        p.edit().remove("tid").apply();
        p.edit().remove("wt").apply();
        reset(mockIDListener);
        reset(mockUploadListener);
        reset(mockTrackingStateListener);
        reset(mockLocationListener);
    }

    @After
    public void after() {
    }

    @Test
    public void activity_shouldHaveWayTodayMenu() {

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {

            ViewInteraction vi = onView(withId(R.id.btn_waytoday));
            vi.perform(click());
            onView(withText(R.string.off))
                    .perform(click());
        }
    }

    @Test
    public void activity_clickChangeID() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {

            Application application = ApplicationProvider.getApplicationContext();
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(application);

            String prevID = sp.getString("tid", "");

            // open waytoday menu
            onView(withId(R.id.btn_waytoday))
                    .perform(click());
            //close waytoday menu
            onView(withText(R.string.wt_new_id))
                    .perform(click());

            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(application);

            verify(mockIDListener, timeout(10000)).onTrackID(any());
            String id = sp.getString("tid", "xxxx");
            assertThat(id, not(prevID));
            assertThat(id, not("xxxx"));
        }
    }
}
