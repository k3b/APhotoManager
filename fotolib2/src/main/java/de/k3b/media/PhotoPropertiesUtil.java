/*
 * Copyright (c) 2016-2019 by k3b.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import de.k3b.LibGlobal;
import de.k3b.io.FileUtils;
import de.k3b.io.GeoUtil;
import de.k3b.io.ListUtils;
import de.k3b.io.StringUtils;
import de.k3b.io.VISIBILITY;

import de.k3b.media.MediaFormatter.FieldID;
/**
 * Created by k3b on 10.10.2016.
 */

public class PhotoPropertiesUtil {
    private static final Logger logger = LoggerFactory.getLogger(LibGlobal.LOG_TAG);

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


    // Translate exif-orientation code (0..8) to EXIF_ORIENTATION_CODE_2_ROTATION_DEGREES (clockwise)
    // http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/EXIF.html
    private static final short[] EXIF_ORIENTATION_CODE_2_ROTATION_DEGREES = {
            0,     // EXIF Orientation constants:
            0,     // 1 = Horizontal (normal)
            0,     // 2 = (!) Mirror horizontal
            180,   // 3 = Rotate 180
            180,   // 4 = (!) Mirror vertical
            90,    // 5 = (!) Mirror horizontal and rotate 270 CW
            90,    // 6 = Rotate 90 CW
            270,   // 7 = (!) Mirror horizontal and rotate 90 CW
            270};  // 8 = Rotate 270 CW

    /** copy content from source to destination. @return number of copied properties */
    public static int copy(IPhotoProperties destination, IPhotoProperties source,
                           boolean overwriteExisting, boolean allowSetNull) {
        return copyImpl(destination, source, false, overwriteExisting, allowSetNull, null, null, (FieldID[]) null);
    }

    /**
     * Copies non null properties of source to destination.
     *
     * @param _allowSetNulls     if one of these columns are null, the set null is copied, too
     * @return number of copied properties
     */
    public static int copyNonEmpty(IPhotoProperties destination, IPhotoProperties source, FieldID... _allowSetNulls) {
        return copyImpl(destination, source, false, true, false, null, null, _allowSetNulls);
    }

    /**
     * Copies properties of source to destination when included in fields2copy.
     *
     * @return possible empy list of FieldID-s of modified properties
     */
    public static List<FieldID>  copySpecificProperties(IPhotoProperties destination, IPhotoProperties source,
                                                                       boolean overwriteExisting, final EnumSet<FieldID> fields2copy) {
        List<FieldID> collectedChanges = new ArrayList<FieldID>();

        copyImpl(destination, source, false, overwriteExisting, overwriteExisting, fields2copy, collectedChanges, (FieldID[]) null);
        return collectedChanges;
    }

    public static List<FieldID> getChanges(IPhotoProperties destination, IPhotoProperties source) {
        List<FieldID> collectedChanges = new ArrayList<FieldID>();
        if (0 == copyImpl(destination, source, true, true, true, null, collectedChanges, (FieldID[]) null)) {
            return null;
        }
        return collectedChanges;
    }

