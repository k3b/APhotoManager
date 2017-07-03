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

package de.k3b.android.androFotoFinder;

import android.app.Activity;
import android.content.Context;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import de.k3b.android.util.AndroidFileCommands;
import de.k3b.io.DateUtil;
import de.k3b.io.DirectoryFormatter;
import de.k3b.media.MediaAsString;
import de.k3b.media.MediaUtil;
import de.k3b.media.MediaUtil.FieldID;
import de.k3b.tagDB.TagConverter;
import de.k3b.tagDB.TagProcessor;
import de.k3b.transactionlog.MediaTransactionLogEntryType;

/**
 * Created by k3b on 02.07.2017.
 */

public class TransactionLogger implements Closeable {
    private Context ctx;
    private final long id;
    private final String path;
    private final long now;
    private AndroidFileCommands execLog;
    boolean mustCloseLog;

    public TransactionLogger(Activity ctx, long id, String path, long now, AndroidFileCommands execLog) {
        this.ctx = ctx;
        this.id = id;
        this.path = path;
        this.now = now;

        mustCloseLog = (execLog == null);
        this.execLog = mustCloseLog ? AndroidFileCommands.createFileCommand(ctx) : execLog;

    }

    public void addChanges(MediaAsString newData, EnumSet<MediaUtil.FieldID> changes, List<String> oldTags) {
        if (changes.contains(FieldID.dateTimeTaken))  addChangesDateTaken(newData.getDateTimeTaken());
        if (changes.contains(FieldID.latitude))  addChanges(MediaTransactionLogEntryType.GPS, DirectoryFormatter.parseLatLon(newData.getLatitude()) + " " + DirectoryFormatter.parseLatLon(newData.getLongitude()));
        if (changes.contains(FieldID.description))  addChanges(MediaTransactionLogEntryType.DESCRIPTION, newData.getDescription());
        if (changes.contains(FieldID.title))  addChanges(MediaTransactionLogEntryType.HEADER, newData.getTitle());
        if (changes.contains(FieldID.rating)) addChanges(MediaTransactionLogEntryType.RATING, (newData.getRating() != null) ? newData.getRating().toString(): "0");
        if (changes.contains(FieldID.tags)) addChangesTags(oldTags, newData.getTags());
    }

    protected void addChangesDateTaken(Date newData) {
        addChanges(MediaTransactionLogEntryType.DATE, DateUtil.toIsoDateString(newData));
    }

    protected void addChangesTags(List<String> oldTags, List<String> newTags) {
        List<String> addedTags = new ArrayList<String>();
        List<String> removedTags = new ArrayList<String>();
        TagProcessor.getDiff(oldTags, newTags, addedTags, removedTags);
        if (addedTags.size() > 0) {
            addChanges(MediaTransactionLogEntryType.TAGSADD, TagConverter.asBatString(addedTags));
        }
        if (removedTags.size() > 0) {
            addChanges(MediaTransactionLogEntryType.TAGSREMOVE, TagConverter.asBatString(removedTags));
        }
    }

    private void addChanges(MediaTransactionLogEntryType command, String parameter) {
        execLog.log(command.getCommand(path,parameter));
        execLog.addTransactionLog(id, path, now, command, parameter);
    }

    @Override
    public void close() throws IOException {
        if (mustCloseLog) {
            execLog.closeAll();
        }
        execLog = null;
        ctx = null;
    }
}
