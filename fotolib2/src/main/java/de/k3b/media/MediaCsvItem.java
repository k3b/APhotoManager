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

package de.k3b.media;

import java.util.Date;
import java.util.List;

import de.k3b.csv2db.csv.CsvItem;
import de.k3b.io.GeoUtil;
import de.k3b.tagDB.TagConverter;

/**
 * Created by k3b on 10.10.2016.
 */

public class MediaCsvItem extends CsvItem implements IMetaApi {
    public final static String MEDIA_CSV_STANDARD_HEADER = XmpFieldDefinition.PATH.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            XmpFieldDefinition.TITLE.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            XmpFieldDefinition.DESCRIPTION.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            XmpFieldDefinition.DateTimeOriginal.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            XmpFieldDefinition.GPSLatitude.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            XmpFieldDefinition.GPSLongitude.getShortName() + DEFAULT_CSV_FIELD_DELIMITER +
            XmpFieldDefinition.TAGS.getShortName();

    private static final String CSV_PATH = "SourceFile"; // used by exiftool-csv
    private int colFilePath;
    private int colDateTimeTaken;
    private int colDateCreated;
    private int colCreateDate;
    private int colTitle;
    private int colDescription;
    private int colTags;
    private int colLatitude;
    private int colLongitude;

    /** there are cols 0..maxColumnIndex */
    int maxColumnIndex;

    @Override
    public void setHeader(List<String> header) {
        maxColumnIndex = -1;
        super.setHeader(header);
        colFilePath         = getColumnIndex(XmpFieldDefinition.PATH);
        colDateTimeTaken    = getColumnIndex(XmpFieldDefinition.DateTimeOriginal);
        colDateCreated    = getColumnIndex(XmpFieldDefinition.DateCreated);
        colCreateDate    = getColumnIndex(XmpFieldDefinition.CreateDate);
        colTitle            = getColumnIndex(XmpFieldDefinition.TITLE);
        colDescription      = getColumnIndex(XmpFieldDefinition.DESCRIPTION);
        colTags             = getColumnIndex(XmpFieldDefinition.TAGS);
        colLatitude         = getColumnIndex(XmpFieldDefinition.GPSLatitude);
        colLongitude        = getColumnIndex(XmpFieldDefinition.GPSLongitude);
    }

    @Override
    public String getPath() {
        return getString(colFilePath);
    }

    @Override
    public IMetaApi setPath(String filePath) {
        setString(filePath, colFilePath);
        return this;
    }

    @Override
    public Date getDateTimeTaken() {
        return getDate(colDateTimeTaken, colDateCreated, colCreateDate);
    }

    @Override
    public IMetaApi setDateTimeTaken(Date value) {
        setDate(value, colCreateDate, colDateCreated, colDateTimeTaken );
        return this;
    }

    @Override
    public IMetaApi setLatitude(Double latitude) {
        setString(GeoUtil.toCsvStringLatLon(latitude), colLatitude);
        return this;
    }

    @Override
    public IMetaApi setLongitude(Double longitude) {
        setString(GeoUtil.toCsvStringLatLon(longitude), colLongitude);
        return this;
    }

    @Override
    public Double getLatitude() {
        return GeoUtil.parse(getString(colLatitude),"NS");
    }

    @Override
    public Double getLongitude() {
        return GeoUtil.parse(getString(colLongitude),"EW");
    }

    @Override
    public String getTitle() {
        return getString(colTitle);
    }

    @Override
    public IMetaApi setTitle(String title) {
        setString(title, colTitle);
        return this;
    }

    @Override
    public String getDescription() {
        return getString(colDescription);
    }

    @Override
    public IMetaApi setDescription(String description) {
        setString(description, colDescription);
        return this;
    }

    @Override
    public List<String> getTags() {
        String tags = getString(colTags);
        return TagConverter.fromString(tags);
    }

    @Override
    public IMetaApi setTags(List<String> tags) {
        setString(TagConverter.asDbString(null, tags), colTags);
        return this;
    }

    /**
     * Returns the zero-based index for the given column name, or -1 if the column doesn't exist.
     *
     * @param columnDefinition contains the name of the target column.
     * @return the zero-based column index for the given column name, or -1 if
     * the column name does not exist.
     */
    protected int getColumnIndex(XmpFieldDefinition columnDefinition) {
        String destCcolumnName = columnDefinition.getShortName();
        int columnIndex = header.indexOf(destCcolumnName);
        if (columnIndex > maxColumnIndex) maxColumnIndex = columnIndex;
        return columnIndex;
    }
}

