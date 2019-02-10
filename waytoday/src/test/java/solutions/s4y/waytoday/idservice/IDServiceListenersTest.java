package solutions.s4y.waytoday.idservice;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class IDServiceListenersTest {
    @Test
    public void idService_canAddAndRemoveListener() {
        assertThat(IDService.sListeners.size()).isEqualTo(0);
        IDService.IIDSeriviceListener l = trackID -> {

        };

        IDService.addOnITagChangeListener(l);
        assertThat(IDService.sListeners.size()).isEqualTo(1);
        IDService.removeOnITagChangeListener(l);
        assertThat(IDService.sListeners.size()).isEqualTo(0);

    }

    @Test
    public void idService_canAddAndRemoveListeneres() {
        assertThat(IDService.sListeners.size()).isEqualTo(0);

        IDService.IIDSeriviceListener l1 = trackID -> {

        };
        IDService.IIDSeriviceListener l2 = trackID -> {

        };

        IDService.addOnITagChangeListener(l1);
        assertThat(IDService.sListeners.size()).isEqualTo(1);
        IDService.addOnITagChangeListener(l2);
        assertThat(IDService.sListeners.size()).isEqualTo(2);
        IDService.removeOnITagChangeListener(l1);
        assertThat(IDService.sListeners.size()).isEqualTo(1);
        IDService.removeOnITagChangeListener(l2);
        assertThat(IDService.sListeners.size()).isEqualTo(0);
    }

}
