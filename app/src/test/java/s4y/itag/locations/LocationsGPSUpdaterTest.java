package s4y.itag.locations;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLocationManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import s4y.waytoday.errors.ErrorsObservable;
import s4y.waytoday.locations.LocationsGPSUpdater;
import s4y.waytoday.locations.RequestUpdatesListener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"unchecked", "WeakerAccess"})
@RunWith(AndroidJUnit4.class)
public class LocationsGPSUpdaterTest {
    LocationManager mockLocationManager = mock(LocationManager.class);
    LocationListener mockLocationListener = mock(LocationListener.class);
    RequestUpdatesListener mockRequestUpdatesListener = mock(RequestUpdatesListener.class);
    ErrorsObservable.IErrorListener mockErrorListener = mock(ErrorsObservable.IErrorListener.class);
    LocationsGPSUpdater locationsGPSUpdater;

    @Before
    public void setUp() {
        reset(mockLocationManager);
        reset(mockLocationListener);
        reset(mockRequestUpdatesListener);
        reset(mockErrorListener);
        ErrorsObservable.addOnITagChangeListener(mockErrorListener);

        Application context = ApplicationProvider.getApplicationContext();
        locationsGPSUpdater = new LocationsGPSUpdater(context);
    }

    @After
    public void tearDown() {
        ErrorsObservable.removeOnITagChangeListener(mockErrorListener);
    }

    @Test
    public void locationsGPSUpdater_shouldNotEmitPermissionRequest() {
        Application context = ApplicationProvider.getApplicationContext();
        LocationsGPSUpdater locationsGPSUpdater = new LocationsGPSUpdater(context);
        locationsGPSUpdater.requestLocationUpdates(mockLocationListener, mockRequestUpdatesListener, 1000);

        verify(mockRequestUpdatesListener).onRequestResult(true);
        verify(mockErrorListener, never()).onError(any());
        verify(mockLocationListener, never()).onLocationChanged(any());
    }

    @Test
    public void locationsGPSUpdater_shouldEmitNotificationsOnLocationUpdates() {
        Application context = ApplicationProvider.getApplicationContext();
        LocationsGPSUpdater locationsGPSUpdater = new LocationsGPSUpdater(context);
        LocationManager manager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        ShadowLocationManager shadowLocationManager = Shadows.shadowOf(manager);


        locationsGPSUpdater.requestLocationUpdates(mockLocationListener, mockRequestUpdatesListener, 1000);
        verify(mockLocationListener, never()).onLocationChanged(any());

        Location location = new Location(LocationManager.GPS_PROVIDER);
        shadowLocationManager.simulateLocation(location);
        verify(mockLocationListener).onLocationChanged(any());
    }
}
