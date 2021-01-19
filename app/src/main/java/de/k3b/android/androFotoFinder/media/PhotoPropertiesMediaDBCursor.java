/*
 * Copyright (c) 2016-2020 by k3b.
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

package de.k3b.android.androFotoFinder.media;

import android.database.Cursor;

import java.util.Date;
import java.util.List;

import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.io.VISIBILITY;
import de.k3b.media.IPhotoProperties;
import de.k3b.media.PhotoPropertiesFormatter;
import de.k3b.tagDB.TagConverter;

/**
 * r {@link IPhotoProperties} Implementation for {@link Cursor}.
 *
 * Created by k3b on 10.10.2016.
 */
public class PhotoPropertiesMediaDBCursor implements IPhotoProperties {
    private final Cursor cursor;
    private final int colId;
    private final int colFilePath;
    private final int colDateTimeTaken;
    private final int colTitle;
    private final int colDescription;
    private final int colTags;
    private final int colLatitude;
    private final int colLongitude;
    private final int colRating;
    private final int colType;

    public PhotoPropertiesMediaDBCursor(Cursor cursor) {
        this.cursor = cursor;

        colId               = getColumnIndex(TagSql.SQL_COL_PK);
        colFilePath         = getColumnIndex(TagSql.SQL_COL_PATH);
        colDateTimeTaken    = getColumnIndex(TagSql.SQL_COL_DATE_TAKEN);
        colTitle            = getColumnIndex(TagSql.SQL_COL_EXT_TITLE);
        colDescription      = getColumnIndex(TagSql.SQL_COL_EXT_DESCRIPTION);
        colTags             = getColumnIndex(TagSql.SQL_COL_EXT_TAGS);
        colLatitude         = getColumnIndex(TagSql.SQL_COL_LAT);
        colLongitude        = getColumnIndex(TagSql.SQL_COL_LON);
        colRating           = getColumnIndex(TagSql.SQL_COL_EXT_RATING);
        colType             = getColumnIndex(TagSql.SQL_COL_EXT_MEDIA_TYPE);
    }

    public Integer getID() {
        if (colId == -1) return null;
        return cursor.getInt(colId);
    }


    @Override
    public String getPath() {
        if (colFilePath == -1) return null;
        return cursor.getString(colFilePath);
    }

    @Override
    public Date getDateTimeTaken() {
        return FotoSql.getDate(cursor, colDateTimeTaken);
    }

    @Override
    public Double getLatitude() {
        if (colLatitude == -1) return null;
        return cursor.getDouble(colLatitude);
    }

    @Override
    public Double getLongitude() {
        if (colLongitude == -1) return null;
        return cursor.getDouble(colLongitude);
    }

    @Override
    public String getTitle() {
        if (colTitle == -1) return null;
        return cursor.getString(colTitle);
    }

    @Override
    public String getDescription() {
        if (colDescription == -1) return null;
        return cursor.getString(colDescription);
    }

    /**
     * 5=best .. 1=worst or 0/null unknown
     */
    @Override
    public Integer getRating() {
        if (colRating == -1) return null;
        return cursor.getInt(colRating);
    }

    @Override
    public VISIBILITY getVisibility() {
        if (colType == -1) return null;
        Integer ty = cursor.getInt(colType);
        if (ty != null) {
            return FotoSql.mediaType2Visibility(ty.intValue());
        }
        return null;
    }

    @Override
    public List<String> getTags() {
        if (colTags == -1) return null;
        return TagConverter.fromString(cursor.getString(colTags));
    }

    /**
     * Returns the zero-based index for the given column name, or -1 if the column doesn't exist.
     *
     * @param columnName the name of the target column.
     * @return the zero-based column index for the given column name, or -1 if
     * the column name does not exist.
     */
    protected int getColumnIndex(String columnName) {
        return cursor.getColumnIndex(columnName);
    }

    @Override
    public IPhotoProperties setPath(String filePath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IPhotoProperties setDateTimeTaken(Date value) {
        throw new UnsupportedOperationException();
    }

    @Override public IPhotoProperties setLatitudeLongitude(Double latitude, Double longitude) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IPhotoProperties setTitle(String title) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IPhotoProperties setDescription(String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IPhotoProperties setTags(List<String> tags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IPhotoProperties setRating(Integer value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IPhotoProperties setVisibility(VISIBILITY value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return PhotoPropertiesFormatter.format(this).toString();
    }
}
