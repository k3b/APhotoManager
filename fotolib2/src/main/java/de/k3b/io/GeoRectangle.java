/*
 * Copyright (c) 2015-2017 by k3b.
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

/**
 * parameter for foto filter by location: only fotos from certain lat/lon will be visible.
 * Created by k3b on 12.07.2015.
 */
public class GeoRectangle implements IGeoRectangle {
    public static final String DELIM_SUB_FIELD = ",";
    private static final char DELIM_FIELD_CHAR = ';';
    public static final String DELIM_FIELD = "" + DELIM_FIELD_CHAR;

    private double latitudeMin = Double.NaN;
    private double latitudeMax = Double.NaN;
    private double logituedMin = Double.NaN;
    private double logituedMax = Double.NaN;

    protected static double parseLatLon(String value) {
        if ((value == null) || value.isEmpty()) return Double.NaN;
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    public GeoRectangle get(IGeoRectangle src) {
        if (src != null) {
            this.setLogituedMin(src.getLogituedMin());
            this.setLatitudeMin(src.getLatitudeMin());
            this.setLatitudeMax(src.getLatitudeMax());
            this.setLogituedMax(src.getLogituedMax());
        }
        return this;
    }

    @Override
    public double getLatitudeMin() {
        return latitudeMin;
    }

    public GeoRectangle setLatitudeMin(double latitudeMin) {
        this.latitudeMin = latitudeMin; return this;
    }

    public static boolean isEmpty(IGeoRectangle rect) {
        if (rect == null) return true;
        return Double.isNaN(rect.getLatitudeMin())
                || ((rect.getLatitudeMin() == rect.getLatitudeMax()) && (rect.getLogituedMin() == rect.getLogituedMax()));
    }

    public boolean isEmpty() {
        return isEmpty(this);
    }

    /** inflate this-rectangle so that lat/lon is inside  */
    public void inflate(double lat, double lon) {
        latitudeMin = min(latitudeMin, lat);
        latitudeMax = max(latitudeMax, lat);
        logituedMin = min(logituedMin, lon);
        logituedMax = max(logituedMax, lon);
    }

    /** inflate this-rectangle so that it occupies percent more sapce >= minValue. */
    public void increase(double percent, double minValue) {
        double latitudeDelta = max(((latitudeMax - latitudeMin) * percent) / 200, minValue);
        latitudeMax = min(latitudeMax + latitudeDelta, 85.0);
        latitudeMin = max(latitudeMin - latitudeDelta, -85.0);

        double logitudeDelta = max(((logituedMax - logituedMin) * percent) / 200, minValue);
        logituedMax = min(logituedMax + logitudeDelta, 180.0);
        logituedMin = max(logituedMin - logitudeDelta, -180.0);
    }

    private static double min(double old, double newValue) {
        if (Double.isNaN(old) || (newValue < old)) return newValue;
        return old;
    }
    private static double max(double old, double newValue) {
        if (Double.isNaN(old) || (newValue > old)) return newValue;
        return old;
    }

    @Override
    public double getLatitudeMax() {
        return latitudeMax;
    }

    public GeoRectangle setLatitudeMax(double latitudeMax) {
        this.latitudeMax = latitudeMax; return this;
    }

    @Override
    public double getLogituedMin() {
        return logituedMin;
    }

    public GeoRectangle setLogituedMin(double logituedMin) {
        this.logituedMin = logituedMin; return this;
    }

    @Override
    public double getLogituedMax() {
        return logituedMax;
    }

    public GeoRectangle setLogituedMax(double logituedMax) {
        this.logituedMax = logituedMax; return this;
    }

    public GeoRectangle setLatitude(String min, String max) {
        //         return GalleryFilterParameter.parseLatLon(paramValue);
        return setLatitude(parseLatLon(min), parseLatLon(max));
    }

    public GeoRectangle setLatitude(double min, double max) {
        setLatitudeMin(Math.min(min, max));
        setLatitudeMax(Math.max(min, max));
        return this;
    }

    public GeoRectangle setLogitude(String min, String max) {
        //         return GalleryFilterParameter.parseLatLon(paramValue);
        return setLogitude(parseLatLon(min), parseLatLon(max));
    }

    public GeoRectangle setLogitude(double min, double max) {
        setLogituedMin(Math.min(min, max));
        setLogituedMax(Math.max(min, max));
        return this;
    }

    /********************* string conversion support ***************/
    @Override
    public String toString() {
        StringBuilder result = toStringBuilder();
        removeTrailingDelimiters(result);
        return result.toString().trim();
    }

    /** implementation detail of toString() to be overwritten by subclasses */
    protected StringBuilder toStringBuilder() {
        StringBuilder result = new StringBuilder();
        appendLatLon(result, getLatitudeMin(), getLogituedMin());
        appendLatLon(result, getLatitudeMax(), getLogituedMax());
        return result;
    }

    private static void removeTrailingDelimiters(StringBuilder result) {
        int len = result.length();
        while ((len > 0) && (result.charAt(len - 1) == DELIM_FIELD_CHAR)) {
            len--;
        }
        if (len < result.length()) result.setLength(len);
    }

    protected static StringBuilder appendLatLon(StringBuilder result, double lat, double lon) {
        return appendSubFields(result, format(lat), format(lon));
    }

    protected static StringBuilder appendSubFields(StringBuilder result, String... items) {
        if (items != null) {
            int last = items.length - 1;
            while ((last >= 0) && (items[last].isEmpty())) {
                last--;
            }

            if (last >= 0) {
                result.append(items[0]);
                for (int i=1; i <= last; i++) {
                    result.append(DELIM_SUB_FIELD).append(items[i]);
                }
            }
        }
        return result.append(DELIM_FIELD);
    }

    private static String format(double d) {
        if (Double.isNaN(d)) return "";
        return Double.toString(d);
    }

}
