/*
 * Copyright (c) 2017-2021 by k3b.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import de.k3b.LibGlobal;
import de.k3b.media.IPhotoProperties;
import de.k3b.media.PhotoPropertiesUtil;

public enum VISIBILITY {
    /**
     * take from current settings
     */
    DEFAULT(0),
    /**
     * private only
     */
    PRIVATE(1),
    /**
     * public only
     */
    PUBLIC(2),

    /**
     * Used as Filter: private and public images but not other files like album-files
     */
    PRIVATE_PUBLIC(3),

    /* Hidden Photo (inside directoryname starting with "." or Directory miwth ".nomedia" file */
    HIDDEN(4);

    // #100: if photo has this tag it has visibility PRIVATE
    public static final String TAG_PRIVATE = "PRIVATE";


    // causes "SLF4J: Class path contains multiple SLF4J bindings." in unittests :-(
    // private static final Logger logger = LoggerFactory.getLogger(LibGlobal.LOG_TAG);

    public static final VISIBILITY MAX = HIDDEN;
    public final int value;

    private VISIBILITY(int value) {
        this.value = value;
    }

    private static VISIBILITY[] values = null;
    public static VISIBILITY fromInt(int i) {
        if(VISIBILITY.values == null) {
            VISIBILITY.values = VISIBILITY.values();
        }
        for (VISIBILITY elem : values) {
            if (elem.value == i) return elem;
        }
        return null;
    }
    public static VISIBILITY fromString(String value) {
        if ((value != null) && (value.length() > 0)) {
            String lower = value.toLowerCase();
            try {
                int i = Integer.parseInt(value, 10);
                return fromInt(i);
            } catch (Exception ex) {
                if (lower.startsWith("private_p")) return PRIVATE_PUBLIC;
                if (lower.startsWith("pr")) return PRIVATE;
                if (lower.startsWith("pu")) return PUBLIC;
                if (lower.startsWith("hi")) return HIDDEN;
                switch (lower.charAt(0)) {
                    case 'd' :
                        return DEFAULT;
                    case 'f' : // false
                        return PRIVATE;
                    case 't' : // true
                        return PUBLIC;
                    default:
                        break;
                }

            }

            // workar9oud to avoid "SLF4J: Class path contains multiple SLF4J bindings." in unittests :-(
            Logger logger = LoggerFactory.getLogger(LibGlobal.LOG_TAG);
            logger.warn(VISIBILITY.class.getSimpleName() + ".fromString " + value + ": unknown value");
        }
        return null;
    }

    public static boolean isChangingValue(VISIBILITY value) {
        return ((value == PRIVATE) || (value == PUBLIC) || (value == HIDDEN));
    }

    public static boolean hasPrivate(List<String> tags) {
        int existing = (tags == null) ? -1 : tags.indexOf(TAG_PRIVATE);
        return (existing >= 0);
    }

    /** infers visibility from tags */
    public static VISIBILITY getVisibility(List<String> tags) {
        return hasPrivate(tags) ? PRIVATE : PUBLIC;
    }

    /** infers visibility from path */
    public static VISIBILITY getVisibility(String filePath) {
        if (filePath == null) return null;
        return PhotoPropertiesUtil.isPrivateImage(filePath) ? PRIVATE : PUBLIC;
    }

    /** infers visibility from tags and path */
    public static VISIBILITY getVisibility(IPhotoProperties photoProperties) {
        VISIBILITY result = null;
        if (photoProperties != null) {
            List<String> tags = photoProperties.getTags();
            if (tags != null) {
                result = VISIBILITY.getVisibility(tags);
            }

            if (result == null) {
                String path = photoProperties.getPath();
                if (path != null) {
                    result = VISIBILITY.getVisibility(path);
                }
            }
        }
        return result;
    }

    public static List<String> setPrivate(List<String> tags, VISIBILITY visibility) {
        boolean hasPrivate = VISIBILITY.hasPrivate(tags);
        if (visibility == VISIBILITY.PRIVATE) {
            if (!hasPrivate) {
                tags = (tags == null) ? new ArrayList<String>() : new ArrayList<String>(tags);
                tags.add(VISIBILITY.TAG_PRIVATE);
                return tags;
            }
        } else { // PUBLIC
            if (hasPrivate) {
                // now it is public so remove PRIVATE tag
                tags = new ArrayList<String>(tags);
                tags.remove(VISIBILITY.TAG_PRIVATE);
                return tags;
            }
        }
        return null;
    }
}
