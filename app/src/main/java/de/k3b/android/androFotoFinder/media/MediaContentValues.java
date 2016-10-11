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
import de.k3b.media.IMetaApi;
import de.k3b.tagDB.TagConverter;

import static de.k3b.android.androFotoFinder.tagDB.TagSql.SQL_COL_EXT_TAGS;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.setLastScanDate;

/**
 * r/w {@link IMetaApi} Implementation for {@link ContentValues}.
 *
 * Created by k3b on 10.10.2016.
 */

public class MediaContentValues implements IMetaApi {
    private ContentValues data;

    public MediaContentValues set(ContentValues data) {
        this.data = data;
        return this;
    }

    public MediaContentValues setID(Integer id) {
        data.put(FotoSql.SQL_COL_PK, id);
        return this;
    }

    public Integer getID() {
        return data.getAsInteger(FotoSql.SQL_COL_PK);
    }

    public MediaContentValues clear(){
        data.clear();
        return this;
    }
    // ############# IMetaApi

    @Override
    public String getPath() {
        return data.getAsString(FotoSql.SQL_COL_PATH);
    }

    @Override
    public IMetaApi setPath(String filePath) {
        data.put(FotoSql.SQL_COL_PATH, filePath);
        return this;
    }

    @Override
    public Date getDateTimeTaken() {
        Integer milliSecsOrNull = data.getAsInteger(FotoSql.SQL_COL_DATE_TAKEN);
        int milliSecs = (milliSecsOrNull == null) ? 0 : milliSecsOrNull.intValue();
        if (milliSecs == 0) return null;
        return new Date(milliSecs);
    }

    @Override
    public IMetaApi setDateTimeTaken(Date value) {
        data.put(FotoSql.SQL_COL_PATH, (value != null) ? value.getTime() : null);
        return this;
    }

    @Override
    public IMetaApi setLatitude(Double latitude) {
        data.put(FotoSql.SQL_COL_LAT, latitude);
        return this;
    }

    @Override
    public IMetaApi setLongitude(Double longitude) {
        data.put(FotoSql.SQL_COL_LON, longitude);
        return this;
    }

    @Override
    public Double getLatitude() {
        return  data.getAsDouble(FotoSql.SQL_COL_LAT);
    }

    @Override
    public Double getLongitude() {
        return  data.getAsDouble(FotoSql.SQL_COL_LON);
    }

    @Override
    public String getTitle() {
        return data.getAsString(TagSql.SQL_COL_EXT_TITLE);
    }

    @Override
    public IMetaApi setTitle(String title) {
        data.put(TagSql.SQL_COL_EXT_TITLE, title);
        return this;
    }

    @Override
    public String getDescription() {
        return data.getAsString(TagSql.SQL_COL_EXT_DESCRIPTION);
    }

    @Override
    public IMetaApi setDescription(String description) {
        TagSql.setDescription(data, description);
        return this;
    }

    @Override
    public List<String> getTags() {
        return TagConverter.fromString(data.getAsString(TagSql.SQL_COL_EXT_TAGS));
    }

    @Override
    public IMetaApi setTags(List<String> tags) {
        data.put(SQL_COL_EXT_TAGS, TagConverter.asString("",tags));
        setLastScanDate(data, new Date());
        return this;
    }
}
