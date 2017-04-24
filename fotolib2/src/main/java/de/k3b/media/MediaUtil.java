/*
 * Copyright (c) 2016 by k3b.
 *
 * This file is part of AndroFotoFinder.
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
import java.util.Date;
import java.util.List;

import de.k3b.io.DateUtil;
import de.k3b.io.GeoUtil;
import de.k3b.tagDB.TagConverter;

/**
 * Created by k3b on 10.10.2016.
 */

public class MediaUtil {

    public static String toString(IMetaApi item) {
        if (item == null) return "";
        return item.getClass().getSimpleName() + ":" +
                " path " + item.getPath() +
                " dateTimeTaken " + DateUtil.toIsoDateString(item.getDateTimeTaken()) +
                " title " + item.getTitle() +
                " description " + item.getDescription() +
                " latitude " + GeoUtil.toCsvStringLatLon(item.getLatitude()) +
                " longitude " + GeoUtil.toCsvStringLatLon(item.getLongitude()) +
                " tags " + TagConverter.asDbString(null, item.getTags());
    }

    /** copy content from source to destination. @return number of copied properties */
    public static int copy(IMetaApi destination, IMetaApi source, boolean allowSetNull, boolean overwriteExisting) {
        return copyExif(destination, source, allowSetNull, overwriteExisting) + copyXmp(destination, source, allowSetNull, overwriteExisting);
    }

    /** copy exif-prio content from source to destination. @return number of copied properties */
    private static int copyExif(IMetaApi destination, IMetaApi source, boolean allowSetNull, boolean overwriteExisting) {
        int changes = 0;

        if ((destination != null) && (source != null)) {
            String sValue;

            sValue = source.getPath();
            if (allowed(allowSetNull, sValue, overwriteExisting, destination.getPath())) {
                destination.setPath(sValue);
                changes++;
            }

            Date dValue = source.getDateTimeTaken();
            if (allowed(allowSetNull, dValue, overwriteExisting, destination.getDateTimeTaken())) {
                destination.setDateTimeTaken(dValue);
                changes++;
            }

            Double doValue = source.getLatitude();
            if (allowed(allowSetNull, doValue, overwriteExisting, destination.getLatitude())) {
                destination.setLatitude(doValue);
                changes++;
            }

            doValue = source.getLongitude();
            if (allowed(allowSetNull, doValue, overwriteExisting, destination.getLongitude())) {
                destination.setLongitude(doValue);
                changes++;
            }

        }
        return changes;
    }

    /** copy xmp-priority content from source to destination. @return number of copied properties */
    public static int copyXmp(IMetaApi destination, IMetaApi source, boolean allowSetNull, boolean overwriteExisting) {
        int changes = 0;
        if ((destination != null) && (source != null)) {

            String sValue;// xmp-only attributes
            sValue = source.getTitle();
            if (allowed(allowSetNull, sValue, overwriteExisting, destination.getTitle())) {
                destination.setTitle(sValue);
                changes++;
            }

            sValue = source.getDescription();
            if (allowed(allowSetNull, sValue, overwriteExisting, destination.getDescription())) {
                destination.setDescription(sValue);
                changes++;
            }

            List<String> tValue = source.getTags();
            if (allowed(allowSetNull, tValue, overwriteExisting, destination.getTags())) {
                destination.setTags(tValue);
                changes++;
            }

            Integer iValue = source.getRating();
            if (allowed(allowSetNull, iValue, overwriteExisting, destination.getDateTimeTaken())) {
                destination.setRating(iValue);
                changes++;
            }
        }
        return changes;
    }

    /**
     * Calculate the number of properties that are different between destination and other.
     * Used to skip "save" if there are no changes.
     *
     * @param setIdenticalPropsToNull If true set all properties of destination to null
     *                                that are identical to the properties of "other".
     **/
    public static int countChangedProperties(IMetaApi destination, IMetaApi other,
                                             boolean setIdenticalPropsToNull) {
        int differentCount = 0;
        if (isSameOrNull(other.getDescription(), destination.getDescription())) {
            if (setIdenticalPropsToNull) destination.setDescription(null);
        } else {
            differentCount++;
        }

        if (isSameOrNull(other.getPath(), destination.getPath())) {
            if (setIdenticalPropsToNull) destination.setPath(null);
        } else {
            differentCount++;
        }

        if (isSameOrNull(other.getTitle(), destination.getTitle())) {
            if (setIdenticalPropsToNull) destination.setTitle(null);
        } else {
            differentCount++;
        }

        if (isSameOrNull(other.getDateTimeTaken(), destination.getDateTimeTaken())) {
            if (setIdenticalPropsToNull) destination.setDateTimeTaken(null);
        } else {
            differentCount++;
        }

        if (isSameOrNull(other.getLatitude(), destination.getLatitude())) {
            if (setIdenticalPropsToNull) destination.setLatitude(null);
        } else {
            differentCount++;
        }

        if (isSameOrNull(other.getLongitude(), destination.getLongitude())) {
            if (setIdenticalPropsToNull) destination.setLongitude(null);
        } else {
            differentCount++;
        }

        List<String> tValue = other.getTags();
        if (isSameOrNull(TagConverter.asDbString(null, other.getTags()), TagConverter.asDbString(null, destination.getTags()))) {
            if (setIdenticalPropsToNull) destination.setTags(tValue);
        } else {
            differentCount++;
        }
        return differentCount;
    }

    private static boolean isSameOrNull(Object me, Object other) {
        if (me == null) return true;
        if (other != null) {
            return me.equals(other);
        }
        return false;
    }

    private static boolean allowed(boolean allowSetNull, Object newValue,
                                   boolean overwriteExisting, Object oldValue) {
        if ((!overwriteExisting) && (oldValue != null)) return false;
        return ((newValue != null) || allowSetNull);
    }

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