package s4y.itag.locations;

import android.app.Application;
import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import s4y.itag.MainActivityMockBinder;
import s4y.waytoday.errors.ErrorsObservable;
import s4y.waytoday.locations.LocationsGPSUpdater;
import s4y.waytoday.locations.RequestUpdatesListener;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"unchecked", "WeakerAccess"})
@RunWith(AndroidJUnit4.class)
public class LocationsGPSUpdaterNoPermissionTest {
    @Rule
    public ActivityTestRule<MainActivityMockBinder> mActivityRule =
            new ActivityTestRule(MainActivityMockBinder.class);

    LocationManager mockLocationManager = mock(LocationManager.class);
    LocationListener mockLocationListener = mock(LocationListener.class);
    RequestUpdatesListener mockRequestUpdatesListener = mock(RequestUpdatesListener.class);
    ErrorsObservable.IErrorListener mockErrorListener = mock(ErrorsObservable.IErrorListener.class);

    @Before
    public void setUp() {
        reset(mockLocationManager);
        reset(mockLocationListener);
        reset(mockRequestUpdatesListener);
        reset(mockErrorListener);
        ErrorsObservable.addOnITagChangeListener(mockErrorListener);
    }

    @After
    public void tearDown() {
        ErrorsObservable.removeOnITagChangeListener(mockErrorListener);
    }

    @Test
    public void activity_shouldNotHaveFineLocationPermission() {
        int result = mActivityRule.getActivity()
                .checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);
        assertThat(result).isEqualTo(PERMISSION_DENIED);
    }

    @Test
    public void shadowContext_shouldProvideMockedSystemService() {
        Application context = ApplicationProvider.getApplicationContext();
        ShadowApplication shadowContext = Shadows.shadowOf(context);
        shadowContext.setSystemService(Context.LOCATION_SERVICE, mockLocationManager);

        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        assertThat(manager).isEqualTo(mockLocationManager);
    }

    @Test
    public void locationsGPSUpdater_shouldEmitPermissionRequestAndonRequestResultFalse() {
        Application context = ApplicationProvider.getApplicationContext();
        ShadowApplication shadowContext = Shadows.shadowOf(context);
        shadowContext.setSystemService(Context.LOCATION_SERVICE, mockLocationManager);
        LocationsGPSUpdater locationsGPSUpdater = new LocationsGPSUpdater(context);

        doThrow(SecurityException.class)
                .when(mockLocationManager)
                .requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000,
                        1,
                        mockLocationListener);

        locationsGPSUpdater.requestLocationUpdates(mockLocationListener, mockRequestUpdatesListener, 1000);

        verify(mockRequestUpdatesListener, times(1)).onRequestResult(false);
        verify(mockErrorListener, times(1)).onError(any());
        verify(mockLocationListener, never()).onLocationChanged(any());
    }
}
