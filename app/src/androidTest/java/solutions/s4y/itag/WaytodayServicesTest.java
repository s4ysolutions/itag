package solutions.s4y.itag;

import android.location.Location;
import android.location.LocationManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.List;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import solutions.s4y.waytoday.idservice.IDService;
import solutions.s4y.waytoday.upload.UploadJobService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WaytodayServicesTest {
    static IDService.IIDSeriviceListener mockIDListiner = mock(IDService.IIDSeriviceListener.class);
    static UploadJobService.IUploadListener mockUploadListiner = mock(UploadJobService.IUploadListener.class);

    @BeforeClass
    public static void beforeClass() {
        IDService.addOnTrackIDChangeListener(mockIDListiner);
        UploadJobService.addOnITagChangeListener(mockUploadListiner);
    }

    @AfterClass
    public static void afterClass() {
        IDService.removeOnTrackIDChangeListener(mockIDListiner);
        UploadJobService.removeOnITagChangeListener(mockUploadListiner);
    }

    @Before
    public void before() {
        reset(mockIDListiner);
        reset(mockUploadListiner);
    }

    @After
    public void after() {
    }


    @Test
    public void idService_shouldReturnTrackID() {
        assertThat(IDService.sListeners.size(), equalTo(1));
        verify(mockIDListiner, never()).onTrackID(any());

        IDService.enqueueRetrieveId(ApplicationProvider.getApplicationContext(), "");
        verify(mockIDListiner, timeout(5000).times(1)).onTrackID(any());
    }

    @Test
    public void uplaodService_shouldUploadLocation() {
        ArgumentCaptor<UploadJobService.Status> argument =
                ArgumentCaptor.forClass(UploadJobService.Status.class);
        Location location = new Location(LocationManager.GPS_PROVIDER);
        UploadJobService.enqueueUploadLocation(
                ApplicationProvider.getApplicationContext(),
                location
        );
        verify(mockUploadListiner, timeout(5000).times(3)).onStatusChange(argument.capture());
        List<UploadJobService.Status> statuses = argument.getAllValues();
        assertThat(statuses.size(), equalTo(3));
        assertThat(statuses.get(0), equalTo(UploadJobService.Status.QUEUED));
        assertThat(statuses.get(1), equalTo(UploadJobService.Status.UPLOADING));
        assertThat(statuses.get(2), equalTo(UploadJobService.Status.EMPTY));
    }

}
