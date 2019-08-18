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

import de.k3b.io.DirectoryFormatter;

public class MediaFormatter {
    protected static final ILabelGenerator defaultLabeler = new DefaultLabelGenerator(" ", " ");

    protected static void add(StringBuilder result, boolean includeEmpty,
                              final EnumSet<FieldID> excludes, FieldID id,
                              CharSequence name, Object value, boolean isEmpty) {

        if (notEmpty(id, value, excludes, includeEmpty, isEmpty)) {
            result.append(name).append(value);
        }
    }

    /**
     * translates FieldID to text. In android this is implemented via resource id
     */
    public interface ILabelGenerator {
        CharSequence get(FieldID id);
    }

    protected static boolean notEmpty(FieldID id, Object value, EnumSet<FieldID> excludes,
                                      boolean includeEmpty, boolean isEmpty) {
        if (value == null) isEmpty = true;
        return (((includeEmpty) || !isEmpty) &&
                ((excludes == null) || (!excludes.contains(id))));

    }

    public static class DefaultLabelGenerator implements ILabelGenerator {

        private final String idPrefix;
        private final String idSuffix;

        public DefaultLabelGenerator(String idPrefix, String idSuffix) {
            this.idPrefix = idPrefix;
            this.idSuffix = idSuffix;
        }

        @Override
        public CharSequence get(FieldID id) {
            if (id == FieldID.clasz) return "";
            return idPrefix + id + idSuffix;
        }
    }

    public static EnumSet<FieldID> toEnumSet(FieldID... _excludes) {
        return ((_excludes == null) || (_excludes.length == 0)) ? null : EnumSet.copyOf(Arrays.asList(_excludes));
    }

    protected static void add(StringBuilder result, boolean includeEmpty,
                              final EnumSet<FieldID> excludes, FieldID item,
                              ILabelGenerator labeler, Object value, boolean isEmpty) {
        add(result, includeEmpty, excludes, item, labeler.get(item), value, isEmpty);
    }

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
        lastModified,
        // sort
    }

    public static String convertLL(double latLon) {
        if (Double.isNaN(latLon)) return "";
        return DirectoryFormatter.formatLatLon(latLon);
    }

}
