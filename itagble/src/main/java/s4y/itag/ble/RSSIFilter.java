package s4y.itag.ble;

import java.util.Arrays;

public class RSSIFilter {
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
}
