/*
 * Copyright (c) 2015-2017 by k3b.
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

package de.k3b.android.util;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Date;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.media.MediaContentValues;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.io.FileUtils;
import de.k3b.media.MediaUtil;
import de.k3b.media.MediaXmpSegment;
import de.k3b.tagDB.Tag;
import de.k3b.tagDB.TagRepository;

/**
 * Extensions to Android Media Scanner that are not supported by original scanner.
 *
 * Created by k3b on 04.10.2016.
 */

public class MediaScannerEx extends MediaScanner {
    private Tag mImportRoot = null;

    public MediaScannerEx(Context context) {
        super(context);
    }

    @Override
    protected void getExifFromFile(ContentValues values, File file) {
        super.getExifFromFile(values, file);

        // for first tests generate test data
        if (false && Global.Media.enableNonStandardMediaFields) {
            addTags(values, null, "test1", "test2");
            TagSql.setDescription(values, null, "test");
            TagSql.setRating(values, null, 3);
        }
    }

    /**
     * Loads xmp content from xmp-file
     *
     * @param mediaContentValuesToReceiveLastUpdated   if not null: xmp-content-lastModified is
     *                                                 updated from xmpFileSource.lastModified
     * @param xmpFileSource i.e. "/path/to/file.xmp"
     * @return              the loaded segment or null if not found or error
     */
    public MediaXmpSegment loadXmp(MediaContentValues mediaContentValuesToReceiveLastUpdated, File xmpFileSource) {
        MediaXmpSegment xmp = null;
        if ((xmpFileSource != null) && xmpFileSource.exists() && xmpFileSource.isFile()) {
            xmp = new MediaXmpSegment();
            try {
                getLastUpdated(mediaContentValuesToReceiveLastUpdated, xmpFileSource);
                xmp.load(new FileInputStream(xmpFileSource));
                TagRepository.getInstance().include(getImportRoot(), xmp.getTags());
            } catch (FileNotFoundException e) {
                Log.e(Global.LOG_CONTEXT, "MediaScannerEx:loadXmp(xmpFileSource=" + xmpFileSource +") failed " + e.getMessage(),e);
                xmp = null;
            }
        }
        return xmp;
    }

    public static void getLastUpdated(MediaContentValues mediaContentValuesToReceiveLastUpdated, File xmpFileSource) {
        if (mediaContentValuesToReceiveLastUpdated != null) {
            TagSql.setXmpFileModifyDate(mediaContentValuesToReceiveLastUpdated.getContentValues(), xmpFileSource.lastModified());
        }
    }

    /**
     * updates values with current values of file.
     * Override: also get xmp data (i.e. Tags)
     */
    @Override
    protected int getExifValues(MediaContentValues dest, File file, ExifInterfaceEx exif) {
        int changes = 0;
        long xmpFileModifyDate = TagSql.EXT_LAST_EXT_SCAN_UNKNOWN;

        File xmpFile = FileUtils.getXmpFile(file.getAbsolutePath());
        MediaXmpSegment xmp = loadXmp(dest, xmpFile);

        if (xmp != null) {
            xmpFile = null;
        } else if (Global.Media.enableXmpNone) {
            xmpFileModifyDate = TagSql.EXT_LAST_EXT_SCAN_NO_XMP;
        }

        if (Global.Media.xmpOverwritesExif) {
            // xmp overwrites exif so execute first exif then xmp
            changes += super.getExifValues(dest, file, exif);
            changes += MediaUtil.copy(dest, xmp, false, true);
        } else {
            // exif overwrites xmp so execute first xmp then exif
            changes += MediaUtil.copy(dest, xmp, false, true);
            changes += super.getExifValues(dest, file, exif);
        }

        if (xmpFileModifyDate != TagSql.EXT_LAST_EXT_SCAN_UNKNOWN) {
            TagSql.setXmpFileModifyDate(dest.getContentValues(), xmpFileModifyDate);
        }
        return changes;
    }

    /**
     * Override: make shure that TagDB is saved
     */
    @Override
    public int updateMediaDatabase_Android42(Context context, String[] oldPathNames, String... newPathNames) {
        int result = super.updateMediaDatabase_Android42(context, oldPathNames, newPathNames);
        if ((result > 0) && (Global.Media.enableNonStandardMediaFields)) {
            TagRepository.getInstance().save();
        }
        return result;
    }

    private int addTags(ContentValues values,  Date xmpFileModifyDate, String... tags) {
        TagSql.setTags(values, xmpFileModifyDate, tags);
        return TagRepository.getInstance().includeChildTags(getImportRoot(), Tag.toList(tags));
    }

    /** get or create parent-tag where alle imports are appendend as children */
    public Tag getImportRoot() {
        if (mImportRoot == null) {
            mImportRoot = TagRepository.getInstance().getImportRoot();
        }
        return mImportRoot;
    }
}