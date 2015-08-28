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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.util.IsoDateTimeParser;

/**
 * converts between string an geo-component-type.
 *
 * Created by k3b on 25.03.2015.
 */
public class GeoFormatter {
    /* converter for Datatypes */
    private static final DecimalFormat latLonFormatter = new DecimalFormat("#.#######", new DecimalFormatSymbols(Locale.ENGLISH));
    private static final DateFormat timeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static {
        timeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /** parsing helper: converts a double value from string to double */
    public static double parseLatOrLon(String val) throws ParseException {
        if ((val == null) || (val.length() == 0)) return IGeoPointInfo.NO_LAT_LON;
        return latLonFormatter.parse(val).doubleValue();
    }

    public static String formatLatLon(double latitude) {
        if (latitude != IGeoPointInfo.NO_LAT_LON) {
            return latLonFormatter.format(latitude);
        }
        return "";
    }

    public static String formatDate(Date date) {
        if (date != null) {
            return timeFormatter.format(date);
        }
        return "";
    }

    public static String formatZoom(int val) {
        if (val != IGeoPointInfo.NO_ZOOM) {
            return Integer.toString(val);
        }
        return "";
    }

    /** parsing helper: converts value into zoom compatible int */
    public static int parseZoom(String value) {
        if (value != null) {
            try {
                int result = Integer.parseInt(value);
                if ((result >= 0) && (result < 64)) {
                    return result;
                }
            } catch (Exception ignore) {
            }
        }
        return IGeoPointInfo.NO_ZOOM;
    }
}
