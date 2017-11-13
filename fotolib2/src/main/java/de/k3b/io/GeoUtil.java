/*
 * Copyright (c) 2016 by k3b.
 *
 * This file is part of AndroFotoFinder.
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

package de.k3b.io;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;

/**
 * Formatting and parsing of different latitude/longitude formats.
 *
 * Supports Degrees, Degrees+Minutes and Degrees+Minutes*Seconds with or without fractionals
 *
 * Created by k3b on 19.10.2016.
 */

public class GeoUtil {
    public static final Double NO_LAT_LON = Double.valueOf(0.0);

    // ###### maximum 6 digits after "."
    private static DecimalFormat doubleFormatter = new DecimalFormat("#.######", new DecimalFormatSymbols(Locale.US));

    public static Double parse(String degreeString, String plusMinus) {
        if (!StringUtils.isNullOrEmpty(degreeString)) {
            double result = 0;
            boolean isNegativ = false;
            int factor = 1;
            try {
                // degree, hours, minutes, seconds
                String[] parts = degreeString.toUpperCase().split("[:,; '\"°]");

                for (String part : parts) {
                    if ((part != null) && (part.length() > 0)) {
                        StringBuilder numberPart = new StringBuilder(part);
                        if (isNegative(numberPart, part, plusMinus)) {
                            isNegativ = true;
                        }
                        if (numberPart.length() > 0) {
                            double doubleValue = Double.parseDouble(numberPart.toString());
                            result += (doubleValue / factor);
                            factor *= 60;
                        }
                    }

                }

            } catch (NumberFormatException e) {
                // Some of the nubmers are not valid
                throw new IllegalArgumentException(degreeString, e);
            }

            if (isNegativ) return -result;
            return result;
        }
        return null;
    }

    public static String toXmpStringLatNorth(final Double lat) {
        return toString(lat, 2, ",", "NS");
    }

    public static String toXmpStringLonEast(final Double lon) {
        return toString(lon, 2, ",", "EW");
    }

    public static String toCsvStringLatLon(final Double lon) {
        return toString(lon, 1, ",", null);
    }

    /**
     * convert latitude_longitude into DMS (degree minute second) format. For instance<br/>
     * toString(-79.948862,3,",","EW") becomes<br/>
     * 79 degrees, 56 minutes, 55.903 seconds) West
     *  79,56,55.903W<br/>
     * It works for latitude and longitude<br/>
     * @param latLon latitude or longitude.
     * @param digits number of groups 1..3.
     * @param sperator added between groups.
     * @param plusMinus string with "+" and "-" chars to be appended. if null "-" may be prepended.
     * @return null if latLon is null.
     */
    public static String toString(final Double latLon, int digits, String sperator, String plusMinus) {
        if (GeoUtil.getValue(latLon) == NO_LAT_LON) return null;

        StringBuilder result = new StringBuilder();
        double remaining = latLon;
        int groupValueAsInt;

        char sign = (plusMinus != null) ? plusMinus.charAt(0) : 0;
        if (remaining < 0) {
            remaining *= -1;
            if (plusMinus != null) {
                sign = plusMinus.charAt(1);
            } else {
                result.append("-");
            }
        }
        for(int i=1; i < digits;i++) {
            groupValueAsInt = (int) remaining;

            result.append(groupValueAsInt).append(sperator);

            remaining -= groupValueAsInt;
            remaining *= 60;
        }

        result.append(doubleFormatter.format(remaining));
        if (sign != 0) result.append(sign);

        return result.toString();
    }


    private static boolean isNegative(StringBuilder doubleString, String value, String plusMinus) {
        boolean result = false;
        int plusMinusOffset = getPlusMinusOffset(value, plusMinus);
        if (plusMinusOffset >= 0) {
            if (plusMinus.charAt(1) == doubleString.charAt(plusMinusOffset)) {
                result = true;
            }
            doubleString.deleteCharAt(plusMinusOffset);
        }
        return result;
    }

    private static int getPlusMinusOffset(String value, String plusMinus) {
        for (char c : plusMinus.toCharArray()) {
            int offset = value.indexOf(c);
            if (offset >= 0) return offset;
        }
        return -1;
    }

    /** null save function: return if both are null or both non-null are the same.
     * special logig also obeying NaN */
    public static boolean equals(Double lhs, Double rhs) {
        return getValue(lhs).equals(getValue(rhs));
    }

    /** normalized getGeoValue with null or NaN are translated to NO_LAT_LON.
     *  #91: Fix Photo without geo may have different representations values for no-value
     *  @return always != null: either GeoUtil.NO_LAT_LON if null, empty, unknown or value
     */
    public static Double getValue(Double value) {
        if (value == null) return GeoUtil.NO_LAT_LON;
        double result = value.doubleValue();
        if (Double.isNaN(result) || (result == IGeoPointInfo.NO_LAT_LON)) return GeoUtil.NO_LAT_LON;
        return value;
    }

}
        