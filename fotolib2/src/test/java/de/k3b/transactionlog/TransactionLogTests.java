/*
 * Copyright (c) 2018-2020 by k3b.
 *
 * This file is part of #APhotoManager (https://github.com/k3b/APhotoManager/)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *
 * for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */

package de.k3b.transactionlog;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import de.k3b.LibGlobal;
import de.k3b.TestUtil;
import de.k3b.io.DateUtil;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.FileFacadeLogger;
import de.k3b.io.filefacade.IFile;
import de.k3b.io.filefacade.StringFileFacade;
import de.k3b.media.MediaFormatter;
import de.k3b.media.PhotoPropertiesDTO;

/**
 * Analyses TransactionLog to generate move/copy/delete script and update-dtos.
 *
 * Created by k3b on 21.02.2017.
 */

public class TransactionLogTests {
    @Test
    public void parseShouldFindDeleted() throws IOException {
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

    // TODO @Test
    public void shouldWrite() throws IOException {
        LibGlobal.debugEnabledJpgMetaIo = true;
        StringFileFacade out = new StringFileFacade();
        FileFacadeLogger logger = new FileFacadeLogger(out);

        //logger.log(message);
        // logger.close();

        final int id = 1;

        final long now = DateUtil.parseIsoDate("20071224T12:39").getTime();
        final TransactionLoggerBase transactionLogger = new TransactionLoggerBase(logger, now);
        log(transactionLogger, id);
        transactionLogger.close();

        Assert.assertEquals("", out.getOutputString());
    }

    protected void log(TransactionLoggerBase transactionLogger, int id) {
        List<String> oldTags = Arrays.asList("oldA" + id, "oldB" + id);
        final PhotoPropertiesDTO testMedia = TestUtil.createTestMediaDTO(id);
        final IFile testFile = FileFacade.convert("junit", testMedia.getPath());
        transactionLogger.set(id, testFile);
        transactionLogger.addChanges(testMedia, EnumSet.allOf(MediaFormatter.FieldID.class), oldTags);
        transactionLogger.addChangesCopyMove(false, FileFacade.convert("junit", testMedia.path + ".newCopy.jpg"), "junit");
        transactionLogger.addChangesCopyMove(true, FileFacade.convert("junit", testMedia.path + ".newMove.jpg"), "junit");
        transactionLogger.addChanges(MediaTransactionLogEntryType.DELETE, null, true);
        transactionLogger.set(id + 100, testFile.getParentIFile());
        transactionLogger.addChanges(MediaTransactionLogEntryType.MOVE_DIR, "/path/to/other/dir", true);
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
