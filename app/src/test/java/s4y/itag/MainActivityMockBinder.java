package s4y.itag;

import android.os.IBinder;

import androidx.annotation.NonNull;
import s4y.itag.ble.ITagsService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MainActivityMockBinder extends MainActivity {
    static ITagsService.GattBinder mockedBinder =
            mock(ITagsService.GattBinder.class);
    static ITagsService mockedBackgroundService =
            mock(ITagsService.class);

    static {
        when(mockedBinder.getService()).thenReturn(mockedBackgroundService);
    }

    @Override
    protected void setBinder(@NonNull IBinder binder) {
        super.setBinder(mockedBinder);
    }
}
