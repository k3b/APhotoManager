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
import java.io.StringWriter;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

/**
 * Analyses TransactionLog to generate move/copy/delete script and update-dtos.
 *
 * Created by k3b on 21.02.2017.
 */

public class TransactionLogParserTests {
    @Test
    public void shouldFindDeleted() throws IOException {
        ArrayList<IMediaTransactionLogEntry> items = new ArrayList<IMediaTransactionLogEntry>();
        addItems(items, "a.jpg"
                , MediaTransactionLogEntryType.GPS.toString(),"gps"
                , MediaTransactionLogEntryType.DELETE.toString(),null);
        StringWriter stringWriter = new StringWriter();
        BufferedWriter fileUpdateBatch = new BufferedWriter(stringWriter);
        TransactionLogParser parser = new TransactionLogParser(items.iterator(), fileUpdateBatch);

        TransactionLogParser.Status data = parser.getNext();
        Assert.assertEquals("data", null, data);
        Assert.assertEquals("bat", "call apmDelete.cmd 'a.jpg'", stringWriter.toString().replace("\"","'").trim());
        fileUpdateBatch.close();
    }

    private long currentMediaID = 0;
    private void addItems(ArrayList<IMediaTransactionLogEntry> items, String initialFileName, String... data) {
        String fileFullPath = initialFileName;
        currentMediaID++;
        int i = 0;
        IMediaTransactionLogEntry dto;
        while (i < data.length) {
            int modificationDate = i;
            MediaTransactionLogEntryType mediaTransactionLogEntryType = MediaTransactionLogEntryType.get(data[i++]);
            String commandData = data[i++];
            dto = new MediaTransactionLogEntryDto(currentMediaID, fileFullPath, modificationDate, mediaTransactionLogEntryType, commandData);
            items.add(dto);
            if (dto.getCommand().compareTo(MediaTransactionLogEntryType.MOVE) == 0) {
                fileFullPath = dto.getCommandData();
            }
        }


    }
}
