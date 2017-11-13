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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.k3b.media.MediaDTO;

/**
 * Analyses TransactionLog to generate move/copy/delete script and update-dtos.
 *
 * Created by k3b on 21.02.2017.
 */

public class TransactionLogParser {
    private final Iterator<IMediaTransactionLogEntry> sortedTransactions;
    private final BufferedWriter fileUpdateBatch;
    private IMediaTransactionLogEntry lastLog = null;

    public static class Status {
        public MediaDTO dto = new MediaDTO();
        public List<String> filenames = new ArrayList<String>();
        public boolean isDelete = false;
        private long lastId=-1;
        private String path;

        public Status(IMediaTransactionLogEntry log) {
            lastId = log.getMediaID();
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
        @Override
        public String toString() {
            Status log = this;
            StringBuilder sb = new StringBuilder();
            sb
                    .append(log.getClass().getSimpleName())
                    .append("#")
                    .append(log.lastId)
                    .append("@")
                    .append(log.getPath())
                    .append(":[")
                    .append(log.dto)
                    .append("]")
            ;
            return sb.toString();
        }
    }

    public TransactionLogParser(Iterator<IMediaTransactionLogEntry> sortedTransactions, BufferedWriter fileUpdateBatch) {

        this.sortedTransactions = sortedTransactions;
        this.fileUpdateBatch = fileUpdateBatch;
    }

    protected IMediaTransactionLogEntry getNextLog() {
        IMediaTransactionLogEntry result = this.lastLog;
        this.lastLog = null;
        if((result == null) && sortedTransactions.hasNext()) {
            result = sortedTransactions.next();
        }
        return result;
    }
    public Status getNext()  throws IOException {
        IMediaTransactionLogEntry log = getNextLog();

        if (log != null) {
            Status status = createMedia(log);
            while (null != (log = getNextLog())) {
                if (null == process(status, log)) {
                    if (status.isDelete) {
                        log = getNextLog();
                        status = createMedia(log);
                    } else {
                        this.lastLog = log;
                        return status;
                    }
                }
            }
        }
        return null;
    }

    private Status createMedia(IMediaTransactionLogEntry log) throws IOException {
        if (log != null) {
            Status dto = new Status(log);
            return process(dto, log);
        }
        return null;
    }

    /** @return null if status processing is completed */
    private Status process(Status status, IMediaTransactionLogEntry log) throws IOException {
        if (log.getMediaID() != status.lastId) return null;

        MediaTransactionLogEntryType command = log.getCommand();
        if (command.compareTo(MediaTransactionLogEntryType.DELETE) == 0) {
            this.fileUpdateBatch.write("call apmDelete.cmd \"" + log.getFullPath() + "\"");
            this.fileUpdateBatch.newLine();
            this.fileUpdateBatch.flush();
            status.isDelete = true;
            return null;
        }
        status.setPath(log.getFullPath());
        return status;
    }
}
