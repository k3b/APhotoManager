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
import java.util.HashMap;
import java.util.Map;

import de.k3b.media.MediaFormatter;

public class GalleryFilterFormatter extends MediaFormatter {
    private final boolean includeEmpty;
    private final ILabelGenerator _labeler;
    private final EnumSet<FieldID> excludes;

    public GalleryFilterFormatter(boolean includeEmpty, ILabelGenerator labeler,
                                  FieldID... _excludes) {
        this.includeEmpty = includeEmpty;
        this._labeler = (labeler != null) ? labeler : defaultLabeler;
        this.excludes = toEnumSet(_excludes);
    }

    public CharSequence format(IGalleryFilter item) {
        if (item == null) return "";

        final StringBuilder result = new StringBuilder();
        new Binder() {
            void bind(FieldID id, Object value) {
                result.append(_labeler.get(id)).append(value);
            }
        }.bind(item);
        return result;
    }

    public Map<CharSequence, Object> asMap(IGalleryFilter item) {
        if (item == null) return null;

        final HashMap<CharSequence, Object> result = new HashMap();
        new Binder() {
            void bind(FieldID id, Object value) {
                result.put(_labeler.get(id), value);
            }
        }.bind(item);
        return result;
    }

    private abstract class Binder {
        abstract void bind(FieldID id, Object value);

        void bindIf(FieldID id, Object value) {
            if (notEmpty(id, value, excludes, includeEmpty)) {
                bind(id, value);
            }
        }

        void bind(IGalleryFilter item) {
            bindIf(FieldID.clasz, item.getClass().getSimpleName());
            bindIf(FieldID.path, item.getPath());
            bindIf(FieldID.dateTimeTaken, formatDate(item.getDateMin(), item.getDateMax()));
            bindIf(FieldID.lastModified, formatDate(item.getDateModifiedMin(), item.getDateModifiedMax()));
            bindIf(FieldID.rating, item.getRatingMin());
            bindIf(FieldID.visibility, item.getVisibility());
            bindIf(FieldID.find, item.getInAnyField());
            bindIf(FieldID.tags, GalleryFilterParameter.convertList(item.getTagsAllIncluded()));

            bindIf(FieldID.latitude_longitude, DirectoryFormatter.formatLatLon(
                    item.getLatitudeMin(), item.getLogituedMin(), item.getLatitudeMax(), item.getLogituedMax()));

        /* ignored

            bindIf(includeEmpty, excludes, FieldID.sort, labeler, item.getPath());

            boolean isSortAscending();
            boolean isWithNoTags();
            List<String> getTagsAllExcluded();
        */
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
}
