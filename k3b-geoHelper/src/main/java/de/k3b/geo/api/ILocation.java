/*
 * Copyright (C) 2015-2016 k3b
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
 * Represents a geographic location: latitude(in degrees north), longitude(in degrees east)
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
