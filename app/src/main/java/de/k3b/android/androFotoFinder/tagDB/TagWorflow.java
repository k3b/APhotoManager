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
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.media.MediaContentValues;
import de.k3b.android.util.AndroidFileCommands;
import de.k3b.media.ExifInterfaceEx;
import de.k3b.android.util.MediaScannerEx;
import de.k3b.database.SelectedFiles;
import de.k3b.io.FileUtils;
import de.k3b.io.ListUtils;
import de.k3b.media.MediaUtil;
import de.k3b.media.MediaXmpSegment;
import de.k3b.tagDB.Tag;
import de.k3b.tagDB.TagConverter;
import de.k3b.tagDB.TagProcessor;
import de.k3b.tagDB.TagRepository;
import de.k3b.transactionlog.MediaTransactionLogEntryType;

/**
 *  Class to handle tag update for one or more photos.
 *
 * Created by k3b on 09.01.2017.
 */

public class TagWorflow extends TagProcessor {
    private List<TagSql.TagWorflowItem> items = null;
    private Activity context;

    /** Get current assigned tags from selectedItemPks and/or anyOfTags
     * @param context
     * @param selectedItems if not null list of comma seperated item-pks
     * @param anyOfTags if not null list of tag-s where at least one oft the tag must be in the photo.
     */
    public TagWorflow init(Activity context, SelectedFiles selectedItems, List<Tag> anyOfTags) {
        this.context = context;
        this.items = loadTagWorflowItems(context, (selectedItems == null) ? null : selectedItems.toIdString(), anyOfTags);
        for (TagSql.TagWorflowItem item : items) {
            List<String> tags = item.tags;
            File xmpFile = FileUtils.getXmpFile(item.path);
            if (xmpFile.exists() && (item.xmpLastModifiedDate < xmpFile.lastModified())){ // || (tags == null) || (tags.size() == 0)) {
                // xmp has been updated since last db update.
                tags = loadTags(xmpFile);
                item.xmpMoreRecentThanSql = true;
            }
            registerExistingTags(tags);
        }

        //!!!todo if file based .noMediaFolder
        return this;
    }

    /** execute the updates for all affected files in the Workflow. */
    public int updateTags(List<String> addedTags, List<String> removedTags) {
        int itemCount = 0;
        if (items != null) {
            int progressCountDown = 0;
            int total = items.size();
            for (TagSql.TagWorflowItem item : items) {
                File xmpFile = updateTags(item, addedTags, removedTags);
                itemCount++;
                progressCountDown--;
                if (progressCountDown < 0) {
                    progressCountDown = 10;
                    onProgress(itemCount, total, xmpFile.toString());
                }
            } // for each image
        }

        return itemCount;
    }