    public static EnumSet<FieldID> getChangesAsDiffsetOrNull(IPhotoProperties destination, IPhotoProperties source) {
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
    private static int copyImpl(IPhotoProperties destination, IPhotoProperties source,
                                boolean _simulateDoNotCopy, boolean overwriteExisting, boolean allowSetNull,
                                final EnumSet<FieldID> fields2copy,
                                List<FieldID> collectedChanges,
                                FieldID... _allowSetNulls) {
        int changes = 0;

        if (source != null) {
            boolean simulateDoNotCopy = (destination == null) || _simulateDoNotCopy;
            final EnumSet<FieldID>  allowSetNulls = MediaFormatter.toEnumSet(_allowSetNulls);
            String sValue;

            sValue = source.getPath();
            if (allowedObject(sValue, (destination == null) ? null : destination.getPath()
                    , fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls
                    , FieldID.path, collectedChanges)) {
                destination.setPath(sValue);
                changes++;
            }

            Date dValue = source.getDateTimeTaken();
            if (allowedObject(dValue, (destination == null) ? null : destination.getDateTimeTaken()
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
            if (allowedObject(sValue, (destination == null) ? null : destination.getTitle()
                    , fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls
                    , FieldID.title, collectedChanges)) {
                destination.setTitle(sValue);
                changes++;
            }

            sValue = source.getDescription();
            if (allowedObject(sValue, (destination == null) ? null : destination.getDescription()
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
                allowedObject(vValue, oldVisibility
                    , fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls
                    , FieldID.visibility, collectedChanges)) {
                destination.setVisibility(vValue);
                changes++;
                List<String> tValueModifiedNewTags = VISIBILITY.setPrivate(tValueNewTags, vValue);
                if (tValueModifiedNewTags != null) tValueNewTags = tValueModifiedNewTags;
            }

            if (allowedObject(tValueNewTags, (destination == null) ? null : destination.getTags()
                    , fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls
                    , FieldID.tags, collectedChanges)) {
                destination.setTags(tValueNewTags);
                changes++;
            }

            Integer iValue = source.getRating();
            if (allowedObject(iValue, (destination == null) ? null : destination.getRating()
                    , fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull, allowSetNulls
                    , FieldID.rating, collectedChanges)) {
                destination.setRating(iValue);
                changes++;
            }
        }

        if (collectedChanges != null) return collectedChanges.size();
        return changes;
    }

    public static void setLatitudeLongitude(IPhotoProperties destination, Double _latitude, Double _longitude) {
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
        return allowedObject(newValue, oldValue, fields2copy, simulateDoNotCopy, overwriteExisting, allowSetNull,
                                            allowSetNulls, item, collectedChanges);
    }

    private static boolean allowedObject(Object newValue, Object oldValue,
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
    public static String get(IPhotoProperties data, FieldID field) {
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

        return (IMG_TYPE_PRIVATE == (imageTypeFlags & IMG_TYPE_PRIVATE)) &&
                (lcPath.endsWith(EXT_JPG_PRIVATE));

    }

    /** returns the full path that item should get or null if path is already ok */
    public static String getModifiedPath(IPhotoProperties item) {
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
            return PhotoPropertiesUtil.isImage(filename, PhotoPropertiesUtil.IMG_TYPE_ALL);
        }
    };

    /** @param exifOrientationCode either code 0..8 or rotation angle 0, 90, 180, 270 */
    public static int exifOrientationCode2RotationDegrees(int exifOrientationCode, int notFoundValue) {
        if ((exifOrientationCode >= 0) && (exifOrientationCode < EXIF_ORIENTATION_CODE_2_ROTATION_DEGREES.length)) {
            return EXIF_ORIENTATION_CODE_2_ROTATION_DEGREES[exifOrientationCode];
        }
        return notFoundValue;
    }

    /** loads IPhotoProperties from jpg and corresponding xmp */
    public static IPhotoProperties loadExifAndXmp(String fileName, String dbg_context) {
        try {
            PhotoPropertiesXmpSegment xmp = PhotoPropertiesXmpSegment.loadXmpSidecarContentOrNull(fileName, dbg_context);

            PhotoPropertiesImageReader jpg = new PhotoPropertiesImageReader().load(fileName, null, xmp, dbg_context);

            return jpg;
        } catch (IOException ex) {
            logger.error(dbg_context, "PhotoPropertiesUtil.loadExifAndXmp", ex);
        }
        return null;
    }

    /** #132: reads lat,lon,description,tags from files until tags and lat/long are found */
    public static <T extends IPhotoProperties> T inferAutoprocessingExifDefaults(T result, File... files) {
        return inferAutoprocessingExifDefaults(result, files, null,null,null, null);
    }

    /** #132: reads lat,lon,description,tags from files until tags and lat/long are found */
    public static <T extends IPhotoProperties> T inferAutoprocessingExifDefaults(T result,
                                                                                 File[] files,
                                                                                 Double latitude,
                                                                                 Double longitude,
                                                                                 String description,
                                                                                 List<String> _tags) {
        List<String> tags = (_tags == null) ? new ArrayList<String>() : _tags;

        int exampleCount = files.length;
        int index = 0;
        while ((index < exampleCount) && ((latitude == null) || (longitude == null)  || (tags.size() == 0))) {
            File exampleFile = files[index++];

            if ((exampleFile != null) && (exampleFile.exists())) {
                IPhotoProperties example = PhotoPropertiesUtil.loadExifAndXmp(exampleFile.getPath(),"infer defaults for autoprocessing");
                if (example != null) {
                    ListUtils.include(tags, example.getTags());
                    latitude = GeoUtil.getValue(example.getLatitude(), latitude);
                    longitude = GeoUtil.getValue(example.getLongitude(), longitude);
                    if (StringUtils.isNullOrEmpty(description)) description = example.getDescription();
                }
            }
        }
        result.setLatitudeLongitude(latitude,longitude)
                .setDescription(description)
                .setTags(tags);

        return result;
    }


}