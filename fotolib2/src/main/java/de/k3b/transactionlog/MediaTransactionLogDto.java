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
import de.k3b.tagDB.Tag;

/**
 * Records media changes to be reapplied on a different system so that media-metha-data keeps in sync.
 *
 * Created by k3b on 21.02.2017.
 */

public class MediaTransactionLogDto implements IMediaTransactionLog {

    public static final Comparator<IMediaTransactionLog> COMPARATOR = new Comparator<IMediaTransactionLog>() {
        @Override
        public int compare(IMediaTransactionLog lhs, IMediaTransactionLog rhs) {
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

    public MediaTransactionLogDto(){}

    public MediaTransactionLogDto(long currentMediaID, String fileFullPath, long modificationDate, MediaTransactionLogEntryType mediaTransactionLogEntryType, String commandData) {
        setMediaID(currentMediaID);
        setFullPath(fileFullPath);
        setModificationDate(modificationDate);
        setCommand(mediaTransactionLogEntryType);
        setCommandData(commandData);
    }

    public MediaTransactionLogDto get(IMediaTransactionLog src) {
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


    public MediaTransactionLogDto setMediaID(long mediaID) {
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


    public MediaTransactionLogDto setFullPath(String fullPath) {
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

    public MediaTransactionLogDto setModificationDate(long modificationDate) {
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

    public MediaTransactionLogDto setCommand(MediaTransactionLogEntryType command) {
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

    public MediaTransactionLogDto setCommandData(String commandData) {
        this.commandData = commandData;
        return this;
    }

    @Override
    public String toString() {
        return toString(this);
    }

    public static String toString(IMediaTransactionLog log) {
        if (log != null) {
            StringBuilder sb = new StringBuilder();
            sb
                    .append(log.getClass().getSimpleName())
                    .append("#")
                    .append(DateUtil.toIsoDateString(new Date(log.getModificationDate())))
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
