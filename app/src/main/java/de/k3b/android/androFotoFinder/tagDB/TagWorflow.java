/*
 * Copyright (c) 2017-2020 by k3b.
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
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.IMediaRepositoryApi;
import de.k3b.android.io.AndroidFileCommands;
import de.k3b.io.IProgessListener;
import de.k3b.io.XmpFile;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;
import de.k3b.media.MediaFormatter;
import de.k3b.media.PhotoPropertiesUpdateHandler;
import de.k3b.media.PhotoPropertiesXmpSegment;
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

public class TagWorflow extends TagProcessor implements IProgessListener {
    private List<TagSql.TagWorflowItem> items = null;
    private Activity context;

    /** Get current assigned tags from selectedItemPks
     * and/or any image that has one or more tag of anyOfTags.
     * @param context
     * @param selectedItems if not null list of comma seperated item-pks
     * @param anyOfTags if not null list of tag-s where at least one oft the tag must be in the photo.
     */
    public TagWorflow init(Activity context, SelectedFiles selectedItems, List<Tag> anyOfTags) {
        this.context = context;
        this.items = loadTagWorflowItems(context, (selectedItems == null) ? null : selectedItems.toIdString(), anyOfTags);
        for (TagSql.TagWorflowItem item : items) {
            List<String> tags = item.tags;
            IFile xmpFile = XmpFile.getExistingSidecarOrNull(item.path);
            if ((xmpFile != null) && xmpFile.exists() && (item.xmpLastModifiedDate < xmpFile.lastModified())){ // || (tags == null) || (tags.size() == 0)) {
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
        final IMediaRepositoryApi mediaDBApi = FotoSql.getMediaDBApi();
        try {
            mediaDBApi.beginTransaction(); // Performance boost: all db-inserts/updates in one transaction
            int itemCount = 0;
            if (items != null) {
                int progressCountDown = 0;
                int total = items.size();
                for (TagSql.TagWorflowItem item : items) {
                    itemCount += updateTags(item, addedTags, removedTags);
                    progressCountDown--;
                    if (progressCountDown < 0) {
                        progressCountDown = 10;
                        if (!onProgress(itemCount, total, item.path)) break;
                    }
                } // for each image
            }
            mediaDBApi.setTransactionSuccessful();
            return itemCount;
        } finally {
            mediaDBApi.endTransaction();
        }
    }

    /** update one file if tags change or xmp does not exist yet: xmp-sidecar-file, media-db and batch */
    protected int updateTags(TagSql.TagWorflowItem tagWorflowItemFromDB, List<String> addedTags, List<String> removedTags) {
        int result = 0;
        boolean mustSave = tagWorflowItemFromDB.xmpMoreRecentThanSql;
        String dbgSaveReason = (mustSave) ? "xmpMoreRecentThanSql." : "";

        List<String> currentItemTags = tagWorflowItemFromDB.tags;
        try {
            PhotoPropertiesUpdateHandler exif = PhotoPropertiesUpdateHandler.create(
                    FileFacade.convert("TagWorflow updateTags from db", tagWorflowItemFromDB.path),
                    null, false, "updateTags:");
            List<String> tagsDbPlusFile = getUpdated(currentItemTags, exif.getTags(), null);
            if (tagsDbPlusFile != null) {
                mustSave = true;
                dbgSaveReason += "jpg/xmp has more tags than sql.";
                currentItemTags = tagsDbPlusFile;
            }

            List<String> modifiedTags = getUpdated(currentItemTags, addedTags, removedTags);
            if (modifiedTags != null) {
                // tags have changed.
                currentItemTags = modifiedTags;
                mustSave = true;
                dbgSaveReason += "tags modified.";
            }

            dbgSaveReason = "TagWorflow.updateTags(" + tagWorflowItemFromDB.path + "): " + dbgSaveReason;

            if (mustSave) {
                exif.setTags(currentItemTags);
                exif.save(dbgSaveReason);
                TagSql.updateDB(dbgSaveReason, tagWorflowItemFromDB.path, exif, MediaFormatter.FieldID.tags);

                // update tag repository
                TagRepository.getInstance().includeTagNamesIfNotFound(currentItemTags);
                result = 1;
            }

            // update batch
            long now = new Date().getTime();
            String tagsString = TagConverter.asBatString(removedTags);
            AndroidFileCommands cmd = AndroidFileCommands.createFileCommand(context, false);
            if (tagsString != null) {
                cmd.addTransactionLog(tagWorflowItemFromDB.id, tagWorflowItemFromDB.path, now,
                        MediaTransactionLogEntryType.TAGSREMOVE, tagsString);
            }

            tagsString = TagConverter.asBatString(addedTags);
            if (tagsString != null) {
                cmd.addTransactionLog(tagWorflowItemFromDB.id, tagWorflowItemFromDB.path, now,
                        MediaTransactionLogEntryType.TAGSADD, tagsString);
            }

            cmd.addTransactionLog(tagWorflowItemFromDB.id, tagWorflowItemFromDB.path, now,
                    MediaTransactionLogEntryType.TAGS, TagConverter.asBatString(exif.getTags()));

            cmd.closeLogFile();

        } catch (IOException e) {
            Log.e(Global.LOG_CONTEXT,dbgSaveReason + " error : " + e.getMessage(),e);
        }
        return result;
    }

    /** periodically called while work in progress. can be overwritten to supply feedback to user */
    public boolean onProgress(int itemCount, int total, String message) {
        return true;
    }

    private List<String> loadTags(IFile xmpFile) {
        PhotoPropertiesXmpSegment xmp = loadXmp(xmpFile);
        return (xmp == null) ? null : xmp.getTags();
    }

    @NonNull
    private PhotoPropertiesXmpSegment loadXmp(IFile xmpFile) {
        if ((xmpFile != null) && (xmpFile.exists())) {
            try {
                PhotoPropertiesXmpSegment xmp = new PhotoPropertiesXmpSegment();
                xmp.load(xmpFile.openInputStream(), "loadXmp(" + xmpFile + ")");
                return xmp;
            } catch (IOException e) {
                Log.e(Global.LOG_CONTEXT,"error: loadXmp(" + xmpFile +  ") : " + e.getMessage(),e);
            }
        }
        return null;
    }

    /**
     * same as {@link TagSql#loadTagWorflowItems(String, List)} but can be overwritten for unittests.
     */
    protected List<TagSql.TagWorflowItem> loadTagWorflowItems(Context context, String selectedItems, List<Tag> anyOfTags) {
        return TagSql.loadTagWorflowItems(selectedItems, anyOfTags);
    }
}
