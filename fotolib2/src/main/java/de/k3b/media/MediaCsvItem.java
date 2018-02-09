/*
 * Copyright (c) 2016 - 2017 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
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

package de.k3b.media;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.k3b.csv2db.csv.CsvItem;
import de.k3b.io.GeoUtil;
import de.k3b.io.VISIBILITY;
import de.k3b.tagDB.TagConverter;

/**
 * csv storage for IMetaApi items
 *
 * Created by k3b on 10.10.2016.
 */

public class MediaCsvItem extends CsvItem implements IMetaApi {
    public final static String MEDIA_CSV_STANDARD_HEADER = MediaXmpFieldDefinition.SourceFile.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            MediaXmpFieldDefinition.title.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            MediaXmpFieldDefinition.description.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            MediaXmpFieldDefinition.DateTimeOriginal.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            MediaXmpFieldDefinition.GPSLatitude.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            MediaXmpFieldDefinition.GPSLongitude.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            MediaXmpFieldDefinition.subject.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            MediaXmpFieldDefinition.Rating.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            MediaXmpFieldDefinition.Visibility.getShortName();

    private int colFilePath;
    private int colFileModifyDate;

    private int colDateTimeTaken;
    private int colDateCreated;
    private int colCreateDate;
    private int colTitle;
    private int colDescription;
    private int colTags;
    private int colLatitude;
    private int colLongitude;
    private int colRating;
    private int colVisibility;

    /** there are cols 0..maxColumnIndex */
    public int maxColumnIndex;

    @Override
    public void setHeader(List<String> header) {
        maxColumnIndex = -1;
        super.setHeader(header);

        List<String> lcHeader = new ArrayList<String>(header.size());
        for (String lc: header) {
            lcHeader.add(lc.toLowerCase());
        }
        initFieldDefinitions(lcHeader);


    }

    protected void initFieldDefinitions(List<String> lcHeader) {
        // import specific
        colFilePath         = getColumnIndex(lcHeader, MediaXmpFieldDefinition.SourceFile);
        colFileModifyDate   = getColumnIndex(lcHeader, MediaXmpFieldDefinition.FileModifyDate);

        colDateTimeTaken    = getColumnIndex(lcHeader, MediaXmpFieldDefinition.DateTimeOriginal);
        colDateCreated      = getColumnIndex(lcHeader, MediaXmpFieldDefinition.DateCreated);
        colCreateDate       = getColumnIndex(lcHeader, MediaXmpFieldDefinition.CreateDate);
        colTitle            = getColumnIndex(lcHeader, MediaXmpFieldDefinition.title);
        colDescription      = getColumnIndex(lcHeader, MediaXmpFieldDefinition.description);
        colTags             = getColumnIndex(lcHeader, MediaXmpFieldDefinition.subject);
        colLatitude         = getColumnIndex(lcHeader, MediaXmpFieldDefinition.GPSLatitude);
        colLongitude        = getColumnIndex(lcHeader, MediaXmpFieldDefinition.GPSLongitude);
        colRating           = getColumnIndex(lcHeader, MediaXmpFieldDefinition.Rating);
        colVisibility       = getColumnIndex(lcHeader, MediaXmpFieldDefinition.Visibility);
    }

    @Override
    public String getPath() {
        return getString("getFilePath", colFilePath);
    }

    @Override
    public IMetaApi setPath(String filePath) {
        setString(filePath, colFilePath);
        return this;
    }

    public Date getFileModifyDate() {
        return getDate("getFileModifyDate", colFileModifyDate);
    }

    @Override
    public Date getDateTimeTaken() {
        return getDate("getDateTimeTaken", colDateTimeTaken, colDateCreated, colCreateDate);
    }

    @Override
    public IMetaApi setDateTimeTaken(Date value) {
        setDate(value, colCreateDate, colDateCreated, colDateTimeTaken );
        return this;
    }

    /** latitude, in degrees north. (-90 .. +90); longitude, in degrees east.  (-180 .. + 180)    */
    @Override public IMetaApi setLatitudeLongitude(Double latitude, Double longitude) {
        setString(GeoUtil.toCsvStringLatLon(latitude), colLatitude);
        setString(GeoUtil.toCsvStringLatLon(longitude), colLongitude);
        return this;
    }

    @Override
    public Double getLatitude() {
        return GeoUtil.parse(getString("getLatitude", colLatitude),"NS");
    }

    @Override
    public Double getLongitude() {
        return GeoUtil.parse(getString("getLongitude", colLongitude),"EW");
    }

    @Override
    public String getTitle() {
        return getString("getTitle", colTitle);
    }

    @Override
    public IMetaApi setTitle(String title) {
        setString(title, colTitle);
        return this;
    }

    @Override
    public String getDescription() {
        return getString("getDescription", colDescription);
    }

    @Override
    public IMetaApi setDescription(String description) {
        setString(description, colDescription);
        return this;
    }

    @Override
    public List<String> getTags() {
        String tags = getString("getTags", colTags);
        return TagConverter.fromString(tags);
    }

    @Override
    public IMetaApi setTags(List<String> tags) {
        setString(TagConverter.asDbString(null, tags), colTags);
        return this;
    }

    @Override
    public Integer getRating() {
        return getInteger("getRating", colRating);
    }

    @Override
    public IMetaApi setRating(Integer value) {
        setString(value, colRating);
        return this;
    }

    @Override
    public VISIBILITY getVisibility() {
        String sValue = getString("getVisibility", colVisibility);
        if (sValue == null) return null;
        return VISIBILITY.valueOf(sValue);
    }

    @Override
    public IMetaApi setVisibility(VISIBILITY value) {
        String sValue = VISIBILITY.isChangingValue(value) ? value.toString() : null;
        setString(sValue, colVisibility);
        return this;
    }

    /**
     * Returns the zero-based index for the given column name, or -1 if the column doesn't exist.
     *
     *
     * @param lcHeader
     * @param columnDefinition contains the name of the target column.
     * @return the zero-based column index for the given column name, or -1 if
     * the column name does not exist.
     */
    protected int getColumnIndex(List<String> lcHeader, MediaXmpFieldDefinition columnDefinition) {
        String destCcolumnName = columnDefinition.getShortName();
        int columnIndex = lcHeader.indexOf(destCcolumnName.toLowerCase());
        if (columnIndex > maxColumnIndex) maxColumnIndex = columnIndex;
        return columnIndex;
    }
}

