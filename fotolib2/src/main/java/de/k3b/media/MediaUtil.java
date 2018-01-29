/*
 * Copyright (c) 2016-2018 by k3b.
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
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import de.k3b.io.DateUtil;
import de.k3b.io.FileUtils;
import de.k3b.io.GeoUtil;
import de.k3b.io.StringUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.tagDB.TagConverter;

/**
 * Created by k3b on 10.10.2016.
 */

public class MediaUtil {

    /** image will get this fileextension if updated from private image to public image. */
    private static final String EXT_JPG = ".jpg";

    /** image will get this fileextension if updated from public image to private image.
     * since other gallery-apps/image-pickers do not know this extension, they will not show images
     * with this extension. */
    public static final String EXT_JPG_PRIVATE = ".jpg-p";

    /** types of images currently supported */
    public static final int IMG_TYPE_ALL        = 0xffff;
    public static final int IMG_TYPE_JPG        = 0x0001; // jp(e)g
    public static final int IMG_TYPE_NON_JPG    = 0x0010; // png, gif, ...
    public static final int IMG_TYPE_PRIVATE    = 0x1000; // jpg-p

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
        visibility,
    };

    /** translates FieldID to text. In android this is implemented via resource id  */
    public interface ILabelGenerator {
        String get(FieldID id);
    }

    private static final ILabelGenerator defaultLabeler = new ILabelGenerator() {
        @Override
        public String get(FieldID id) {
            return " " + id + " ";
        }
    };

    public static String toString(IMetaApi item) {
        return toString(item, true, null, (EnumSet<FieldID>) null);
    }

    public static String toString(IMetaApi item, boolean includeEmpty, ILabelGenerator labeler, FieldID... _excludes) {
        return toString(item, includeEmpty, labeler, toEnumSet(_excludes));
    }

    public static String toString(IMetaApi item, boolean includeEmpty, ILabelGenerator _labeler, EnumSet<FieldID> excludes) {
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
        return result.toString();
    }

    public static EnumSet<FieldID> toEnumSet(FieldID... _excludes) {
        return ((_excludes == null) || (_excludes.length == 0)) ? null : EnumSet.copyOf(Arrays.asList(_excludes));
    }

    private static void add(StringBuilder result, boolean includeEmpty,
                            final EnumSet<FieldID> excludes, FieldID item,
                            ILabelGenerator labeler, Object value) {
        add(result, includeEmpty, excludes, item, labeler.get(item), value);
    }

    private static void add(StringBuilder result, boolean includeEmpty,
                            final EnumSet<FieldID> excludes, FieldID item, String name, Object value) {
        if (name != null) {
            if ((includeEmpty) || (value != null)) {
                if ((excludes == null) || (!excludes.contains(item))) {
                    result.append(name).append(value);
                }
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
    public static List<FieldID>  copySpecificProperties(IMetaApi destination, IMetaApi source,
                                    boolean overwriteExisting, final EnumSet<FieldID> fields2copy) {
        List<FieldID> collectedChanges = new ArrayList<FieldID>();

        copyImpl(destination, source, false, overwriteExisting, overwriteExisting, fields2copy, collectedChanges, (FieldID[]) null);
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
     * @param destination where fields are copied to or null (for collectedChanges calculation)
     * @param source where data is copied from.
     * @param _simulateDoNotCopy
     * @param overwriteExisting false: write only if destinatin field is null before.
     * @param allowSetNull if true for all fields setNull is possible.
 *                     Else only those containted in _allowSetNulls is allowed to set null.
     * @param fields2copy null: all fields will be copied. else only the fields contained are copied.
     * @param collectedChanges if not null: do not copy but collect the FieldID-s that
*                         would be copied in this list.
     * @param _allowSetNulls if not null: for these fields setNull is allowed      @return number of changed fields
     */
    private static int copyImpl(IMetaApi destination, IMetaApi source,
                                boolean _simulateDoNotCopy, boolean overwriteExisting, boolean allowSetNull,
                                final EnumSet<FieldID> fields2copy,
                                List<FieldID> collectedChanges,
                                FieldID... _allowSetNulls) {
        int changes = 0;

        if (source != null) {
            boolean simulateDoNotCopy = (destination == null) ? true : _simulateDoNotCopy;
            final EnumSet<FieldID>  allowSetNulls = toEnumSet(_allowSetNulls);
            String sValue;

            sValue = source.getPath();
            if (allowed(sValue, (destination == null) ? null : destination.getPath()
                    , fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls
                    , FieldID.path, collectedChanges)) {
                destination.setPath(sValue);
                changes++;
            }

            Date dValue = source.getDateTimeTaken();
            if (allowed(dValue, (destination == null) ? null : destination.getDateTimeTaken()
                    , fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls
                    , FieldID.dateTimeTaken, collectedChanges)) {
                destination.setDateTimeTaken(dValue);
                changes++;
            }

            Double latitude = source.getLatitude();
            Double longitude = source.getLongitude();
            if (allowed(latitude, (destination == null) ? null : destination.getLatitude()
                    , fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls
                    , FieldID.latitude_longitude, collectedChanges) ||
                allowed(longitude, (destination == null) ? null : destination.getLongitude()
                        , fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls
                        , FieldID.latitude_longitude, collectedChanges)) {
                setLatitudeLongitude(destination, latitude, longitude);
                changes++;
            }

            sValue = source.getTitle();
            if (allowed(sValue, (destination == null) ? null : destination.getTitle()
                    , fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls
                    , FieldID.title, collectedChanges)) {
                destination.setTitle(sValue);
                changes++;
            }

            sValue = source.getDescription();
            if (allowed(sValue, (destination == null) ? null : destination.getDescription()
                    , fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls
                    , FieldID.description, collectedChanges)) {
                destination.setDescription(sValue);
                changes++;
            }

            // tags are also used for visibility
            List<String> tValueNewTags = source.getTags();

            VISIBILITY vValue = source.getVisibility();
            final VISIBILITY oldVisibility = (destination == null) ? null : destination.getVisibility();

            if (VISIBILITY.isChangingValue(vValue) &&
                allowed(vValue, oldVisibility
                    , fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls
                    , FieldID.visibility, collectedChanges)) {
                destination.setVisibility(vValue);
                changes++;
                List<String> tValueModifiedNewTags = VISIBILITY.setPrivate(tValueNewTags, vValue);
                if (tValueModifiedNewTags != null) tValueNewTags = tValueModifiedNewTags;
            }

            if (allowed(tValueNewTags, (destination == null) ? null : destination.getTags()
                    , fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls
                    , FieldID.tags, collectedChanges)) {
                destination.setTags(tValueNewTags);
                changes++;
            }

            Integer iValue = source.getRating();
            if (allowed(iValue, (destination == null) ? null : destination.getRating()
                    , fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls
                    , FieldID.rating, collectedChanges)) {
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

    private static boolean allowed(Object newValue, Object oldValue,
                                   EnumSet<FieldID> fields2copy,
                                   boolean simulateDoNotCopy, boolean overwriteExisting, boolean allowSetNull,
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
                return DateUtil.toIsoDateTimeString(data.getDateTimeTaken());
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
    public static boolean isImage(String path, int imageTypeFlags) {
        if (path == null) return false;
        String lcPath = path.toLowerCase();

        if ((IMG_TYPE_JPG == (imageTypeFlags & IMG_TYPE_JPG)) &&
            (lcPath.endsWith(EXT_JPG) || lcPath.endsWith(".jpeg"))) {
            return true;
        }

        if ((IMG_TYPE_NON_JPG == (imageTypeFlags & IMG_TYPE_NON_JPG)) &&
                (lcPath.endsWith(".gif") || lcPath.endsWith(".png") || lcPath.endsWith(".tiff") || lcPath.endsWith(".bmp"))) {
            return true;
        }

        if ((IMG_TYPE_PRIVATE == (imageTypeFlags & IMG_TYPE_PRIVATE)) &&
                (lcPath.endsWith(EXT_JPG_PRIVATE))) {
            return true;
        }

        return false;
    }

    /** returns the full path that item should get or null if path is already ok */
    public static String getModifiedPath(IMetaApi item) {
        if (item != null) {
            return getModifiedPath(item.getPath(), item.getVisibility());
        }
        return null;
    }

    /** unittest friedly version: returns the full path that item should get or null if path is already ok */
    static String getModifiedPath(String currentPath, VISIBILITY visibility) {
        if ((visibility != null) && (currentPath != null)) {
            switch (visibility) {
                case PRIVATE:
                    if (isImage(currentPath, IMG_TYPE_JPG))
                        return FileUtils.replaceExtension(currentPath, EXT_JPG_PRIVATE);
                    break;
                case PUBLIC:
                    if (isImage(currentPath, IMG_TYPE_PRIVATE))
                        return FileUtils.replaceExtension(currentPath, EXT_JPG);
                    break;
            }
        }
        return null;
    }

    public static final FilenameFilter JPG_FILENAME_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return MediaUtil.isImage(filename, MediaUtil.IMG_TYPE_ALL);
        }
    };

}