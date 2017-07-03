/*
 * Copyright (c) 2016-2017 by k3b.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import de.k3b.io.DateUtil;
import de.k3b.io.GeoUtil;
import de.k3b.io.StringUtils;
import de.k3b.tagDB.TagConverter;

/**
 * Created by k3b on 10.10.2016.
 */

public class MediaUtil {
    /**
     * used to identify a member of IMetaApi
     */
    public static enum FieldID {
        path,
        dateTimeTaken,
        title,
        description,
        latitude,
        longitude,
        rating,
        tags,
        clasz,
    };

    public static String toString(IMetaApi item) {
        return toString(item, true);
    }

    public static String toString(IMetaApi item, boolean includeEmpty, FieldID... _excludes) {
        if (item == null) return "";
        final EnumSet<FieldID> excludes = toEnumSet(_excludes);
        StringBuilder result = new StringBuilder();
        add(result, includeEmpty, excludes, FieldID.clasz, item.getClass().getSimpleName(), ":");
        add(result, includeEmpty, excludes, FieldID.path, " path ", item.getPath());
        add(result, includeEmpty, excludes, FieldID.dateTimeTaken, " dateTimeTaken ", DateUtil.toIsoDateString(item.getDateTimeTaken()));
        add(result, includeEmpty, excludes, FieldID.title, " title ", item.getTitle());
        add(result, includeEmpty, excludes, FieldID.description, " description ", item.getDescription());
        add(result, includeEmpty, excludes, FieldID.latitude, " latitude ", GeoUtil.toCsvStringLatLon(item.getLatitude()));
        add(result, includeEmpty, excludes, FieldID.longitude, " longitude ", GeoUtil.toCsvStringLatLon(item.getLongitude()));
        add(result, includeEmpty, excludes, FieldID.rating, " rating ", item.getRating());
        add(result, includeEmpty, excludes, FieldID.tags, " tags ", TagConverter.asDbString(null, item.getTags()));
        return result.toString();
    }

    public static EnumSet<FieldID> toEnumSet(FieldID... _excludes) {
        return ((_excludes == null) || (_excludes.length == 0)) ? null : EnumSet.copyOf(Arrays.asList(_excludes));
    }

    private static void add(StringBuilder result, boolean includeEmpty,
                            final EnumSet<FieldID> excludes, FieldID item, String name, Object value) {
        if ((includeEmpty) || (value != null)) {
            if ((excludes == null) || (!excludes.contains(item))) {
                result.append(name).append(value);
            }
        }
    }

    /** copy content from source to destination. @return number of copied properties */
    public static int copy(IMetaApi destination, IMetaApi source,
                           boolean overwriteExisting, boolean allowSetNull) {
        return copyImpl(destination, source, overwriteExisting, allowSetNull, null, null, (FieldID[]) null);
    }

    /**
     * Copies non null properties of source to destination.
     *
     * @param _allowSetNulls     if one of these columns are null, the set null is copied, too
     * @return number of copied properties
     */
    public static int copyNonEmpty(IMetaApi destination, IMetaApi source, FieldID... _allowSetNulls) {
        return copyImpl(destination, source, true, false, null, null, _allowSetNulls);
    }

    /**
     * Copies properties of source to destination when included in fields2copy.
     *
     * @return number of copied properties
     */
    public static int copySpecificProperties(IMetaApi destination, IMetaApi source, final EnumSet<FieldID> fields2copy) {
        return copyImpl(destination, source, true, true, fields2copy, null, (FieldID[]) null);
    }

    public static List<FieldID> getChanges(IMetaApi destination, IMetaApi source) {
        List<FieldID> collectedChanges = new ArrayList<FieldID>();
        if (0 == copyImpl(destination, source, true, true, null, collectedChanges, (FieldID[]) null)) {
            return null;
        }
        return collectedChanges;
    }

