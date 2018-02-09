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

package de.k3b.android.androFotoFinder.media;

import android.content.ContentValues;

import java.util.Date;
import java.util.List;

import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.io.VISIBILITY;
import de.k3b.media.IMetaApi;
import de.k3b.media.MediaUtil;
import de.k3b.tagDB.TagConverter;

/**
 * r/w {@link IMetaApi} Implementation for android databse/contentprovider {@link ContentValues}.
 *
 * Created by k3b on 10.10.2016.
 */

public class MediaContentValues implements IMetaApi {
    private ContentValues mData;
    private Date mXmpFileModifyDate;

    public MediaContentValues set(ContentValues data, Date xmpLastFileModifyDate) {
        if (data != null) this.mData = data;
        mXmpFileModifyDate = xmpLastFileModifyDate;
        return this;
    }

    public ContentValues getContentValues(){
        return mData;
    }

    public MediaContentValues setID(Integer id) {
        mData.put(FotoSql.SQL_COL_PK, id);
        return this;
    }

    public Integer getID() {
        return mData.getAsInteger(FotoSql.SQL_COL_PK);
    }

    public MediaContentValues clear(){
        mData.clear();
        return this;
    }
    // ############# IMetaApi

    @Override
    public String getPath() {
        return mData.getAsString(FotoSql.SQL_COL_PATH);
    }

    @Override
    public IMetaApi setPath(String filePath) {
        mData.put(FotoSql.SQL_COL_PATH, filePath);
        return this;
    }

    @Override
    public Date getDateTimeTaken() {
        Long milliSecsOrNull = mData.getAsLong(FotoSql.SQL_COL_DATE_TAKEN);
        long milliSecs = (milliSecsOrNull == null) ? 0 : milliSecsOrNull.longValue();
        if (milliSecs == 0) return null;
        return new Date(milliSecs);
    }

    @Override
    public IMetaApi setDateTimeTaken(Date value) {
        Long milliSecs = (value != null) ? value.getTime() : null;
        mData.put(FotoSql.SQL_COL_DATE_TAKEN, milliSecs);
        return this;
    }

    @Override public IMetaApi setLatitudeLongitude(Double latitude, Double longitude) {
        mData.put(FotoSql.SQL_COL_LAT, latitude);
        mData.put(FotoSql.SQL_COL_LON, longitude);
        return this;
    }

    @Override
    public Double getLatitude() {
        return  mData.getAsDouble(FotoSql.SQL_COL_LAT);
    }

    @Override
    public Double getLongitude() {
        return  mData.getAsDouble(FotoSql.SQL_COL_LON);
    }

    @Override
    public String getTitle() {
        return mData.getAsString(TagSql.SQL_COL_EXT_TITLE);
    }

    @Override
    public IMetaApi setTitle(String title) {
        mData.put(TagSql.SQL_COL_EXT_TITLE, title);
        return this;
    }

    @Override
    public String getDescription() {
        return mData.getAsString(TagSql.SQL_COL_EXT_DESCRIPTION);
    }

    @Override
    public IMetaApi setDescription(String description) {
        TagSql.setDescription(mData, mXmpFileModifyDate, description);
        return this;
    }

    @Override
    public List<String> getTags() {
        return TagConverter.fromString(mData.getAsString(TagSql.SQL_COL_EXT_TAGS));
    }

    @Override
    public IMetaApi setTags(List<String> tags) {
        mData.put(TagSql.SQL_COL_EXT_TAGS, TagConverter.asDbString("",tags));
        setLastXmpFileModifyDate();
        return this;
    }

    /**
     * 5=best .. 1=worst or 0/null unknown
     */
    @Override
    public Integer getRating() {
        return mData.getAsInteger(TagSql.SQL_COL_EXT_RATING);
    }

    @Override
    public IMetaApi setRating(Integer value) {
        mData.put(TagSql.SQL_COL_EXT_RATING, value);
        setLastXmpFileModifyDate();
        return this;
    }

    @Override
    public VISIBILITY getVisibility() {
        Integer ty = mData.getAsInteger(TagSql.SQL_COL_EXT_MEDIA_TYPE);
        if (ty != null) {
            if (ty.intValue() == FotoSql.MEDIA_TYPE_IMAGE_PRIVATE)
                return VISIBILITY.PRIVATE;

            return VISIBILITY.PUBLIC;
        }
        return null;
    }

    @Override
    public IMetaApi setVisibility(VISIBILITY value) {
        if (VISIBILITY.isChangingValue(value)) {
            int iValue = (value.equals(VISIBILITY.PRIVATE))
                    ? FotoSql.MEDIA_TYPE_IMAGE_PRIVATE
                    : FotoSql.MEDIA_TYPE_IMAGE;
            mData.put(TagSql.SQL_COL_EXT_MEDIA_TYPE, iValue);
            setLastXmpFileModifyDate();
        }
        return this;
    }

    protected void setLastXmpFileModifyDate() {
        if (mXmpFileModifyDate != null) {
            TagSql.setXmpFileModifyDate(mData, mXmpFileModifyDate);
        }
    }

    @Override
    public String toString() {
        return MediaUtil.toString(this);
    }
}
