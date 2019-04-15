package s4y.itag;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import solutions.s4y.waytoday.idservice.IDService;
import solutions.s4y.waytoday.locations.LocationsTracker;
import solutions.s4y.waytoday.upload.UploadJobService;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class WayTodayFirstSessionTest {
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
        p.edit().remove("wtfirst").apply();
        reset(mockIDListener);
        reset(mockUploadListener);
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
    public void activity_firstLaunchWT() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            // open waytoday menu
            onView(withId(R.id.btn_waytoday))
                    .perform(click());
            onView(withText(R.string.wt_new_id))
                    .check(doesNotExist());
            //close waytoday menu
            onView(withText(R.string.freq_continuously))
                    .perform(click());

            Application application = ApplicationProvider.getApplicationContext();
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(application);
            assertThat(p.getInt("freq", -1), equalTo(1));
            assertThat(p.getBoolean("wt", false), is(true));
            assertThat(p.getBoolean("wtfirst", true), is(true));
            assertThat(p.getBoolean("wtfirst", false), is(false));

            ArgumentCaptor<LocationsTracker.TrackingState> state =
                    ArgumentCaptor.forClass(LocationsTracker.TrackingState.class);

            verify(mockIDListener, timeout(10000)).onTrackID(any());
            verify(mockTrackingStateListener, timeout(1000).times(2)).onStateChange(state.capture());
            List<LocationsTracker.TrackingState> states =
                    state.getAllValues();
            assertThat(states.size(), is(2));
            assertThat(states.get(0).isSuspended, is(false));
            assertThat(states.get(0).isUpdating, is(true));
            assertThat(states.get(1).isSuspended, is(anything()));
            assertThat(states.get(1).isUpdating, is(true));
            assertThat(p.getBoolean("wtfirst", true), is(false));

            // open waytoday menu
            onView(withId(R.id.btn_waytoday))
                    .perform(click());
            onView(withText(R.string.wt_new_id));
            //close waytoday menu
            onView(withId(R.id.btn_waytoday))
                    .perform(click());
        }
    }
}
