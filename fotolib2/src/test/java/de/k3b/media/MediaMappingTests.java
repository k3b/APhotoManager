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
package de.k3b.media;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.k3b.TestUtil;

/**
 * Used to document meta data mapping
 *
 * Created by k3b on 29.04.2017.
 */

public class MediaMappingTests {
    protected StringBuilder debugResult = new StringBuilder();

    private static final String HEADER = "\n" +
            "| property | #1 | #2 | #3 | #4 | #5 | #6 | #7 | #8 | #9 |\n" +
            "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n";

    @BeforeClass
    public static void initDirectories() {
        System.out.printf("Generated on " + new Date() +
                " with fotolib2/src/tests/" +
                MediaMappingTests.class.getName() + "\n\n\n");

    }

    @Before
    public void setup() throws IOException {
        debugResult.setLength(0);
    }


    private class ImageMetaReaderDummy extends ImageMetaReader {
        protected boolean isEmpty(Object result, int tryNumber, String debugContext, String debugFieldName) {
            return isEmptyDbgImpl(debugResult, result, tryNumber, debugContext, debugFieldName);
        }
    }

    private class ExifInterfaceExDummy extends ExifInterfaceEx {
        protected boolean isEmpty(Object result, int tryNumber, String debugContext, String debugFieldName) {
            return isEmptyDbgImpl(debugResult, result, tryNumber, debugContext, debugFieldName);
        }

    }

    private class MediaXmpSegmentDummy extends MediaXmpSegment {
        protected String getPropertyAsString(String debugContext, MediaXmpFieldDefinition... definitions) {
            debugResult.append("| ").append(debugContext.substring(3)).append(" | ");
            for (MediaXmpFieldDefinition definition : definitions) {
                debugResult.append(definition.getXmpNamespace().getPrefix()).append(".").append(definition.getShortName()).append(" | ");
            }
            debugResult.append("\n");
            return null;
        }

        protected List<String> getPropertyArray(String debugContext, MediaXmpFieldDefinition... definitions) {
            getPropertyAsString(debugContext, definitions);
            return null;
        }
    }

    private class MediaCsvItemDummy extends MediaCsvItem {
        private ArrayList<MediaXmpFieldDefinition> cols = new ArrayList<>();

        MediaCsvItemDummy() {
            initFieldDefinitions(null);
        }

        protected Date getDate(String debugContext, int... columnNumbers) {
            debugResult.append("| ").append(debugContext.substring(3)).append(" | ");
            for (int columnNumber : columnNumbers) {
                MediaXmpFieldDefinition definition = cols.get(columnNumber);
                debugResult.append(definition.getXmpNamespace().getPrefix()).append(".").append(definition.getShortName()).append(" | ");
            }
            debugResult.append("\n");
            return null;
        }

        protected String getString(String debugContext, int columnNumber) {
            getDate(debugContext, columnNumber);
            return null;
        }
        protected int getColumnIndex(List<String> lcHeader, MediaXmpFieldDefinition columnDefinition) {
            cols.add(columnDefinition);
            return cols.indexOf(columnDefinition);
        }
    }

    private static boolean isEmptyDbgImpl(StringBuilder debugResult, Object result, int tryNumber, String debugContext, String debugFieldName) {
        if (tryNumber == 1) {
            debugResult.append("| ").append(debugContext.substring(3)).append(" | ");
        } else {
            if (result != null) {
                debugResult.append("='").append(result).append("'");
            }
            debugResult.append(" | ");
        }
        if (debugFieldName != null) {
            debugResult.append(debugFieldName);
        } else {
            debugResult.append("\n");
        }
        return true;
    }

    @Test
    public void dump1ImageMetaReaderDummy() throws IOException {
        IMetaApi sut = new ImageMetaReaderDummy();
        dump(sut);

    }

    @Test
    public void dump2ExifInterfaceExDummy() throws IOException {
        IMetaApi sut = new ExifInterfaceExDummy();
        dump(sut);

    }

    @Test
    public void dump3MediaXmpSegmentDummy() throws IOException {
        IMetaApi sut = new MediaXmpSegmentDummy();
        dump(sut);

    }


    @Test
    public void dump4MediaCsvItemDummy() throws IOException {
        IMetaApi sut = new MediaCsvItemDummy();
        dump(sut);
    }

    protected void dump(IMetaApi sut) {
        MediaUtil.copyNonEmpty(TestUtil.createTestMediaDTO(3), sut);

        System.out.printf("**" + sut.getClass().getSuperclass().getName() + "**\n" + HEADER);
        System.out.printf(debugResult.toString() + "\n");
    }
}