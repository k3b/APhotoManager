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

package de.k3b.android.androFotoFinder.tagDB;

import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.Date;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.media.IMetaApi;
import de.k3b.database.QueryParameter;
import de.k3b.tagDB.TagConverter;

/**
 * Database related code to handle non standard image processing (Tags, Description)
 *
 * Created by k3b on 30.09.2016.
 */

public class TagSql extends FotoSql {
    /** used to query non-standard-image fields */
    public static final Uri SQL_TABLE_EXTERNAL_CONTENT_URI_FILE = MediaStore.Files.getContentUri("external");

    public static final String SQL_COL_EXT_TAGS = MediaStore.Video.Media.TAGS;
    public static final String SQL_COL_EXT_DESCRIPTION = MediaStore.Images.Media.DESCRIPTION;
    public static final String SQL_COL_EXT_TITLE = MediaStore.Images.Media.TITLE;
    public static final String SQL_COL_EXT_RATING = MediaStore.Video.Media.BOOKMARK;

    /** The date & time when last non standard media-scan took place
     *  <P>Type: INTEGER (long) as seconds since jan 1, 1970</P> */
    private static final String SQL_COL_EXT_LAST_EXT_SCAN = MediaStore.Video.Media.DURATION;

    private static final String EXT_FILTER_MEDIA_TYPE
            = MediaStore.Files.FileColumns.MEDIA_TYPE
            + "='" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE+"'";

    /** only rows containing all tags are visible */
    public static void addWhereTag(QueryParameter newQuery, String... tags) {
        String tagvalue = (Global.enableNonStandardMediaFields) ? TagConverter.asDbString("%", tags) : null;
        if (tagvalue != null) {
            newQuery.addWhere(SQL_COL_EXT_TAGS + " like ?", tagvalue);
            switchFrom(newQuery, SQL_TABLE_EXTERNAL_CONTENT_URI_FILE);
        }
    }

    /** modifies the from part if not already set */
    private static void switchFrom(QueryParameter newQuery, Uri newTable) {
        boolean toFile = newTable.equals(SQL_TABLE_EXTERNAL_CONTENT_URI_FILE);
        Uri oldTable = (toFile)
                ? FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI
                : SQL_TABLE_EXTERNAL_CONTENT_URI_FILE;

        // must use different contentprovider that supports other columns
        if (newQuery.toFrom().compareTo(oldTable.toString()) == 0) {
            if (toFile) {
                // file table contain data for different media types.
                // We are only interested in images
                newQuery.addWhere(EXT_FILTER_MEDIA_TYPE);
            } else {
                // database image view does not contain media_type.
                // it is already filtered media_type=images
                // remove expression
                newQuery.removeWhere(EXT_FILTER_MEDIA_TYPE);
            }
            newQuery.replaceFrom(newTable.toString());
        }
    }

    public static void setTags(ContentValues values, String... tags) {
        values.put(SQL_COL_EXT_TAGS, TagConverter.asDbString("", tags));
        setLastScanDate(values, new Date());
    }

    public static void setDescription(ContentValues values, String description) {
        values.put(SQL_COL_EXT_DESCRIPTION, description);
        setLastScanDate(values, new Date());
    }

    public static void setRating(ContentValues values, Integer value) {
        values.put(SQL_COL_EXT_RATING, value);
        setLastScanDate(values, new Date());
    }

    public static void setLastScanDate(ContentValues values, Date lastScanDate) {
        if (Global.enableNonStandardMediaFieldsUpdateLastSCanTimestamp) {
            Long now = (lastScanDate != null)
                    ? lastScanDate.getTime() / 1000 // sec
                    : null;
            values.put(SQL_COL_EXT_LAST_EXT_SCAN, now);
        }
    }

    /** only rows are visible that needs to run the ext media scanner */
    public static void addWhereNeedsExtMediaScan(QueryParameter newQuery) {
        if (Global.enableNonStandardMediaFields) {
            newQuery.addWhere(SQL_COL_EXT_LAST_EXT_SCAN + " is null");
            switchFrom(newQuery, SQL_TABLE_EXTERNAL_CONTENT_URI_FILE);
        }
    }

    public static void setValues(ContentValues values, IMetaApi data) {

    }
}
