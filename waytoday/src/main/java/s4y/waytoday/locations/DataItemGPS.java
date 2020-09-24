package s4y.waytoday.locations;


import android.hardware.GeomagneticField;
import android.location.Location;

import androidx.annotation.Nullable;

import mad.location.manager.lib.Commons.Utils;

/**
 * Created by lezh1k on 2/13/18.
 */

public class DataItemGPS extends DataItem {
    public final Location location;
    double velErr = NOT_INITIALIZED;
    double declination = NOT_INITIALIZED;

    public DataItemGPS(@Nullable Location location) {
        super();
        this.location = location;
        if (location != null) {
            //WARNING!!! here should be speed accuracy, but loc.hasSpeedAccuracy()
            // and loc.getSpeedAccuracyMetersPerSecond() requares API 26
            velErr = location.getAccuracy() * 0.1;
        }
    }

    public double getVelErr() {
        return velErr;
    }

    public double getDeclination() {
        if (declination == NOT_INITIALIZED) {
            long timeStamp;
            timeStamp = Utils.nano2milli(location.getElapsedRealtimeNanos());
            //WARNING!!! here should be speed accuracy, but loc.hasSpeedAccuracy()
            // and loc.getSpeedAccuracyMetersPerSecond() requares API 26
            double velErr = location.getAccuracy() * 0.1;


            GeomagneticField f = new GeomagneticField(
                    (float) location.getLatitude(),
                    (float) location.getLongitude(),
                    (float) location.getAltitude(),
                    timeStamp);
            declination = f.getDeclination();
        }
        return declination;
    }
}
