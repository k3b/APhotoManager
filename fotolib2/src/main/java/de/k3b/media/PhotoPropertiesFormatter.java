/*
 * Copyright (c) 2019 by k3b.
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

import java.util.EnumSet;

import de.k3b.io.DateUtil;
import de.k3b.io.GeoUtil;
import de.k3b.tagDB.TagConverter;

public class PhotoPropertiesFormatter extends MediaFormatter {
    public static CharSequence format(IPhotoProperties item) {
        return format(item, true, null, (EnumSet<FieldID>) null);
    }

    public static CharSequence format(IPhotoProperties item, boolean includeEmpty, ILabelGenerator labeler, FieldID... _excludes) {
        return format(item, includeEmpty, labeler, toEnumSet(_excludes));
    }

    public static CharSequence format(IPhotoProperties item, boolean includeEmpty, ILabelGenerator _labeler, EnumSet<FieldID> excludes) {
        if (item == null) return "";

        ILabelGenerator labeler = (_labeler == null) ? defaultLabeler : _labeler;
        StringBuilder result = new StringBuilder();
        add(result, includeEmpty, excludes, FieldID.clasz, item.getClass().getSimpleName(), ":");
        add(result, includeEmpty, excludes, FieldID.path, labeler, item.getPath());
        add(result, includeEmpty, excludes, FieldID.dateTimeTaken, labeler, DateUtil.toIsoDateTimeString(item.getDateTimeTaken()));
        add(result, includeEmpty, excludes, FieldID.title, labeler, item.getTitle());
        add(result, includeEmpty, excludes, FieldID.description, labeler, item.getDescription());
        add(result, includeEmpty, excludes, FieldID.latitude_longitude, labeler, GeoUtil.toCsvStringLatLon(item.getLatitude()));
        // longitude used same flag as latitude but no label of it-s own
        add(result, includeEmpty, excludes, FieldID.latitude_longitude, ", ", GeoUtil.toCsvStringLatLon(item.getLongitude()));
        add(result, includeEmpty, excludes, FieldID.rating, labeler, item.getRating());
        add(result, includeEmpty, excludes, FieldID.visibility, labeler, item.getVisibility());
        add(result, includeEmpty, excludes, FieldID.tags, labeler, TagConverter.asDbString(null, item.getTags()));
        return result;
    }
}
