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

package de.k3b.io;

import java.util.Date;
import java.util.EnumSet;

import de.k3b.media.MediaFormatter;

public class GalleryFilterFormatter extends MediaFormatter {
    private final boolean includeEmpty;
    private final ILabelGenerator _labeler;
    private final EnumSet<FieldID> excludes;

    public GalleryFilterFormatter(boolean includeEmpty, ILabelGenerator labeler,
                                  FieldID... _excludes) {
        this.includeEmpty = includeEmpty;
        this._labeler = labeler;
        this.excludes = toEnumSet(_excludes);
    }

    public CharSequence format(IGalleryFilter item) {
        if (item == null) return "";

        ILabelGenerator labeler = (_labeler == null) ? defaultLabeler : _labeler;
        StringBuilder result = new StringBuilder();
        add(result, includeEmpty, excludes, FieldID.clasz, item.getClass().getSimpleName(), ":");
        add(result, includeEmpty, excludes, FieldID.path, labeler, item.getPath());
        add(result, includeEmpty, excludes, FieldID.dateTimeTaken, labeler,
                formatDate(item.getDateMin(), item.getDateMax()));
        add(result, includeEmpty, excludes, FieldID.lastModified, labeler,
                formatDate(item.getDateModifiedMin(), item.getDateModifiedMax()));
        add(result, includeEmpty, excludes, FieldID.rating, labeler, item.getRatingMin());
        add(result, includeEmpty, excludes, FieldID.visibility, labeler, item.getVisibility());
        add(result, includeEmpty, excludes, FieldID.find, labeler, item.getInAnyField());
        add(result, includeEmpty, excludes, FieldID.tags, labeler, GalleryFilterParameter.convertList(item.getTagsAllIncluded()));

        add(result, includeEmpty, excludes, FieldID.latitude_longitude, labeler,
                DirectoryFormatter.formatLatLon(
                        item.getLatitudeMin(), item.getLogituedMin(), item.getLatitudeMax(), item.getLogituedMax()));

        // add(result, includeEmpty, excludes, FieldID.sort, labeler, item.getPath());

        /*

boolean isSortAscending();
boolean isWithNoTags();
List<String> getTagsAllExcluded();

        */
        return result;
    }

    private CharSequence formatDate(long dateMin, long dateMax) {
        if ((dateMin == 0) && (dateMax == 0)) return null;

        StringBuffer result = new StringBuffer();
        if (dateMin != 0) result.append(DateUtil.toIsoDateString(new Date(dateMin)));
        result.append("...");
        if (dateMax != 0) result.append(DateUtil.toIsoDateString(new Date(dateMax)));
        return result;
    }

}
