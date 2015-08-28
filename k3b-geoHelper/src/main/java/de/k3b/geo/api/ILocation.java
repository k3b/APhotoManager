package de.k3b.geo.api;

import java.util.Date;

/**
 * Represents a geografic location: latitude(in degrees north), longitude(in degrees east)
 * Interface to make the lib independant from Android and other location sources.<br/>
 *  <br/>
 * Created by k3b on 11.05.2014.
 */
public interface ILocation {
    /** Get the latitude, in degrees north. */
    double getLatitude();
    /** Get the longitude, in degrees east. */
    double getLongitude();
    /** Get the date when the measurement was taken. Null if unknown. */
    Date getTimeOfMeasurement();
}
