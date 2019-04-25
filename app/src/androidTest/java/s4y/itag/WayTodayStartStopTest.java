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
import s4y.waytoday.idservice.IDService;
import s4y.waytoday.locations.LocationsTracker;
import s4y.waytoday.upload.UploadJobService;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class WayTodayStartStopTest {
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
        p.edit().putString("tid", "0000").apply();
        p.edit().remove("wt").apply();
        reset(mockIDListener);
        reset(mockUploadListener);
        reset(mockTrackingStateListener);
        LocationsTracker.isUpdating = false;
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
    public void activity_shouldStartWayTodayContiniously() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            Application application = ApplicationProvider.getApplicationContext();
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(application);

            assertThat(LocationsTracker.isUpdating, is(false));

            ViewInteraction vi = onView(withId(R.id.btn_waytoday));
            vi.perform(click());
            onView(withText(R.string.freq_continuously))
                    .perform(click());
            ArgumentCaptor<LocationsTracker.TrackingState> state =
                    ArgumentCaptor.forClass(LocationsTracker.TrackingState.class);

            verify(mockTrackingStateListener, timeout(5000).atLeast(1)).onStateChange(state.capture());
            List<LocationsTracker.TrackingState> states =
                    state.getAllValues();
            assertThat(states.size(), greaterThan(0));
            assertThat(states.get(0).isUpdating, is(true));
            assertThat(LocationsTracker.isUpdating, is(true));
            assertThat(p.getInt("freq", -1), equalTo(1));
        }
    }

    @Test
    public void activity_shouldStartWayToday5min() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            Application application = ApplicationProvider.getApplicationContext();
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(application);

            assertThat(LocationsTracker.isUpdating, is(false));

            ViewInteraction vi = onView(withId(R.id.btn_waytoday));
            vi.perform(click());
            onView(withText(R.string.freq_min_5))
                    .perform(click());
            ArgumentCaptor<LocationsTracker.TrackingState> state =
                    ArgumentCaptor.forClass(LocationsTracker.TrackingState.class);

            verify(mockTrackingStateListener, timeout(5000).atLeast(1)).onStateChange(state.capture());
            List<LocationsTracker.TrackingState> states =
                    state.getAllValues();
            assertThat(states.size(), greaterThan(0));
            assertThat(states.get(0).isUpdating, is(true));
            assertThat(LocationsTracker.isUpdating, is(true));
            assertThat(p.getInt("freq", -1), equalTo(5 * 60 * 1000));
            assertThat(p.getBoolean("wt", false), is(true));
        }
    }

    @Test
    public void activity_shouldStartWayToday1h() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            Application application = ApplicationProvider.getApplicationContext();
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(application);

            assertThat(LocationsTracker.isUpdating, is(false));

            ViewInteraction vi = onView(withId(R.id.btn_waytoday));
            vi.perform(click());
            onView(withText(R.string.freq_hour_1))
                    .perform(click());
            ArgumentCaptor<LocationsTracker.TrackingState> state =
                    ArgumentCaptor.forClass(LocationsTracker.TrackingState.class);

            verify(mockTrackingStateListener, timeout(5000).atLeast(1)).onStateChange(state.capture());
            List<LocationsTracker.TrackingState> states =
                    state.getAllValues();
            assertThat(states.size(), greaterThan(0));
            assertThat(states.get(0).isUpdating, is(true));
            assertThat(LocationsTracker.isUpdating, is(true));
            assertThat(p.getInt("freq", -1), equalTo(60 * 60 * 1000));
            assertThat(p.getBoolean("wt", false), is(true));

        }
    }


    @Test
    public void activity_shouldStopWayToday() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            Application application = ApplicationProvider.getApplicationContext();
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(application);

            assertThat(LocationsTracker.isUpdating, is(false));

            ViewInteraction vi = onView(withId(R.id.btn_waytoday));
            vi.perform(click());
            onView(withText(R.string.freq_hour_1))
                    .perform(click());
            ArgumentCaptor<LocationsTracker.TrackingState> state =
                    ArgumentCaptor.forClass(LocationsTracker.TrackingState.class);

            verify(mockTrackingStateListener, timeout(5000).atLeast(1)).onStateChange(state.capture());
            List<LocationsTracker.TrackingState> states =
                    state.getAllValues();
            assertThat(states.size(), greaterThan(0));
            assertThat(states.get(0).isUpdating, is(true));
            assertThat(LocationsTracker.isUpdating, is(true));
            assertThat(p.getInt("freq", -1), equalTo(60 * 60 * 1000));
            assertThat(p.getBoolean("wt", false), is(true));

            reset(mockTrackingStateListener);

            vi.perform(click());
            onView(withText(R.string.off))
                    .perform(click());
            verify(mockTrackingStateListener, timeout(5000).atLeast(1)).onStateChange(state.capture());
            states =
                    state.getAllValues();
            assertThat(states.size(), greaterThan(0));
            assertThat(LocationsTracker.isUpdating, is(false));
            assertThat(p.getInt("freq", -1), equalTo(60 * 60 * 1000));
            assertThat(p.getBoolean("wt", true), is(false));

        }
    }
}
