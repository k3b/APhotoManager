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
    public static final String DELIM_LAT_LON = ",";
    public static final String DELIM_LL_S = ";";

    private double latitudeMin = Double.NaN;
    private double latitudeMax = Double.NaN;
    private double logituedMin = Double.NaN;
    private double logituedMax = Double.NaN;

    public GeoRectangle get(IGeoRectangle src) {
        this.setLogituedMin(src.getLogituedMin());
        this.setLatitudeMin(src.getLatitudeMin());
        this.setLatitudeMax(src.getLatitudeMax());
        this.setLogituedMax(src.getLogituedMax());
        return this;
    }

    @Override
    public double getLatitudeMin() {
        return latitudeMin;
    }

    public void setLatitudeMin(double latitudeMin) {
        this.latitudeMin = latitudeMin;
    }

    @Override
    public double getLatitudeMax() {
        return latitudeMax;
    }

    public void setLatitudeMax(double latitudeMax) {
        this.latitudeMax = latitudeMax;
    }

    @Override
    public double getLogituedMin() {
        return logituedMin;
    }

    public void setLogituedMin(double logituedMin) {
        this.logituedMin = logituedMin;
    }

    @Override
    public double getLogituedMax() {
        return logituedMax;
    }

    public void setLogituedMax(double logituedMax) {
        this.logituedMax = logituedMax;
    }

    @Override
    public String toString() {
        return "" + getLatitudeMin() + DELIM_LAT_LON + getLogituedMin() + DELIM_LL_S + getLatitudeMax() + DELIM_LAT_LON + getLogituedMax();
    }

    public void setLatitude(double min, double max) {
        if (min > max) {
            // swap
            double temp = min;
            min = max;
            max = temp;
        }
        setLatitudeMin(min);
        setLatitudeMax(max);
    }

    public void setLogitude(double min, double max) {
        if (min > max) {
            // swap
            double temp = min;
            min = max;
            max = temp;
        }
        setLogituedMin(min);
        setLogituedMax(max);
    }
}
