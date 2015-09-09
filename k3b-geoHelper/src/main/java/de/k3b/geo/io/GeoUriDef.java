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

package de.k3b.geo.io;

/** these constants are part of the geo-uri schema geo:lat,lon?q=.... and the poi xml format */
public class GeoUriDef {

    public static final String DESCRIPTION = "d";
    public static final String LINK = "link";
    public static final String SYMBOL = "s";
    public static final String QUERY = "q";
    public static final String ZOOM = "z";
    public static final String ZOOM_MAX = "z2";
    public static final String ID = "id";
    public static final String TIME = "t";

    // n=name is an alternative to geo:...q=(name)
    public static final String NAME = "n";
    // ll=lat,lon is an alternative to geo:...q=lat,lon
    public static final String LAT_LON = "ll";

    // xml-only
    public static final String XML_ELEMENT_POI = "poi";
    public static final String XML_ATTR_GEO_URI = "geoUri";

    // "true" of "1" means infer missing parameters
    public static final String XML_ATTR_GEO_URI_INFER_MISSING = "infer";
}
