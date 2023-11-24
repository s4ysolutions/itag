package s4y.itag.ble;

import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.Arrays;

public class RSSIFilter {
    private KalmanFilter filter;

    public RSSIFilter() {
        reset();
    }

    public void reset() {
        // A = [ 1 ] constant rssi
        RealMatrix A = new Array2DRowRealMatrix(new double[]{1d});
// no control input
        RealMatrix B = null;
// H = [ 1 ]
        RealMatrix H = new Array2DRowRealMatrix(new double[]{1d});
// Q = [ 0 ] process noise
        // 1 indicator level ~ 1 dBm
        RealMatrix Q = new Array2DRowRealMatrix(new double[]{3 * 3});
// R = [ 0 ] measurement noise
        // RSSI varies from -65 to -115 dBm = 50 dBm
        // assume the measurement is within that range
        RealMatrix R = new Array2DRowRealMatrix(new double[]{25 * 25});
        // initial state -85 dBm - empirical ~1-2m
        ProcessModel pm
                = new DefaultProcessModel(A, B, Q, new ArrayRealVector(new double[]{85}), null);
        MeasurementModel mm = new DefaultMeasurementModel(H, R);

        filter = new KalmanFilter(pm, mm);
    }

    public void add(int rawmeasurement) {
        filter.predict();
        filter.correct(new double[]{-rawmeasurement});
    }

    public int get() {
        return - (int) filter.getStateEstimation()[0];
    }
    /*
    final int[] measurements = {0, 0, 0, 0, 0, 0, 0};
    final int[] deviations = {0, 0, 0, 0, 0, 0, 0};
    final int[] spikes = {0, 0, 0};
    int spikecount = 0;
    int current = 0;
    int length = 0;

    int average = 0;
    int deviation = 15;

    public void reset() {
        current = 0;
        length = 0;
        deviation = 15;
        Arrays.fill(measurements, 0);
        Arrays.fill(deviations, 0);
    }

    public void add(int rawmeasurement) {
        int measurement;
        if (average != 0 && length > 3 && Math.abs(rawmeasurement - average) > deviation) {
            spikes[spikecount++] = rawmeasurement;
            if (spikecount < spikes.length) return;
            measurement = (spikes[0] + spikes[1] + spikes[2]) / 3;
        } else {
            measurement = rawmeasurement;
        }
        spikecount = 0;
        measurements[current++] = measurement;
        if (current == measurements.length) {
            current = 0;
        }
        if (length < measurements.length) {
            length++;
        }
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += measurements[i];
        }
        average = (int) ((double) sum / length + 0.5);
        deviations[current] = Math.abs(average - rawmeasurement);

        int sumdeviations = 0;
        for (int i = 0; i < length; i++) {
            sumdeviations += deviations[i];
        }
        deviation = (int) ((double) sumdeviations / length + 0.5);
    }

    public int get() {
        return average;
    }
     */
}
