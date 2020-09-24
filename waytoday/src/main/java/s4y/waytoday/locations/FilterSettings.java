package s4y.waytoday.locations;

import mad.location.manager.lib.Commons.Utils;

public class FilterSettings {
    public static FilterSettings defaultSettings =
            new FilterSettings(Utils.ACCELEROMETER_DEFAULT_DEVIATION,
                    Utils.GPS_MIN_DISTANCE, Utils.GPS_MIN_TIME,
                    Utils.GEOHASH_DEFAULT_PREC, Utils.GEOHASH_DEFAULT_MIN_POINT_COUNT,
                    Utils.SENSOR_DEFAULT_FREQ_HZ,
                    true, Utils.DEFAULT_VEL_FACTOR, Utils.DEFAULT_POS_FACTOR);
    public final double accelerationDeviation;
    public final int geoHashPrecision;
    public final int geoHashMinPointCount;
    public final double mVelFactor;
    public final double mPosFactor;
    private final int gpsMinDistance;
    private final int gpsMinTime;
    private final double sensorFfequencyHz;
    private final boolean filterMockGpsCoordinates;

    public FilterSettings(double accelerationDeviation,
                          int gpsMinDistance,
                          int gpsMinTime,
                          int geoHashPrecision,
                          int geoHashMinPointCount,
                          double sensorFfequencyHz,
                          boolean filterMockGpsCoordinates,
                          double velFactor,
                          double posFactor) {
        this.accelerationDeviation = accelerationDeviation;
        this.gpsMinDistance = gpsMinDistance;
        this.gpsMinTime = gpsMinTime;
        this.geoHashPrecision = geoHashPrecision;
        this.geoHashMinPointCount = geoHashMinPointCount;
        this.sensorFfequencyHz = sensorFfequencyHz;
        this.filterMockGpsCoordinates = filterMockGpsCoordinates;
        this.mVelFactor = velFactor;
        this.mPosFactor = posFactor;
    }
}
