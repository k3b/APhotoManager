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

/**
 * Read media changes to be reapplied on a different system so that media-metha-data keeps in sync.
 *
 * Created by k3b on 21.02.2017.
 */
public interface IMediaTransactionLogEntry {
    /**
     * The foreign key to media_ID.
     * <P>Type: INTEGER (long)</P>
     */
    long getMediaID();

    /**
     * full path to the media item beeing updated.
     * alternate key.
     */
    String getFullPath();

    /**
     * The date & time that the image was modified
     * of milliseconds since jan 1, 1970.
     * <P>Type: INTEGER</P>
     */
    long getModificationDate();

    /**
     * the type of change.
     * One of the CMD_xxx values.
     */
    MediaTransactionLogEntryType getCommand();

    /**
     * Data that belongs to command.
     */
    String getCommandData();
}
