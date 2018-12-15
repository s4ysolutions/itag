package solutions.s4y.itag;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Build;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=Build.VERSION_CODES.LOLLIPOP)
public class SDK21IntTest extends RobolectricActivityInt{
    @Test
    public void reproduceResourceNotFound() {
        final FragmentManager fragmentManager = mainActivity.getFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.content);
        Assert.assertTrue(fragment instanceof ITagsFragment);
    }
}
