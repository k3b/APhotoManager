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

package de.k3b.transactionlog;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import de.k3b.io.DateUtil;
import de.k3b.io.DirectoryFormatter;
import de.k3b.media.IMetaApi;
import de.k3b.media.MediaUtil;
import de.k3b.tagDB.TagConverter;
import de.k3b.tagDB.TagProcessor;

/**
 * Android independant base class to writes change infos into log (bat-file and database).
 *
 * Created by k3b on 08.07.2017.
 */
public abstract class TransactionLoggerBase  {
    protected long id;
    protected String path;
    protected final long now;

    public TransactionLoggerBase(long now) {
        this.now = now;
    }
    public TransactionLoggerBase set(long id, String path) {
        this.id = id;
        this.path = path;
        return this;
    }

    public void addChanges(IMetaApi newData, EnumSet<MediaUtil.FieldID> changes, List<String> oldTags) {
        if (changes.contains(MediaUtil.FieldID.dateTimeTaken))  addChangesDateTaken(newData.getDateTimeTaken());
        if (changes.contains(MediaUtil.FieldID.latitude)) addChanges(MediaTransactionLogEntryType.GPS, DirectoryFormatter.formatLatLon(newData.getLatitude()) + " " + DirectoryFormatter.formatLatLon(newData.getLongitude()), false);
        if (changes.contains(MediaUtil.FieldID.description))  addChanges(MediaTransactionLogEntryType.DESCRIPTION, newData.getDescription(), true);
        if (changes.contains(MediaUtil.FieldID.title))  addChanges(MediaTransactionLogEntryType.HEADER, newData.getTitle(), true);
        if (changes.contains(MediaUtil.FieldID.rating)) addChanges(MediaTransactionLogEntryType.RATING, (newData.getRating() != null) ? newData.getRating().toString(): "0", false);
        if (changes.contains(MediaUtil.FieldID.tags)) addChangesTags(oldTags, newData.getTags());
    }

    protected void addChangesDateTaken(Date newData) {
        addChanges(MediaTransactionLogEntryType.DATE, DateUtil.toIsoDateString(newData), false);
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
    }

    /** todo implement android specific logging here */
    abstract protected void addChanges(MediaTransactionLogEntryType command, String parameter, boolean quoteParam);
}
