package s4y.waytoday.idservice;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class IDServiceListenersTest {
    @Test
    public void idService_canAddAndRemoveListener() {
        assertThat(IDService.sListeners.size()).isEqualTo(0);
        IDService.IIDSeriviceListener l = trackID -> {

        };

        IDService.addOnTrackIDChangeListener(l);
        assertThat(IDService.sListeners.size()).isEqualTo(1);
        IDService.removeOnTrackIDChangeListener(l);
        assertThat(IDService.sListeners.size()).isEqualTo(0);

    }

    @Test
    public void idService_canAddAndRemoveListeneres() {
        assertThat(IDService.sListeners.size()).isEqualTo(0);

        IDService.IIDSeriviceListener l1 = trackID -> {

        };
        IDService.IIDSeriviceListener l2 = trackID -> {

        };

        IDService.addOnTrackIDChangeListener(l1);
        assertThat(IDService.sListeners.size()).isEqualTo(1);
        IDService.addOnTrackIDChangeListener(l2);
        assertThat(IDService.sListeners.size()).isEqualTo(2);
        IDService.removeOnTrackIDChangeListener(l1);
        assertThat(IDService.sListeners.size()).isEqualTo(1);
        IDService.removeOnTrackIDChangeListener(l2);
        assertThat(IDService.sListeners.size()).isEqualTo(0);
    }

}
