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

import de.k3b.csv2db.csv.CsvLoader;
import de.k3b.tagDB.TagConverter;

/**
 * Created by k3b on 10.10.2016.
 */

public class MediaCsvItem extends CsvLoader.CsvItem implements IMetaApi {
    private static final String CSV_PATH = "SourceFile"; // used by exiftool-csv
    private int colFilePath;
    private int colDateTimeTaken;
    private int colTitle;
    private int colDescription;
    private int colTags;
    private int colLatitude;
    private int colLongitude;

    @Override
    public void setHeader(List<String> header) {
        super.setHeader(header);
        colFilePath         = getColumnIndex(XmpFieldDefinition.PATH);
        colDateTimeTaken    = getColumnIndex(XmpFieldDefinition.DateTimeOriginal);
        colTitle            = getColumnIndex(XmpFieldDefinition.TITLE);
        colDescription      = getColumnIndex(XmpFieldDefinition.DESCRIPTION);
        colTags             = getColumnIndex(XmpFieldDefinition.TAGS);
        colLatitude         = getColumnIndex(XmpFieldDefinition.GPSLatitude);
        colLongitude        = getColumnIndex(XmpFieldDefinition.GPSLongitude);
    }

    @Override
    public String getPath() {
        if (colFilePath == -1) return null;
        return getString(colFilePath);
    }

    @Override
    public IMetaApi setPath(String filePath) {
        return null;
    }

    @Override
    public Date getDateTimeTaken() {
        if (colDateTimeTaken == -1) return null;
        return getDate(colDateTimeTaken);
    }

    @Override
    public IMetaApi setDateTimeTaken(Date value) {
        return null;
    }

    @Override
    public IMetaApi setLatitude(Double latitude) {
        return null;
    }

    @Override
    public IMetaApi setLongitude(Double longitude) {
        return null;
    }

    @Override
    public Double getLatitude() {
        if (colLatitude == -1) return null;
        return getDouble(colLatitude);
    }

    @Override
    public Double getLongitude() {
        if (colLongitude == -1) return null;
        return getDouble(colLongitude);
    }

    @Override
    public String getTitle() {
        if (colTitle == -1) return null;
        return getString(colTitle);
    }

    @Override
    public IMetaApi setTitle(String title) {
        return null;
    }

    @Override
    public String getDescription() {
        if (colDescription == -1) return null;
        return getString(colDescription);
    }

    @Override
    public IMetaApi setDescription(String description) {
        return null;
    }

    @Override
    public List<String> getTags() {
        if (colTags == -1) return null;
        String tags = getString(colTags);
        return TagConverter.fromString(tags);
    }

    @Override
    public IMetaApi setTags(List<String> tags) {
        return null;
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
        return header.indexOf(destCcolumnName);
    }
}

