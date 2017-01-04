/*
 * Copyright (c) 2017 by k3b.
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
package de.k3b.android.androFotoFinder.tagDB;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.media.MediaContentValues;
import de.k3b.android.util.AndroidFileCommands;
import de.k3b.android.util.ExifInterfaceEx;
import de.k3b.android.util.MediaScannerEx;
import de.k3b.database.SelectedFiles;
import de.k3b.io.FileUtils;
import de.k3b.media.MediaUtil;
import de.k3b.media.MediaXmpSegment;
import de.k3b.tagDB.TagConverter;
import de.k3b.tagDB.TagProcessor;

/**
 * Created by k3b on 09.01.2017.
 */

public class TagWorflow extends TagProcessor {
    private List<TagSql.TagWorflowItem> items = null;
    private Activity context;

    /** Get current assigned tags. */
    public TagWorflow init(Activity context, SelectedFiles selectedItems) {
        this.context = context;
        if (selectedItems != null) {
            this.items = loadTagWorflowItems(context, selectedItems.toIdString());
            for (TagSql.TagWorflowItem item : items) {
                List<String> tags = item.tags;
                File xmpFile = FileUtils.getXmpFile(item.path);
                if (xmpFile.exists() && (item.xmpLastModifiedDate < xmpFile.lastModified()) || (tags == null) || (tags.size() == 0)) {
                    tags = loadTags(xmpFile);
                }
                registerExistingTags(tags);
            }
        }
        //!!!todo if file based .noMediaFolder
        return this;
    }

    /** execute the updates. */
    public void updateTags(List<String> addedTags, List<String> removedTags) {
        for (TagSql.TagWorflowItem item : items) {
            ContentValues dbValues = null;  // != null: must save
            MediaXmpSegment xmp = null;     // != null: must save

            List<String> currentItemTags = item.tags;
            File xmpFile = FileUtils.getXmpFile(item.path);

            // special case that xmp has never been scanned before or xmp was updated without updateing db
            if (xmpFile.exists() && (item.xmpLastModifiedDate < xmpFile.lastModified()) || (currentItemTags == null) || (currentItemTags.size() == 0)) {
                dbValues = new ContentValues();
                MediaContentValues mediaContentValues = new MediaContentValues().set(dbValues, null);
                xmp = new MediaScannerEx(context).loadXmp(mediaContentValues, xmpFile);
                if (xmp != null) {
                    List<String> xmpTags = getUpdated(currentItemTags, xmp.getTags(), null);

                    if (xmpTags != null) {
                        currentItemTags = xmpTags; // additional tags from xmp that where not in db before
                    }
                }
            }

            // end preprocessig if xmp or db-xmp-data did not exit before.
            List<String> newTags = getUpdated(currentItemTags, addedTags, removedTags);


            if (newTags != null) {
                // tags have been modified: update xmp and db
                if (xmp == null) xmp = loadXmp(xmpFile);
                if (xmp == null) {
                    // xmp does not exist yet: add original content from exif

                    xmp = new MediaXmpSegment();

                    xmp.setOriginalFileName(new File(item.path).getName());

                    ExifInterfaceEx exif = null;
                    try {
                        exif = new ExifInterfaceEx(item.path);
                        MediaUtil.copy(xmp,exif,false,false);
                    } catch (IOException ex) {
                        // exif is null
                    }
                }
                xmp.setTags(newTags);

                if (dbValues == null) dbValues = new ContentValues();
                TagSql.setTags(dbValues, null, newTags.toArray(new String[newTags.size()]));

            }

            if (xmp != null) {
                try {
                    xmp.save(new FileOutputStream(xmpFile), Global.saveXmpAsHumanReadable);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (dbValues != null) {
                TagSql.setXmpFileModifyDate(dbValues, xmpFile.lastModified());
                TagSql.execUpdate(this.context, item.id, dbValues);
            }

            // update batch
            String tagsString = TagConverter.asBatString(removedTags);
            if (tagsString != null){
                AndroidFileCommands.log(context, "call apmTagsRemove.cmd \"", item.path, "\" ", tagsString).closeLogFile();
            }

            tagsString = TagConverter.asBatString(addedTags);
            if (tagsString != null){
                AndroidFileCommands.log(context, "call apmTagsAdd.cmd \"", item.path, "\" ", tagsString).closeLogFile();
            }
        } // for each image
    }

    private List<String> loadTags(File xmpFile) {
        MediaXmpSegment xmp = loadXmp(xmpFile);
        return (xmp == null) ? null : xmp.getTags();
    }

    @NonNull
    private MediaXmpSegment loadXmp(File xmpFile) {
        if ((xmpFile != null) && (xmpFile.exists())) {
            try {
                MediaXmpSegment xmp = new MediaXmpSegment();
                xmp.load(new FileInputStream(xmpFile));
                return xmp;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /** same as {@link TagSql#loadTagWorflowItems(Context, String)} but can be overwritten for unittests. */
    private List<TagSql.TagWorflowItem> loadTagWorflowItems(Context context, String selectedItems) {
        return TagSql.loadTagWorflowItems(context, selectedItems);
    }
}
