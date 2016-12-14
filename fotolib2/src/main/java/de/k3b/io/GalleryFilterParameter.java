/*
 * Copyright (c) 2015-2016 by k3b.
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

import java.sql.Date;
import java.text.SimpleDateFormat;

/**
 * parameter for foto filter: only fotos from certain filepath, date and/or lat/lon will be visible.
 * Created by k3b on 11.07.2015.
 */
public class GalleryFilterParameter extends GeoRectangle implements IGalleryFilter {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final String NON_GEO_ONLY      = "noGeoInfo";
    private static final String NON_GEO_ONLY_FIND = NON_GEO_ONLY.substring(0, 1);
    private String path = null;

    private long dateMin = 0;
    private long dateMax = 0;

    private boolean nonGeoOnly = false;

    private int mSortId = SORT_BY_NONE;
    private boolean mSortAscending = false;

    public GalleryFilterParameter get(IGalleryFilter src) {
        super.get(src);
        if (src != null) {
            this.setDateMax(src.getDateMax());
            this.setDateMin(src.getDateMin());
            this.setPath(src.getPath());
            this.setNonGeoOnly(src.isNonGeoOnly());
            this.setSort(src.getSortID(),src.isSortAscending());
        }
        return this;
    }



    /******************** properties **************************/
    @Override
    public String getPath() {
        return path;
    }

    public GalleryFilterParameter setPath(String path) {
        this.path = path;return this;
    }

    @Override
    public long getDateMin() {
        return dateMin;
    }

    public GalleryFilterParameter setDateMin(long dateMin) {
        this.dateMin = dateMin;return this;
    }

    @Override
    public long getDateMax() {
        return dateMax;
    }

    public GalleryFilterParameter setDateMax(long dateMax) {
        this.dateMax = dateMax; return this;
    }

    public GalleryFilterParameter setDate(String min, String max) {
        return setDate(parseDate(min), parseDate(max));
    }

    public GalleryFilterParameter setDate(long min, long max) {
        return setDateMin(min).setDateMax(max);
    }

    @Override
    public boolean isNonGeoOnly() {
        return nonGeoOnly;
    }

    public GalleryFilterParameter setNonGeoOnly(boolean nonGeoOnly) {
        this.nonGeoOnly = nonGeoOnly;
        return this;
    }

    /**
     * number defining current sorting
     */
    @Override
    public int getSortID() {
        return mSortId;
    }

    public GalleryFilterParameter setSortID(int sortID) {
        mSortId = sortID;
        return this;
    }

    /**
     * false: sort descending
     */
    @Override
    public boolean isSortAscending() {
        return mSortAscending;
    }

    public GalleryFilterParameter setSortAscending(boolean sortAscending) {
        mSortAscending = sortAscending;
        return this;
    }

    public GalleryFilterParameter setSort(int sortID, boolean sortAscending) {
        this.setSortID(sortID);
        this.setSortAscending(sortAscending);
        return this;
    }

    /********************* string conversion support ***************/
    @Override
    public StringBuilder toStringBuilder() {
        StringBuilder result = null;
        if (isNonGeoOnly()) {
            result = new StringBuilder();
            appendSubFields(result, NON_GEO_ONLY);
            appendSubFields(result, "");
        } else {
            result = super.toStringBuilder();
        }
        appendSubFields(result, format(getDateMin()), format(getDateMax()));
        appendSubFields(result, format(getPath()));
        if ((mSortId != SORT_BY_NONE) && (mSortId != SORT_BY_NONE_OLD)) {
            appendSubFields(result, "" + (char) mSortId, mSortAscending ? SORT_DIRECTION_ASCENDING : SORT_DIRECTION_DESCENDING);
        }
        return result;
    }

    private static String format(long millisecs) {
        if (millisecs==0) return "";
        return dateFormat.format(new Date(millisecs));
    }

    private static String format(String doubleValue) {
        if (doubleValue==null) return "";
        return doubleValue;
    }

    public static GalleryFilterParameter parse(String s, GalleryFilterParameter result) {
        if (s != null) {
            String[] fields = s.split(DELIM_FIELD);
            for (int fieldIndex=0; fieldIndex < fields.length; fieldIndex++) {
                result.assign(fieldIndex, fields[fieldIndex].split(DELIM_SUB_FIELD));
            }
        }
        return result;
    }

    private void assign(int fieldIndex, String[] subfields) {
        for (int subFieldIndex=0; subFieldIndex < subfields.length; subFieldIndex++) {
            String subfieldValue = subfields[subFieldIndex];
            if ((subfieldValue != null) && (subfieldValue.length() > 0)) {
                this.assign(fieldIndex, subFieldIndex, subfieldValue);
            }
        }
    }

    // field and subfield must be the same order as toString(Builder)()
    private void assign(int field, int subfield, String value) {
        switch (field) {
            case 0 :
                if (isNonGeoOnly(value)) {
                    setNonGeoOnly(true);
                } else {
                    setNonGeoOnly(false);
                    if (subfield == 0)
                        setLatitudeMin(parseLatLon(value));
                    else
                        setLogituedMin(parseLatLon(value));
                }
                break;
            case 1 :
                if (subfield == 0)
                    setLatitudeMax(parseLatLon(value));
                else
                    setLogituedMax(parseLatLon(value));
                break;
            case 2 :
                if (subfield == 0)
                    setDateMin(parseDate(value));
                else
                    setDateMax(parseDate(value));
                break;
            case 3 :
                setPath(value);
                break;
            case 4 :
                if (subfield == 0)
                    setSortID(value.charAt(0));
                else
                    setSortAscending(value.charAt(0) == SORT_DIRECTION_ASCENDING.charAt(0));
                break;
            default:break;
        }
    }

    private boolean isNonGeoOnly(String value) {
        return (value != null) && (value.toLowerCase().startsWith(NON_GEO_ONLY_FIND));
    }

    private static long parseDate(String value) {
        if ((value == null) || value.isEmpty()) return 0;
        try {
            return dateFormat.parse(value).getTime();
        } catch (Exception e1) {
            try {
                return Long.parseLong(value);
            } catch (Exception e2) {
                return 0;
            }
        }
    }

    public static boolean isEmpty(IGalleryFilter filter) {
        if (filter == null) return true;
        return (GeoRectangle.isEmpty(filter)
                && (filter.getDateMin() == 0)
                && (filter.getDateMax() == 0)
                && (!filter.isNonGeoOnly())
                && (filter.getPath()==null));
    }
}