    /** update one file if tags change or xmp does not exist yet: xmp-sidecar-file, media-db and batch */
    @NonNull
    protected File updateTags(TagSql.TagWorflowItem tagWorflowItemFromDB, List<String> addedTags, List<String> removedTags) {
        boolean mustSave = tagWorflowItemFromDB.xmpMoreRecentThanSql;
        String dbgSaveReason = (mustSave) ? "xmpMoreRecentThanSql." : "";
        MediaXmpSegment xmp = null;

        List<String> currentItemTags = tagWorflowItemFromDB.tags;
        File xmpFile = FileUtils.getXmpFile(tagWorflowItemFromDB.path);

        if (xmpFile.exists()) {
            xmp = new MediaScannerEx(context).loadXmp(null, xmpFile);
            if (xmp != null) {
                List<String> currentXmpTags = xmp.getTags();// current tags is all db-tags + xmp-tags or null if no changes

                List<String> xmpTagsFromDbAndXmpModified = this.getUpdated(currentItemTags, currentXmpTags, null);
                if (xmpTagsFromDbAndXmpModified != null) {
                    mustSave = true;
                    dbgSaveReason += "xmp has more tags than sql.";
                    currentItemTags = xmpTagsFromDbAndXmpModified;
                } else if (ListUtils.toString(currentItemTags).compareTo(ListUtils.toString(currentXmpTags)) != 0) {
                    dbgSaveReason += "sql has more tags than xmp.";
                    mustSave = true;
                }
            }
        } // else xmp-file does not exist yet.

        // apply tag-modifications to currentItemTags or null if no changes
        List<String> modifiedTags = this.getUpdated(currentItemTags, addedTags, removedTags);
        if (modifiedTags != null) {
            // tags have changed.
            currentItemTags = modifiedTags;
            mustSave = true;
            dbgSaveReason += "tags modified.";
        }

        if (mustSave) {
            if (xmp == null) {
        // xmp does not exist yet: add original content from exif to xmp

                xmp = new MediaXmpSegment();
                xmp.setOriginalFileName(new File(tagWorflowItemFromDB.path).getName());

                ExifInterfaceEx exif = null;
                try {
                    exif = new ExifInterfaceEx(tagWorflowItemFromDB.path, null, null, "TagWorflow.updateTags " + dbgSaveReason);
                    MediaUtil.copy(xmp, exif, false, false);
                } catch (IOException ex) {
                    // exif is null
                }
            }

            // apply tag changes to xmp
            xmp.setTags(currentItemTags);

            String dbgMsgPrefix = "TagWorflow.saveXmp(" + xmpFile + "): " + dbgSaveReason;
            // update xmp-sidecar-file
            try {
                xmp.save(xmpFile, Global.saveXmpAsHumanReadable, dbgMsgPrefix);
            } catch (IOException e) {
                Log.e(Global.LOG_CONTEXT,dbgMsgPrefix + " error : " + e.getMessage(),e);
            }

        // update tag repository
            TagRepository.getInstance().include(TagRepository.getInstance().getImportRoot(), currentItemTags);

        // update media database
            ContentValues dbValues = new ContentValues();
            MediaContentValues mediaContentValues = new MediaContentValues().set(dbValues, null);

            // #77: does only copy non-null values
            MediaUtil.copyXmp(mediaContentValues, xmp,false, true);

            // #77: fix make shure that tags might be set to null
            mediaContentValues.setTags(currentItemTags);

            // #77: make shure that db-date is newer than xmp-file-date
            TagSql.setXmpFileModifyDate(dbValues, new Date(xmpFile.lastModified() +1));


            TagSql.execUpdate("updateTags " + dbgSaveReason, this.context, tagWorflowItemFromDB.id, dbValues);

        // update batch
            long now = new Date().getTime();
            String tagsString = TagConverter.asBatString(removedTags);
            AndroidFileCommands cmd = AndroidFileCommands.createFileCommand(context);
            if (tagsString != null) {
                cmd.log("call apmTagsRemove.cmd \"", tagWorflowItemFromDB.path, "\" ", tagsString);
                cmd.addTransactionLog(tagWorflowItemFromDB.id, tagWorflowItemFromDB.path, now,
                        MediaTransactionLogEntryType.TAGSREMOVE, tagsString);
            }

            tagsString = TagConverter.asBatString(addedTags);
            if (tagsString != null) {
                cmd.log("call apmTagsAdd.cmd \"", tagWorflowItemFromDB.path, "\" ", tagsString);
                cmd.addTransactionLog(tagWorflowItemFromDB.id, tagWorflowItemFromDB.path, now,
                        MediaTransactionLogEntryType.TAGSADD, tagsString);
            }

            cmd.addTransactionLog(tagWorflowItemFromDB.id, tagWorflowItemFromDB.path, now,
                    MediaTransactionLogEntryType.TAGS, TagConverter.asBatString(xmp.getTags()));

            cmd.closeLogFile();
        }

        return xmpFile;
    }

    /** periodically called while work in progress. can be overwritten to supply feedback to user */
    protected void onProgress(int itemCount, int total, String message) {
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
                xmp.load(xmpFile, "loadXmp(" + xmpFile + ")");
                return xmp;
            } catch (IOException e) {
                Log.e(Global.LOG_CONTEXT,"error: loadXmp(" + xmpFile +  ") : " + e.getMessage(),e);
            }
        }
        return null;
    }

    /** same as {@link TagSql#loadTagWorflowItems(Context, String, List)} but can be overwritten for unittests. */
    protected List<TagSql.TagWorflowItem> loadTagWorflowItems(Context context, String selectedItems, List<Tag> anyOfTags) {
        return TagSql.loadTagWorflowItems(context, selectedItems, anyOfTags);
    }
}
