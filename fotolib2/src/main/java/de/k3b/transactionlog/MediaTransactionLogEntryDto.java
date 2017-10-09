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

import java.util.Comparator;
import java.util.Date;

import de.k3b.io.DateUtil;

/**
 * Records media changes to be reapplied on a different system so that media-metha-data keeps in sync.
 *
 * Created by k3b on 21.02.2017.
 */

public class MediaTransactionLogEntryDto implements IMediaTransactionLogEntry {

    public static final Comparator<IMediaTransactionLogEntry> COMPARATOR = new Comparator<IMediaTransactionLogEntry>() {
        @Override
        public int compare(IMediaTransactionLogEntry lhs, IMediaTransactionLogEntry rhs) {
            long diff = lhs.getMediaID() - rhs.getMediaID();
            if (diff == 0) {
                diff = lhs.getModificationDate() - rhs.getModificationDate();
            }
            if (diff == 0) {
                return lhs.getCommand().toString().compareToIgnoreCase(rhs.getCommand().toString());
            }
            return (int) diff;
        }
    };


    /**
     * The foreign key to media_ID.
     * <P>Type: INTEGER (long)</P>
     */
    private long mediaID;

    /**
     * full path to the media item beeing updated.
     * alternate key.
     */
    private String fullPath;

    /**
     * The date & time that the image was modified
     * of milliseconds since jan 1, 1970.
     * <P>Type: INTEGER</P>
     */
    private long modificationDate;

    /**
     * the type of change.
     * One of the CMD_xxx values.
     */
    private MediaTransactionLogEntryType command;

    /**
     * Data that belongs to command.
     */
    private String commandData;

    public MediaTransactionLogEntryDto(){}

    public MediaTransactionLogEntryDto(long currentMediaID, String fileFullPath, long modificationDate, MediaTransactionLogEntryType mediaTransactionLogEntryType, String commandData) {
        setMediaID(currentMediaID);
        setFullPath(fileFullPath);
        setModificationDate(modificationDate);
        setCommand(mediaTransactionLogEntryType);
        setCommandData(commandData);
    }

    public MediaTransactionLogEntryDto get(IMediaTransactionLogEntry src) {
        this.setCommand(src.getCommand());
        this.setCommandData(src.getCommandData());
        this.setFullPath(src.getFullPath());
        this.setModificationDate(src.getModificationDate());
        this.setMediaID(src.getMediaID());
        return this;
    }

    /**
     * The foreign key to media_ID.
     * <P>Type: INTEGER (long)</P>
     */
    @Override
    public long getMediaID() {
        return mediaID;
    }


    public MediaTransactionLogEntryDto setMediaID(long mediaID) {
        this.mediaID = mediaID;
        return this;
    }

    /**
     * full path to the media item beeing updated.
     * alternate key.
     */
    @Override
    public String getFullPath() {
        return fullPath;
    }


    public MediaTransactionLogEntryDto setFullPath(String fullPath) {
        this.fullPath = fullPath;
        return this;
    }

    /**
     * The date & time that the image was modified
     * of milliseconds since jan 1, 1970.
     * <P>Type: INTEGER</P>
     */
    @Override
    public long getModificationDate() {
        return modificationDate;
    }

    public MediaTransactionLogEntryDto setModificationDate(long modificationDate) {
        this.modificationDate = modificationDate;
        return this;
    }

    /**
     * the type of change.
     * One of the CMD_xxx values.
     */
    @Override
    public MediaTransactionLogEntryType getCommand() {
        return command;
    }

    public MediaTransactionLogEntryDto setCommand(MediaTransactionLogEntryType command) {
        this.command = command;
        return this;
    }

    /**
     * Data that belongs to command.
     */
    @Override
    public String getCommandData() {
        return commandData;
    }

    public MediaTransactionLogEntryDto setCommandData(String commandData) {
        this.commandData = commandData;
        return this;
    }

    @Override
    public String toString() {
        return toString(this);
    }

    public static String toString(IMediaTransactionLogEntry log) {
        if (log != null) {
            StringBuilder sb = new StringBuilder();
            sb
                    .append(log.getClass().getSimpleName())
                    .append("#")
                    .append(DateUtil.toIsoDateTimeString(new Date(log.getModificationDate())))
                    .append(" ")
                    .append(log.getMediaID())
                    .append("@")
                    .append(log.getFullPath())
                    .append(":")
                    .append(log.getCommand())
                    .append("-")
                    .append(log.getCommandData())
            ;
            return sb.toString();
        }
        return "";
    }
}
