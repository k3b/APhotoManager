/*
 * Copyright (c) 2016-2021 by k3b.
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
 * csv storage for IPhotoProperties items
 *
 * Created by k3b on 10.10.2016.
 */

public class PhotoPropertiesCsvItem extends CsvItem implements IPhotoProperties {
    public final static String MEDIA_CSV_STANDARD_HEADER = PhotoPropertiesXmpFieldDefinition.SourceFile.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            PhotoPropertiesXmpFieldDefinition.title.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            PhotoPropertiesXmpFieldDefinition.description.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            PhotoPropertiesXmpFieldDefinition.DateTimeOriginal.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            PhotoPropertiesXmpFieldDefinition.GPSLatitude.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            PhotoPropertiesXmpFieldDefinition.GPSLongitude.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            PhotoPropertiesXmpFieldDefinition.subject.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            PhotoPropertiesXmpFieldDefinition.Rating.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            PhotoPropertiesXmpFieldDefinition.Visibility.getShortName();

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
        colFilePath         = getColumnIndex(lcHeader, PhotoPropertiesXmpFieldDefinition.SourceFile);
        colFileModifyDate   = getColumnIndex(lcHeader, PhotoPropertiesXmpFieldDefinition.FileModifyDate);

        colDateTimeTaken    = getColumnIndex(lcHeader, PhotoPropertiesXmpFieldDefinition.DateTimeOriginal);
        colDateCreated      = getColumnIndex(lcHeader, PhotoPropertiesXmpFieldDefinition.DateCreated);
        colCreateDate       = getColumnIndex(lcHeader, PhotoPropertiesXmpFieldDefinition.CreateDate);
        colTitle            = getColumnIndex(lcHeader, PhotoPropertiesXmpFieldDefinition.title);
        colDescription      = getColumnIndex(lcHeader, PhotoPropertiesXmpFieldDefinition.description);
        colTags             = getColumnIndex(lcHeader, PhotoPropertiesXmpFieldDefinition.subject);
        colLatitude         = getColumnIndex(lcHeader, PhotoPropertiesXmpFieldDefinition.GPSLatitude);
        colLongitude        = getColumnIndex(lcHeader, PhotoPropertiesXmpFieldDefinition.GPSLongitude);
        colRating           = getColumnIndex(lcHeader, PhotoPropertiesXmpFieldDefinition.Rating);
        colVisibility       = getColumnIndex(lcHeader, PhotoPropertiesXmpFieldDefinition.Visibility);
    }

    @Override
    public String getPath() {
        return getString("getFilePath", colFilePath);
    }

    @Override
    public IPhotoProperties setPath(String filePath) {
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
    public IPhotoProperties setDateTimeTaken(Date value) {
        setDate(value, colCreateDate, colDateCreated, colDateTimeTaken );
        return this;
    }

    /** latitude, in degrees north. (-90 .. +90); longitude, in degrees east.  (-180 .. + 180)    */
    @Override public IPhotoProperties setLatitudeLongitude(Double latitude, Double longitude) {
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
    public IPhotoProperties setTitle(String title) {
        setString(title, colTitle);
        return this;
    }

    @Override
    public String getDescription() {
        return getString("getDescription", colDescription);
    }

    @Override
    public IPhotoProperties setDescription(String description) {
        setString(description, colDescription);
        return this;
    }

    @Override
    public List<String> getTags() {
        String tags = getString("getTags", colTags);
        return TagConverter.fromString(tags);
    }

    @Override
    public IPhotoProperties setTags(List<String> tags) {
        setString(TagConverter.asDbString(null, tags), colTags);
        return this;
    }

    @Override
    public Integer getRating() {
        return getInteger("getRating", colRating);
    }

    @Override
    public IPhotoProperties setRating(Integer value) {
        setString(value, colRating);
        return this;
    }

    @Override
    public VISIBILITY getVisibility() {
        String sValue = getString("getVisibility", colVisibility);
        if (sValue != null) {
            return VISIBILITY.valueOf(sValue);
        }
        return VISIBILITY.getVisibility(this);
    }

    @Override
    public IPhotoProperties setVisibility(VISIBILITY value) {
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
    protected int getColumnIndex(List<String> lcHeader, PhotoPropertiesXmpFieldDefinition columnDefinition) {
        String destCcolumnName = columnDefinition.getShortName();
        int columnIndex = lcHeader.indexOf(destCcolumnName.toLowerCase());
        if (columnIndex > maxColumnIndex) maxColumnIndex = columnIndex;
        return columnIndex;
    }
}

