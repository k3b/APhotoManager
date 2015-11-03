/*
 * Copyright (c) 2015 by k3b.
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
    public static final String DELIM_FIELD = ";";

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
        if (min > max) {
            // swap
            double temp = min;
            min = max;
            max = temp;
        }
        setLatitudeMin(min);
        setLatitudeMax(max);
        return this;
    }

    public GeoRectangle setLogitude(String min, String max) {
        //         return GalleryFilterParameter.parseLatLon(paramValue);
        return setLogitude(parseLatLon(min), parseLatLon(max));
    }

    public GeoRectangle setLogitude(double min, double max) {
        if (min > max) {
            // swap
            double temp = min;
            min = max;
            max = temp;
        }
        setLogituedMin(min);
        setLogituedMax(max);
        return this;
    }

    /********************* string conversion support ***************/
    @Override
    public String toString() {

        return toStringBuilder().toString();
    }

    protected StringBuilder toStringBuilder() {
        StringBuilder result = new StringBuilder();
        appendLatLon(result, getLatitudeMin(), getLogituedMin());
        appendLatLon(result, getLatitudeMax(), getLogituedMax());
        return result;
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
