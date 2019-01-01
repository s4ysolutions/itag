package solutions.s4y.itag;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;

import androidx.test.core.app.ApplicationProvider;
import solutions.s4y.itag.ble.ITagDevice;
import solutions.s4y.itag.ble.ITagsDb;
import solutions.s4y.itag.ble.ITagsService;

import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class TagsDbTest {
    Context context;

    @Before
    public void setupActivity() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void getDevices_ShouldReturnEmptyList() {
        List<ITagDevice> devices = ITagsDb.getDevices(context);
        Assert.assertEquals(0, devices.size());
    }

    @Test
    public void remember_ShouldCreateListOfOne() {
        // Dummy call to make sure devices are loaded
        ITagsDb.getDevices(context);

        BluetoothDevice bd1 = Mockito.mock(BluetoothDevice.class);
        when(bd1.getAddress()).thenReturn("1");
        ITagsDb.remember(context, bd1);
        List<ITagDevice> devices = ITagsDb.getDevices(context);
        Assert.assertEquals(1, devices.size());

        ITagsDb.forget(context, devices.get(0));
        devices = ITagsDb.getDevices(context);
        Assert.assertEquals(0, devices.size());
    }


    @Test
    public void save_ShouldUpdateLinked() {
        // Dummy call to make sure devices are loaded
        ITagsDb.getDevices(context);

        BluetoothDevice bd1 = Mockito.mock(BluetoothDevice.class);
        when(bd1.getAddress()).thenReturn("1");
        ITagsDb.remember(context, bd1);
        List<ITagDevice> devices = ITagsDb.getDevices(context);
        Assert.assertEquals(1, devices.size());

        devices.get(0).linked=true;
        ITagsDb.save(context);

        devices = ITagsDb.getDevices(context);
        Assert.assertEquals(1, devices.size());
        Assert.assertTrue(devices.get(0).linked);
    }
}
