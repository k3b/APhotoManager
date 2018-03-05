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
package de.k3b.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Date;
import static org.mockito.Mockito.*;

/**
 * #93: rule based file renaming
 * Created by k3b on 03.08.2017.
 */

public class FileNameProcessorTests {
    private static final File X_FAKE_OUTPUT_DIR = new File("/fakeOutputDir").getAbsoluteFile();
    private static final File X_FAKE_INPUT_DIR = new File("/fakeInputDir").getAbsoluteFile();

    @Test
    public void shouldExpandDateTimeInfo() {
        Date date = DateUtil.parseIsoDate("20171224");
        RuleFileNameProcessor sut = new RuleFileNameProcessor("yyMM", "Hello", null, X_FAKE_OUTPUT_DIR);
        Assert.assertEquals("1712Hello.jpg", sut.generateFileName(date, 0,".jpg"));
    }

    @Test
    public void shouldAddNumber() {
        RuleFileNameProcessor sut = new RuleFileNameProcessor(null, "Hello", null, X_FAKE_OUTPUT_DIR);
        Assert.assertEquals("Hello1.jpg", sut.generateFileName((Date) null, 1,".jpg"));
        Assert.assertEquals("Hello.jpg", sut.generateFileName((Date) null, 0,".jpg"));
    }

    @Test
    public void shouldAddNumberWithFormat() {
        RuleFileNameProcessor sut = new RuleFileNameProcessor(null, "Hello", "000", X_FAKE_OUTPUT_DIR);
        Assert.assertEquals("Hello001.jpg", sut.generateFileName((Date) null, 1,".jpg"));
        Assert.assertEquals("Hello1001.jpg", sut.generateFileName((Date) null, 1001,".jpg"));
    }

    @Test
    public void shouldHandleMustRename() {
        RuleFileNameProcessor sut = new RuleFileNameProcessor(null, "Hello", "000", X_FAKE_OUTPUT_DIR);
        Assert.assertEquals(false, sut.mustRename("171224Hello123.jpg"));
        Assert.assertEquals(true, sut.mustRename("171224Something123.jpg"));
    }


    @Test
    public void shouldGetNextFreeFileWithoutRename() {
        IFileNameProcessor sut = spy(new RuleFileNameProcessor(null, "Hello", null, X_FAKE_OUTPUT_DIR));
        File outFile = sut.getNextFile(new File(X_FAKE_INPUT_DIR, "171224Hello1234.jpg"),null,0);
        Assert.assertEquals("171224Hello1234.jpg", outFile.getName());
    }

    @Test
    public void shouldGetNextFreeFile() {
        RuleFileNameProcessor sut = spy(new RuleFileNameProcessor(null, "Hello", null, X_FAKE_OUTPUT_DIR));
        registerFakeFiles(sut, "Hello.jpg", "Hello1.jpg", "Hello2.jpg.xmp", "Hello3.xmp");
        File outFile = sut.getNextFile(new File(X_FAKE_INPUT_DIR, "1234.jpg"),null,0);
        Assert.assertEquals("Hello4.jpg", outFile.getName());
    }

    @Test
    public void shouldGetNextFreeFileEmpty() {
        RuleFileNameProcessor sut = spy(new RuleFileNameProcessor(null, null, null, X_FAKE_OUTPUT_DIR));
        registerFakeFiles(sut, "Hello.jpg");
        File outFile = sut.getNextFile(new File(X_FAKE_INPUT_DIR, "originalName.jpg"),null,0);
        Assert.assertEquals("originalName.jpg", outFile.getName());
    }

    @Test
    public void shouldGetNextFreeFileEmptyExisting() {
        RuleFileNameProcessor sut = spy(new RuleFileNameProcessor(null, null, null, X_FAKE_OUTPUT_DIR));
        registerFakeFiles(sut, "originalName.jpg");
        File outFile = sut.getNextFile(new File(X_FAKE_INPUT_DIR, "originalName.jpg"),null,0);
        Assert.assertEquals("originalName(1).jpg", outFile.getName());
    }

    @Test
    public void shouldGetNextFreeFileSequenceWithDifferentDates() {
        RuleFileNameProcessor sut = spy(new RuleFileNameProcessor("yy", "Hello", null, X_FAKE_OUTPUT_DIR));
        registerFakeFiles(sut, "16Hello.jpg", "16Hello1.jpg", "17Hello.jpg");

        File someInputFile = new File(X_FAKE_INPUT_DIR, "1234.jpg");
        int firstFileInstanceNumber = 0;

        Date date = DateUtil.parseIsoDate("20151224");
        Assert.assertEquals("15Hello.jpg", sut.getNextFile(someInputFile,date, firstFileInstanceNumber).getName());

        date = DateUtil.parseIsoDate("20161224");
        Assert.assertEquals("16Hello2.jpg", sut.getNextFile(someInputFile,date, firstFileInstanceNumber).getName());
        Assert.assertEquals("16Hello3.jpg", sut.getNextFile(someInputFile,date, firstFileInstanceNumber).getName());

        date = DateUtil.parseIsoDate("20171224");
        Assert.assertEquals("17Hello1.jpg", sut.getNextFile(someInputFile,date, firstFileInstanceNumber).getName());

        date = DateUtil.parseIsoDate("20181224");
        Assert.assertEquals("18Hello.jpg", sut.getNextFile(someInputFile,date, firstFileInstanceNumber).getName());
    }

    @Test
    public void shouldGetFileExtension() {
        Assert.assertEquals(".jpg", FileUtils.getExtension("hello.jpg"));
    }

    @Test
    public void shouldGetBaseName() {
        CharSequence baseName = RuleFileNameProcessor.getBaseName("01701Test001");
        Assert.assertEquals("Test", baseName);

        baseName = RuleFileNameProcessor.getBaseName("Test");
        Assert.assertEquals("Test", baseName);

        baseName = RuleFileNameProcessor.getBaseName("001");
        Assert.assertEquals(null, baseName);
    }

    @Test
    public void shouldFixRuleOnFileChange() {
        RuleFileNameProcessor ancestor = new RuleFileNameProcessor(
                null,"Crete-habour",null,
                new File ("/DCIM/2007Crete/habour/")
        );

        String newName = RuleFileNameProcessor.translateName(ancestor, new File ("/DCIM/2008Teneriffe/beach/"));

        Assert.assertEquals("Teneriffe-beach", newName);
    }

    /** these files exist in source-dir and in dest-dir */
    private static void registerFakeFiles(RuleFileNameProcessor sut, String... filenames) {
        if (filenames.length == 0) {
            doReturn(false).when(sut).osFileExists(any(File.class));
        } else {
            for (String filename : filenames) {
                doReturn(true).when(sut).osFileExists(new File(X_FAKE_OUTPUT_DIR, filename));
            }
        }
    }
}
