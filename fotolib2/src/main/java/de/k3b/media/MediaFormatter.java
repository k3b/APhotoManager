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

import java.util.Arrays;
import java.util.EnumSet;

public class MediaFormatter {
    /**
     * used to identify a member of {@link IPhotoProperties} or {@link de.k3b.io.IGalleryFilter}
     */
    public enum FieldID {
        path,
        dateTimeTaken,
        title,
        description,
        latitude_longitude,
        rating,
        tags,
        clasz,
        visibility,
        find,
        lastModified
    }

    /** translates FieldID to text. In android this is implemented via resource id  */
    public interface ILabelGenerator {
        CharSequence get(FieldID id);
    }

    protected static final ILabelGenerator defaultLabeler = new ILabelGenerator() {
        @Override
        public CharSequence get(FieldID id) {
            return " " + id + " ";
        }
    };

    public static EnumSet<FieldID> toEnumSet(FieldID... _excludes) {
        return ((_excludes == null) || (_excludes.length == 0)) ? null : EnumSet.copyOf(Arrays.asList(_excludes));
    }

    protected static void add(StringBuilder result, boolean includeEmpty,
                            final EnumSet<FieldID> excludes, FieldID item,
                            ILabelGenerator labeler, Object value) {
        add(result, includeEmpty, excludes, item, labeler.get(item), value);
    }

    protected static void add(StringBuilder result, boolean includeEmpty,
                            final EnumSet<FieldID> excludes, FieldID item, CharSequence name, Object value) {
        if (name != null) {
            if ((includeEmpty) || (value != null)) {
                if ((excludes == null) || (!excludes.contains(item))) {
                    result.append(name).append(value);
                }
            }
        }
    }

}