    /**
     * copy content from source to destination. @return number of copied properties
     *
     * @param destination where fields are copied to.
     * @param source where data is copied from.
     * @param overwriteExisting false: write only if destinatin field is null before.
     * @param allowSetNull if true for all fields setNull is possible.
     *                     Else only those containted in _allowSetNulls is allowed to set null.
     * @param fields2copy null: all fields will be copied. else only the fields contained are copied.
     * @param collectedChanges if not null: do not copy but collect the FieldID-s that
     *                         would be copied in this list.
     * @param _allowSetNulls if not null: for these fields setNull is allowed
     * @return number of changed fields
     */
    private static int copyImpl(IMetaApi destination, IMetaApi source,
                                boolean overwriteExisting, boolean allowSetNull,
                                final EnumSet<FieldID> fields2copy,
                                List<FieldID> collectedChanges,
                                FieldID... _allowSetNulls) {
        int changes = 0;

        if ((destination != null) && (source != null)) {
            final EnumSet<FieldID>  allowSetNulls = toEnumSet(_allowSetNulls);
            String sValue;

            sValue = source.getPath();
            if (allowed(sValue, destination.getPath(), fields2copy, overwriteExisting, allowSetNull, allowSetNulls, FieldID.path, collectedChanges)) {
                destination.setPath(sValue);
                changes++;
            }

            Date dValue = source.getDateTimeTaken();
            if (allowed(dValue, destination.getDateTimeTaken(), fields2copy, overwriteExisting, allowSetNull, allowSetNulls, FieldID.dateTimeTaken, collectedChanges)) {
                destination.setDateTimeTaken(dValue);
                changes++;
            }

            Double doValue = source.getLatitude();
            if (allowed(doValue, destination.getLatitude(), fields2copy, overwriteExisting, allowSetNull, allowSetNulls, FieldID.latitude, collectedChanges)) {
                destination.setLatitude(doValue);
                changes++;
            }

            doValue = source.getLongitude();
            if (allowed(doValue, destination.getLongitude(), fields2copy, overwriteExisting, allowSetNull, allowSetNulls, FieldID.longitude, collectedChanges)) {
                destination.setLongitude(doValue);
                changes++;
            }
            sValue = source.getTitle();
            if (allowed(sValue, destination.getTitle(), fields2copy, overwriteExisting, allowSetNull, allowSetNulls, FieldID.title, collectedChanges)) {
                destination.setTitle(sValue);
                changes++;
            }

            sValue = source.getDescription();
            if (allowed(sValue, destination.getDescription(), fields2copy, overwriteExisting, allowSetNull, allowSetNulls, FieldID.description, collectedChanges)) {
                destination.setDescription(sValue);
                changes++;
            }

            List<String> tValue = source.getTags();
            if (allowed(tValue, destination.getTags(), fields2copy, overwriteExisting, allowSetNull, allowSetNulls, FieldID.tags, collectedChanges)) {
                destination.setTags(tValue);
                changes++;
            }

            Integer iValue = source.getRating();
            if (allowed(iValue, destination.getRating(), fields2copy, overwriteExisting, allowSetNull, allowSetNulls, FieldID.rating, collectedChanges)) {
                destination.setRating(iValue);
                changes++;
            }

        }

        if (collectedChanges != null) return collectedChanges.size();
        return changes;
    }

    private static boolean allowed(Object newValue, Object oldValue, EnumSet<FieldID> fields2copy, boolean overwriteExisting, boolean allowSetNull,
                                   final EnumSet<FieldID> allowSetNulls, FieldID item, List<FieldID> collectedChanges) {
        if ((fields2copy != null) && (!fields2copy.contains(item))) return false;    // not in fields2copy

        if ((!overwriteExisting) && (oldValue != null)) return false;   // already has an old value

        boolean sameValue = StringUtils.equals(newValue, oldValue);
        if ((allowSetNulls != null) && (allowSetNulls.contains(item))) return true;  // null doesn-t matter

        boolean result = ((newValue != null) || allowSetNull) ;
        if (collectedChanges != null) {
            if (result && !sameValue) {
                collectedChanges.add(item);
            }
            return false;
        }
        return result;
    }

    /*
    public static String get(IMetaApi data, FieldID field) {
        switch (field) {
            case path:
                return data.getPath();
            case dateTimeTaken:
                return DateUtil.toIsoDateString(data.getDateTimeTaken());
            case title:
                return data.getTitle();
            case description:
                return data.getDescription();
            case latitude:
                return GeoUtil.toCsvStringLatLon(data.getLatitude());
            case longitude:
                return GeoUtil.toCsvStringLatLon(data.getLongitude());
            case rating:
                Integer rating = data.getRating();
                return (rating == null) ? "0" : rating.toString();
            case tags:
                return TagConverter.asDbString(null, data.getTags());
            case clasz:
                return data.getClass().getSimpleName();
            default:
                return null;
        }
    }
    */

    /** return true if path is "*.jp(e)g" */
    public static boolean isImage(File path, boolean jpgOnly) {
        if (path == null) return false;
        return isImage(path.getName(), jpgOnly);
    }

    /** return true if path is "*.jp(e)g" */
    public static boolean isImage(String path, boolean jpgOnly) {
        if (path == null) return false;
        String lcPath = path.toLowerCase();

        if ((!jpgOnly) && (lcPath.endsWith(".gif") || lcPath.endsWith(".png") || lcPath.endsWith(".tiff") || lcPath.endsWith(".bmp"))) {
            return true;
        }
        return lcPath.endsWith(".jpg") || lcPath.endsWith(".jpeg");
    }
}