/*
 * Copyright (c) 2017 by k3b.
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

import java.io.StringReader;
import java.util.Arrays;

import de.k3b.csv2db.csv.CsvItem;
import de.k3b.csv2db.csv.CsvReader;
import de.k3b.io.FileUtils;

/**
 * A IPhotoProperties that can be converted to/from string using toString() and fromString().
 *
 * Created by k3b on 14.06.2017.
 */

public class PhotoPropertiesAsString extends PhotoPropertiesCsvItem implements IPhotoProperties {
    private int colExtra;

    public PhotoPropertiesAsString() {
        setFieldDelimiter(CsvItem.DEFAULT_CSV_FIELD_DELIMITER);
        String[] fields = MEDIA_CSV_STANDARD_HEADER.split(CsvItem.DEFAULT_CSV_FIELD_DELIMITER);
        this.setHeader(Arrays.asList(fields));
        int size = this.header.size();
        colExtra = size++;

        setData(new String[size]);
    }

    /** convert serializedContent back to IPhotoProperties if serializedContent was generated
     * by PhotoPropertiesAsString.toString() */
    public PhotoPropertiesAsString fromString(String serializedContent) {
        CsvReader reader = new CsvReader(new StringReader(serializedContent));
        setData(reader.readLine());
        FileUtils.close(reader,"PhotoPropertiesAsString.fromString");
        return this;
    }

    public PhotoPropertiesAsString setData(IPhotoProperties data) {
        this.clear();
        if (data != null) {
            PhotoPropertiesUtil.copy(this, data, true, true);
            if (data instanceof PhotoPropertiesAsString) {
                this.setExtra(((PhotoPropertiesAsString) data).getExtra());
            }
        }
        return this;
    }

    public String getExtra() {
        return getString("getExtra", colExtra);
    }

    public PhotoPropertiesAsString setExtra(String title) {
        setString(title, colExtra);
        return this;
    }

    public static String toString(IPhotoProperties mediaDefaults) {
        if (mediaDefaults != null) {
            return (mediaDefaults instanceof PhotoPropertiesAsString)
                    ? mediaDefaults.toString()
                    : new PhotoPropertiesAsString().setData(mediaDefaults).toString();
        }
        return null;
    }
}
