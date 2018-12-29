package solutions.s4y.itag;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
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
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContextImpl;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.test.core.app.ApplicationProvider;
import solutions.s4y.itag.ble.ITagsService;

import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class FragmentsSelectTest {
    private BluetoothManager mockBluetoothManager;
    private BluetoothAdapter mockBluetoothAdapter;

    @Before
    public void setupActivity() {
        Application application = ApplicationProvider.getApplicationContext();
        ShadowApplication shadowApplication = shadowOf(application);
        shadowApplication.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        Intent intent = new Intent(application, ITagsService.class);
        ITagsService iTagsService = new ITagsService();
        shadowApplication.setComponentNameAndServiceForBindServiceForIntent(
                intent,
                new ComponentName(ITagApplication.class.getPackage().getName(),ITagsService.class.getName()),
                iTagsService.onBind(null)
        );
        mockBluetoothManager = Mockito.mock(BluetoothManager.class);
        ShadowContextImpl shadowContext = Shadow.extract(application.getBaseContext());
        shadowContext.setSystemService(Context.BLUETOOTH_SERVICE, mockBluetoothManager);
    }

    @Test
    public void noBle_shouldCreateNoBLEFragment() {
        when(mockBluetoothManager.getAdapter()).thenReturn(null);
        MainActivity mainActivity = Robolectric.setupActivity(MainActivity.class);
        final FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.content);
        Assert.assertTrue(fragment instanceof NoBLEFragment);
    }

    @Test
    public void bleEnabled_shouldReturnITagsFragment() {
        mockBluetoothAdapter =  Mockito.mock(BluetoothAdapter.class);
        when(mockBluetoothAdapter.isEnabled()).thenReturn(true);
        when(mockBluetoothManager.getAdapter()).thenReturn(mockBluetoothAdapter);
        MainActivity mainActivity = Robolectric.setupActivity(MainActivity.class);
        final FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.content);
        Assert.assertTrue(fragment instanceof ITagsFragment);
    }

    @Test
    public void bleDisabled_shouldReturnDisabledBLEFragment() {
        mockBluetoothAdapter =  Mockito.mock(BluetoothAdapter.class);
        when(mockBluetoothAdapter.isEnabled()).thenReturn(false);
        when(mockBluetoothManager.getAdapter()).thenReturn(mockBluetoothAdapter);
        MainActivity mainActivity = Robolectric.setupActivity(MainActivity.class);
        final FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.content);
        Assert.assertTrue(fragment instanceof DisabledBLEFragment);
    }

    @Test
    public void bleTurnedOn_shouldReturnITagsFragment() {
        mockBluetoothAdapter =  Mockito.mock(BluetoothAdapter.class);
        when(mockBluetoothAdapter.isEnabled())
                .thenReturn(false) //disabled
                .thenReturn(true); // turned on
        when(mockBluetoothManager.getAdapter()).thenReturn(mockBluetoothAdapter);
        MainActivity mainActivity = Robolectric.setupActivity(MainActivity.class);
        final FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.content);
        Assert.assertTrue(fragment instanceof ITagsFragment);
    }

    @Test
    public void bleTurnedOn2times_shouldReturnITagsFragment() {
        mockBluetoothAdapter =  Mockito.mock(BluetoothAdapter.class);
        when(mockBluetoothAdapter.isEnabled())
                .thenReturn(false) // disabled
                .thenReturn(false) // turned on
                .thenReturn(true); // enabled
        when(mockBluetoothManager.getAdapter()).thenReturn(mockBluetoothAdapter);
        MainActivity mainActivity = Robolectric.setupActivity(MainActivity.class);
        final FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.content);
        Assert.assertTrue(fragment instanceof ITagsFragment);
    }

    @Test
    public void bleTurnedOn3times_shouldReturnITagsFragment() {
        mockBluetoothAdapter =  Mockito.mock(BluetoothAdapter.class);
        when(mockBluetoothAdapter.isEnabled())
                .thenReturn(false) // disabled
                .thenReturn(false) // turned on 1
                .thenReturn(false) // turned on 2
                .thenReturn(true); // enabled
        when(mockBluetoothManager.getAdapter()).thenReturn(mockBluetoothAdapter);
        MainActivity mainActivity = Robolectric.setupActivity(MainActivity.class);
        final FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.content);
        Assert.assertTrue(fragment instanceof ITagsFragment);
    }

    @Test
    public void bleNotTurnedOn3times_shouldReturnDisabledBLEFragment() {
        mockBluetoothAdapter =  Mockito.mock(BluetoothAdapter.class);
        when(mockBluetoothAdapter.isEnabled())
                .thenReturn(false) // disabled
                .thenReturn(false) // turned on 1
                .thenReturn(false) // turned on 2
                .thenReturn(false) // turned on 3
                .thenReturn(false);// failed to turn on
        when(mockBluetoothManager.getAdapter()).thenReturn(mockBluetoothAdapter);
        MainActivity mainActivity = Robolectric.setupActivity(MainActivity.class);
        final FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.content);
        Assert.assertTrue(fragment instanceof DisabledBLEFragment);
    }

}
