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

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import s4y.waytoday.idservice.IDService;
import s4y.waytoday.locations.LocationsTracker;
import s4y.waytoday.upload.UploadJobService;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class WayTodayUITest {
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
        reset(mockIDListener);
        reset(mockUploadListener);
        reset(mockTrackingStateListener);
        reset(mockLocationListener);
    }

    @After
    public void after() {
    }

    @Test
    public void wtViewVisibilityWithItag0_shouldReflectTrackingState() {
        Application application = ApplicationProvider.getApplicationContext();
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(application);
        p.edit().putInt("freq", 1).apply();
        p.edit().putString("tid", "xxxx").apply();
        p.edit().putBoolean("wt", true).apply();
        p.edit().putBoolean("wtfirst", false).apply();

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            ArgumentCaptor<LocationsTracker.TrackingState> state =
                    ArgumentCaptor.forClass(LocationsTracker.TrackingState.class);

            verify(mockTrackingStateListener, timeout(1000).atLeastOnce())
                    .onStateChange(state.capture());
            assertThat(LocationsTracker.isUpdating, is(true));
        }
    }
}
