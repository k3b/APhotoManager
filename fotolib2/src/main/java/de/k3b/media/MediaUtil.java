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
        latitude_longitude,
        rating,
        tags,
        clasz,
    };

    public static String toString(IMetaApi item) {
        return toString(item, true);
    }

    public static String toString(IMetaApi item, boolean includeEmpty, FieldID... _excludes) {
        return toString(item, includeEmpty, toEnumSet(_excludes));
    }

    public static String toString(IMetaApi item, boolean includeEmpty, EnumSet<FieldID> excludes) {
        if (item == null) return "";
        StringBuilder result = new StringBuilder();
        add(result, includeEmpty, excludes, FieldID.clasz, item.getClass().getSimpleName(), ":");
        add(result, includeEmpty, excludes, FieldID.path, " path ", item.getPath());
        add(result, includeEmpty, excludes, FieldID.dateTimeTaken, " dateTimeTaken ", DateUtil.toIsoDateString(item.getDateTimeTaken()));
        add(result, includeEmpty, excludes, FieldID.title, " title ", item.getTitle());
        add(result, includeEmpty, excludes, FieldID.description, " description ", item.getDescription());
        add(result, includeEmpty, excludes, FieldID.latitude_longitude, " latitude ", GeoUtil.toCsvStringLatLon(item.getLatitude()));
        add(result, includeEmpty, excludes, FieldID.latitude_longitude, " longitude ", GeoUtil.toCsvStringLatLon(item.getLongitude()));
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
        return copyImpl(destination, source, false, overwriteExisting, allowSetNull, null, null, (FieldID[]) null);
    }

    /**
     * Copies non null properties of source to destination.
     *
     * @param _allowSetNulls     if one of these columns are null, the set null is copied, too
     * @return number of copied properties
     */
    public static int copyNonEmpty(IMetaApi destination, IMetaApi source, FieldID... _allowSetNulls) {
        return copyImpl(destination, source, false, true, false, null, null, _allowSetNulls);
    }

    /**
     * Copies properties of source to destination when included in fields2copy.
     *
     * @return possible empy list of FieldID-s of modified properties
     */
    public static List<FieldID>  copySpecificProperties(IMetaApi destination, IMetaApi source, final EnumSet<FieldID> fields2copy) {
        List<FieldID> collectedChanges = new ArrayList<FieldID>();
        copyImpl(destination, source, false, true, true, fields2copy, collectedChanges, (FieldID[]) null);
        return collectedChanges;
    }

    public static List<FieldID> getChanges(IMetaApi destination, IMetaApi source) {
        List<FieldID> collectedChanges = new ArrayList<FieldID>();
        if (0 == copyImpl(destination, source, true, true, true, null, collectedChanges, (FieldID[]) null)) {
            return null;
        }
        return collectedChanges;
    }

    public static EnumSet<FieldID> getChangesAsDiffsetOrNull(IMetaApi destination, IMetaApi source) {
        List<FieldID> differences = getChanges(destination, source);
        return (differences != null) ? EnumSet.copyOf(differences) : null;
    }

    /**
     * copy content from source to destination. @return number of copied properties
     *
     * @param destination where fields are copied to.
     * @param source where data is copied from.
     * @param simulateDoNotCopy
     *@param overwriteExisting false: write only if destinatin field is null before.
     * @param allowSetNull if true for all fields setNull is possible.
 *                     Else only those containted in _allowSetNulls is allowed to set null.
     * @param fields2copy null: all fields will be copied. else only the fields contained are copied.
     * @param collectedChanges if not null: do not copy but collect the FieldID-s that
*                         would be copied in this list.
     * @param _allowSetNulls if not null: for these fields setNull is allowed      @return number of changed fields
     */
    private static int copyImpl(IMetaApi destination, IMetaApi source,
                                boolean simulateDoNotCopy, boolean overwriteExisting, boolean allowSetNull,
                                final EnumSet<FieldID> fields2copy,
                                List<FieldID> collectedChanges,
                                FieldID... _allowSetNulls) {
        int changes = 0;

        if ((destination != null) && (source != null)) {
            final EnumSet<FieldID>  allowSetNulls = toEnumSet(_allowSetNulls);
            String sValue;

            sValue = source.getPath();
            if (allowed(sValue, destination.getPath(), fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls, FieldID.path, collectedChanges)) {
                destination.setPath(sValue);
                changes++;
            }

            Date dValue = source.getDateTimeTaken();
            if (allowed(dValue, destination.getDateTimeTaken(), fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls, FieldID.dateTimeTaken, collectedChanges)) {
                destination.setDateTimeTaken(dValue);
                changes++;
            }

            Double latitude = source.getLatitude();
            Double longitude = source.getLongitude();
            if (allowed(latitude, destination.getLatitude(), fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls, FieldID.latitude_longitude, collectedChanges) ||
                allowed(longitude, destination.getLongitude(), fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls, FieldID.latitude_longitude, collectedChanges)) {
                setLatitudeLongitude(destination, latitude, longitude);
                changes++;
            }

            sValue = source.getTitle();
            if (allowed(sValue, destination.getTitle(), fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls, FieldID.title, collectedChanges)) {
                destination.setTitle(sValue);
                changes++;
            }

            sValue = source.getDescription();
            if (allowed(sValue, destination.getDescription(), fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls, FieldID.description, collectedChanges)) {
                destination.setDescription(sValue);
                changes++;
            }

            List<String> tValue = source.getTags();
            if (allowed(tValue, destination.getTags(), fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls, FieldID.tags, collectedChanges)) {
                destination.setTags(tValue);
                changes++;
            }

            Integer iValue = source.getRating();
            if (allowed(iValue, destination.getRating(), fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls, FieldID.rating, collectedChanges)) {
                destination.setRating(iValue);
                changes++;
            }

        }

        if (collectedChanges != null) return collectedChanges.size();
        return changes;
    }

    public static void setLatitudeLongitude(IMetaApi destination, Double _latitude, Double _longitude) {
        if (destination != null) {
            Double latitude     = GeoUtil.getValue(_latitude);
            Double longitude    = GeoUtil.getValue(_longitude);
            if (GeoUtil.NO_LAT_LON.equals(latitude) && GeoUtil.NO_LAT_LON.equals(longitude)) {
                destination.setLatitudeLongitude(null, null); // (0,0) ==> no-geo
            } else {
                destination.setLatitudeLongitude(latitude, longitude);
            }
        }
    }

    /** #91: Fix Photo without geo may have different representations values for no-value */
    private static boolean allowed(Double newValue, Double oldValue, EnumSet<FieldID> fields2copy, boolean simulateDoNotCopy, boolean overwriteExisting, boolean allowSetNull,
                                   final EnumSet<FieldID> allowSetNulls, FieldID item, List<FieldID> collectedChanges) {
        if (GeoUtil.equals(newValue, oldValue))  return false;  // both are the same, no need to write again
        return allowed((Object) newValue, (Object) oldValue, fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull,
                                            allowSetNulls, item, collectedChanges);
    }

    private static boolean allowed(Object newValue, Object oldValue, EnumSet<FieldID> fields2copy, boolean simulateDoNotCopy, boolean overwriteExisting, boolean allowSetNull,
                                   final EnumSet<FieldID> allowSetNulls, FieldID item, List<FieldID> collectedChanges) {
        // in simulate mode return false as success; in non-simulate return true
        boolean success = !simulateDoNotCopy;

        if (StringUtils.equals(newValue, oldValue)) return false; // both are the same, no need to write again

        if ((fields2copy != null) && (!fields2copy.contains(item))) return false;    // not in fields2copy

        if ((!overwriteExisting) && (oldValue != null)) return false;   // already has an old value

        boolean allowSetNull2 = allowSetNull || ((allowSetNulls != null) && (allowSetNulls.contains(item)));
        if (!allowSetNull2 && (newValue == null)) return false; // null not allowed

        if (collectedChanges != null) collectedChanges.add(item);
        return success; // overwrite existing value with null
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
            case latitude_longitude:
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