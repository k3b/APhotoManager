/*
 * Copyright (c) 2015 by k3b.
 *
 * This file is part of LocationMapViewer.
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

package de.k3b.geo.io.gpx;

/** xml-elements for gpx version 1.1 */
class GpxDef_11 {
    static final String TRKPT = "trkpt";
    static final String ATTR_LAT = "lat";
    static final String ATTR_LON = "lon";
    static final String NAME = "name";
    static final String DESC = "desc";
    static final String TIME = "time";
    static final String LINK = "link"; // <link href=.. /> also used by atom
    static final String ATTR_LINK = "href";
}

class GpxDef_10 {
    static final String WPT = "wpt"; // alias for "trkpt"
    static final String URL = "url"; // alias for "link"
}

class KmlDef_22 {
    static final String PLACEMARK = "Placemark";
    static final String DESCRIPTION = "description";
    static final String COORDINATES = "coordinates";
    static final String COORDINATES2 = "coord";
    static final String TIMESTAMP_WHEN = "when";
    static final String TIMESPAN_BEGIN = "begin";
}
