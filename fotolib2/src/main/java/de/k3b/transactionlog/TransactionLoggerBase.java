/*
 * Copyright (c) 2017-2018 by k3b.
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

package de.k3b.transactionlog;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import de.k3b.FotoLibGlobal;
import de.k3b.io.DateUtil;
import de.k3b.io.DirectoryFormatter;
import de.k3b.io.FileProcessor;
import de.k3b.io.ListUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.media.IMetaApi;
import de.k3b.media.MediaUtil;
import de.k3b.tagDB.TagConverter;
import de.k3b.tagDB.TagProcessor;

/**
 * Android independant base class to writes change infos into log (bat-file).
 *
 * Created by k3b on 08.07.2017.
 */
public class TransactionLoggerBase implements Closeable {
    private FileProcessor execLog;

    // true if this/super created the logger.
    protected boolean mustCloseLog = false;

    protected long id;
    protected String path;
    protected final long now;

    public TransactionLoggerBase(FileProcessor execLog, long now) {
        this.execLog = execLog;
        this.now = now;
    }
    public TransactionLoggerBase set(long id, String path) {
        this.id = id;
        this.path = path;
        return this;
    }

    @Override
    public void close() throws IOException {
        if (mustCloseLog) {
            execLog.closeAll();
        }
        execLog = null;
    }
    public void addChanges(IMetaApi newData, EnumSet<MediaUtil.FieldID> changes, List<String> oldTags) {
        addComment("apply changes image#",id);

        if (changes.contains(MediaUtil.FieldID.dateTimeTaken))  addChangesDateTaken(newData.getDateTimeTaken());
        if (changes.contains(MediaUtil.FieldID.latitude_longitude)) addChanges(MediaTransactionLogEntryType.GPS, DirectoryFormatter.formatLatLon(newData.getLatitude()) + " " + DirectoryFormatter.formatLatLon(newData.getLongitude()), false);
        if (changes.contains(MediaUtil.FieldID.description))  addChanges(MediaTransactionLogEntryType.DESCRIPTION, newData.getDescription(), true);
        if (changes.contains(MediaUtil.FieldID.title))  addChanges(MediaTransactionLogEntryType.HEADER, newData.getTitle(), true);
        if (changes.contains(MediaUtil.FieldID.rating)) addChanges(MediaTransactionLogEntryType.RATING, (newData.getRating() != null) ? newData.getRating().toString(): "0", false);

        if (changes.contains(MediaUtil.FieldID.tags)) addChangesTags(oldTags, newData.getTags());

        final VISIBILITY visibility = newData.getVisibility();
        if (changes.contains(MediaUtil.FieldID.visibility) && VISIBILITY.isChangingValue(visibility)) {
            addChanges(MediaTransactionLogEntryType.VISIBILITY, ((VISIBILITY.PRIVATE.equals(visibility)) ? "1": "0") + " " + visibility, false);
        }

    }

    protected void addChangesDateTaken(Date newData) {
        addChanges(MediaTransactionLogEntryType.DATE, DateUtil.toIsoDateTimeString(newData), false);
    }

    protected void addChangesTags(List<String> oldTags, List<String> newTags) {
        List<String> addedTags = new ArrayList<String>();
        List<String> removedTags = new ArrayList<String>();
        TagProcessor.getDiff(oldTags, newTags, addedTags, removedTags);
        if (addedTags.size() > 0) {
            addChanges(MediaTransactionLogEntryType.TAGSADD, TagConverter.asBatString(addedTags), false);
        }
        if (removedTags.size() > 0) {
            addChanges(MediaTransactionLogEntryType.TAGSREMOVE, TagConverter.asBatString(removedTags), false);
        }
        if ((newTags != null) && (newTags.size() > 0)) {
            addChanges(MediaTransactionLogEntryType.TAGS, TagConverter.asBatString(newTags), false);
        }
    }

    public void addChangesCopyMove(boolean move, String newFullPath, String debugContext) {
        if (this.path.compareToIgnoreCase(newFullPath) != 0) {
            if (!move) {
                addComment(debugContext, "copy image #", this.id);
            }

            addChanges(move ? MediaTransactionLogEntryType.MOVE : MediaTransactionLogEntryType.COPY,
                    newFullPath, true);
            if (move) {
                String oldPath = this.path;
                // id remains the same but path has changed
                set(this.id, newFullPath);
                addComment(debugContext, "image #", this.id, " was renamed from ", oldPath);
            }
        }
    }

    public void addComment(Object... comment) {
        if (FotoLibGlobal.debugEnabledJpgMetaIo) {
            addChanges(MediaTransactionLogEntryType.COMMENT, ListUtils.toString(" ", comment), true);
        }
    }

    /** android specific logging is implemented in AndroidTransactionLogger in Override */
    protected void addChanges(MediaTransactionLogEntryType command, String parameter, boolean quoteParam) {
        execLog.log(command.getCommand(path,parameter));
    }
}
