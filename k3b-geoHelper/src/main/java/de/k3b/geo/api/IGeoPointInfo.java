/*
 * Copyright (C) 2015 k3b
 *
 * This file is part of de.k3b.android.LocationMapViewer (https://github.com/k3b/LocationMapViewer/) .
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */

package de.k3b.geo.api;

import java.util.Date;

/**
 * A location or trackpoint that can be displayed in a locationmap.<br/>
 * <p/>
 * Created by k3b on 13.01.2015.
 */
public interface IGeoPointInfo extends ILocation, Cloneable  {
    public static final double NO_LAT_LON = Double.MAX_VALUE;
    public static final int NO_ZOOM = -1;

    /** Mandatory: Latitude, in degrees north. <br/>
     * In show view: navigate map to this location.<br/>
     * In geo data: map display data.<br/>
     * NO_LAT_LON if not set.<br/>
     * persistet as geo:lat,lon or geo:0,0?q=lat,lon.
     * */
    double getLatitude();

    /** Mandatory: Longitude, in degrees east.  <br/>
     * In show view: navigate map to this location.<br/>
     * In geo data: map display data.<br/>
     * NO_LAT_LON if not set.<br/>
     * persistet as geo:lat,lon or geo:0,0?q=lat,lon.
     * */
    double getLongitude();

    /** Optional:
     * In show view: navigate map to this zoom level.<br/>
     * In geo data: filter - this item is only shown if current zoom-level is >= this value.<br/>
     * NO_LAT_LON if not set.<br/>
     * NO_ZOOM means no lower bound.<br/>
     * persistet in geo-uri as geo:...&z=4
     * */
    int getZoomMin();

    /** Optional in geo data as filter criteria: this item is only shown
     * if current zoom-level is <= this value. NO_ZOOM means no upper bound.<br/>
     * persistet in geo-uri as geo:...&z2=6
     * */
    int getZoomMax();

    /** Optional: Date when the measurement was taken. Null if unknown.<br/>
     * This may be shown in a map as an alternative label<br/>
     * or used as a filter to include only geopoints of a certain date range.<br/>
     * persistet in geo-uri as geo:...&t=2015-03-24T15:39:52z  */
    Date getTimeOfMeasurement();

    /** Optional: Short non-unique text used as marker label. <br/>
     * Null if not set.<br/>
     * In show view after clicking on a marker: Caption/Title in the bubble.<br/>
     * persistet in geo-uri as geo:?q=...(name)
     * */
    String getName();

    /** Optional: Detailed description of the point displayed in popup on long-click.<br/>
     * Null if not set.<br/>
     * In show view after clicking on a marker: Text in the bubble.<br/>
     * persistet in geo-uri as geo:...&d=someDescription
     * */
    String getDescription();

    /** Optional: if not null: a unique id for this item.<br/>
     * persistet in geo-uri as geo:...&id=4711
     * */
    String getId();

    /** Optional: if not null: link-url belonging to this item.<br/>
     * In show view after clicking on a marker: clock on button ">" opens this url.<br/>
     * persistet in geo-uri as geo:...&link=https://path/to/file.html
     * */
    String getLink();
    /** Optional: if not null: icon-url belonging to this item.<br/>
     * persistet in geo-uri as geo:...&s=https://path/to/file.png
     * */
    String getSymbol();

    IGeoPointInfo clone();
}
