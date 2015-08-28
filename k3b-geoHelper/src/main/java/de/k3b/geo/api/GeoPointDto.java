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
 * Created by k3b on 07.01.2015.
 */
public class GeoPointDto implements ILocation, IGeoPointInfo, Cloneable   {
    /**
     * Latitude, in degrees north. NO_LAT_LON if not set
     */
    private double latitude = NO_LAT_LON;

    /**
     * Longitude, in degrees east. NO_LAT_LON if not set
     */
    private double longitude = NO_LAT_LON;

    /**
     * Date when the measurement was taken. Null if unknown.
     */
    private Date timeOfMeasurement = null;

    /**
     * Short non-unique text used as marker label. Null if not set.
     */
    private String name = null;

    /**
     * Detailed descript of the point displayed in popup on long-click . Null if not set.
     */
    private String description = null;

    /**
     * filter: this item is only shown if current zoom-level is >= this value. NO_ZOOM means no lower bound.
     */
    private int zoomMin = NO_ZOOM;

    /**
     * filter: this item is only shown if current zoom-level is <= this value. NO_ZOOM means no upper bound.
     */
    private int zoomMax = NO_ZOOM;

    /**
     * if not null: a unique id for this item.
     */
    private String id = null;

    /** Optional: if not null: link-url belonging to this item.<br/>
     * In show view after clicking on a marker: clock on button ">" opens this url.<br/>
     * persistet in geo-uri as geo:...&link=https://path/to/file.html
     * */
    private String link = null;

    /** Optional: if not null: url to an icon belonging to this item.<br/>
     * persistet in geo-uri as geo:...&s=https://path/to/file.html
     * */
    private String symbol = null;

    public GeoPointDto() {
    }

    public GeoPointDto(double latitude, double longitude,
                       String name, String description) {
        this(latitude, longitude,name, null, null, null, description, NO_ZOOM, NO_ZOOM, null);
    }

    public GeoPointDto(double latitude, double longitude,int zoomMin) {
        this(latitude, longitude,null, null, null, null, null, zoomMin, NO_ZOOM, null);
    }

    public GeoPointDto(double latitude, double longitude,
                       String name, String link, String symbol,
                       String id,
                       String description, int zoomMin, int zoomMax, Date timeOfMeasurement) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.link = link;
        this.symbol = symbol;
        this.id = id;
        this.description = description;
        this.zoomMin = zoomMin;
        this.zoomMax = zoomMax;
        this.timeOfMeasurement = timeOfMeasurement;
    }

    public GeoPointDto(IGeoPointInfo src) {
        if (src != null) {
            this.latitude = src.getLatitude();
            this.longitude = src.getLongitude();
            this.name = src.getName();
            this.link = src.getLink();
            this.symbol = src.getSymbol();
            this.id = src.getId();
            this.description = src.getDescription();
            this.zoomMin = src.getZoomMin();
            this.zoomMax = src.getZoomMax();
            this.timeOfMeasurement = src.getTimeOfMeasurement();
        }
    }

    /**
     * Latitude, in degrees north. NO_LAT_LON if not set
     */
    public GeoPointDto setLatitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    /**
     * Latitude, in degrees north. NO_LAT_LON if not set
     */
    @Override
    public double getLatitude() {
        return latitude;
    }

    /**
     * Longitude, in degrees east. NO_LAT_LON if not set
     */
    public GeoPointDto setLongitude(double longitude) {
        this.longitude = longitude;
        return this;
    }

    /**
     * Longitude, in degrees east. NO_LAT_LON if not set
     */
    @Override
    public double getLongitude() {
        return longitude;
    }

    /**
     * Date when the measurement was taken. Null if unknown.
     */
    public GeoPointDto setTimeOfMeasurement(Date timeOfMeasurement) {
        this.timeOfMeasurement = timeOfMeasurement;
        return this;
    }

    /**
     * Date when the measurement was taken. Null if unknown.
     */
    @Override
    public Date getTimeOfMeasurement() {
        return timeOfMeasurement;
    }

    /**
     * Short non-unique text used as marker label. Null if not set.
     */
    public GeoPointDto setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Short non-unique text used as marker label. Null if not set.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Detailed descript of the point displayed in popup on long-click . Null if not set.
     */
    public GeoPointDto setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Detailed descript of the point displayed in popup on long-click . Null if not set.
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * filter: this item is only shown if current zoom-level is >= this value. NO_ZOOM means no lower bound.
     */
    @Override
    public int getZoomMin() {
        return zoomMin;
    }

    public GeoPointDto setZoomMin(int zoomMin) {
        this.zoomMin = zoomMin;
        return this;
    }

    /**
     * filter: this item is only shown if current zoom-level is <= this value. NO_ZOOM means no upper bound.
     */
    @Override
    public int getZoomMax() {
        return zoomMax;
    }

    public GeoPointDto setZoomMax(int zoomMax) {
        this.zoomMax = zoomMax;
        return this;
    }

    /**
     * if not null: a unique id for this item.
     */
    @Override
    public String getId() {
        return id;
    }

    public GeoPointDto setId(String id) {
        this.id = id;
        return this;
    }

    /** Optional: if not null: link-url belonging to this item.<br/>
     * In show view after clicking on a marker: clock on button ">" opens this url.<br/>
     * persistet in geo-uri as geo:...&link=https://path/to/file.html
     * */
    @Override
    public String getLink() {
        return link;
    }

    /** Optional: if not null: link-url belonging to this item.<br/>
     * In show view after clicking on a marker: clock on button ">" opens this url.<br/>
     * persistet in geo-uri as geo:...&link=https://path/to/file.html
     * */
    public GeoPointDto setLink(String link) {
        this.link = link;
        return this;
    }

    /** Optional: if not null: icon-url belonging to this item.<br/>
     * persistet in geo-uri as geo:...&s=https://path/to/file.png
     * */
    @Override
    public String getSymbol() {
        return symbol;
    }

    /** Optional: if not null: icon-url belonging to this item.<br/>
     * persistet in geo-uri as geo:...&s=https://path/to/file.png
     * */
    public GeoPointDto setSymbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    /**
     * sets all members back to defaultvalue to allow reuse of class.
     *
     * @return this to to allow chains
     */
    public GeoPointDto clear() {
        this.latitude = GeoPointDto.NO_LAT_LON;
        this.longitude = GeoPointDto.NO_LAT_LON;
        this.name = null;
        this.link = null;
        this.symbol = null;
        this.id = null;
        this.description = null;
        this.zoomMin = NO_ZOOM;
        this.zoomMax = NO_ZOOM;
        this.timeOfMeasurement = null;
        return this;
    }

    public GeoPointDto clone() {
        try {
            return (GeoPointDto) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        if (this.name != null) return this.name;
        if (this.id != null) return "#" + this.id;
        return super.toString();
    }
}
