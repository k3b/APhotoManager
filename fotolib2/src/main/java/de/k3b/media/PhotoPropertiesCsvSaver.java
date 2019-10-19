/*
 * Copyright (c) 2015-2019 by k3b.
 *
 * This file is part of AndroFotoFinder.
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

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;

import de.k3b.csv2db.csv.CsvItem;
import de.k3b.io.FileNameUtil;
import de.k3b.io.IItemSaver;
import de.k3b.io.StringUtils;

/**
 * Created by k3b on 13.10.2016.
 */

/** Transfers {@link IPhotoProperties} - items as csv to a writer via {@link #save(IPhotoProperties)} */
public class PhotoPropertiesCsvSaver implements IItemSaver<IPhotoProperties> {
    private PrintWriter printer;
    private final PhotoPropertiesCsvItem csvLine;

    /**
     * if not null file adds will be relative to this path if file is below this path
     */
    private String pathRelativeTo = null;
    private boolean compressFilePath = false;

    public PhotoPropertiesCsvSaver(PrintWriter printer) {
        csvLine = new PhotoPropertiesCsvItem();
        setPrinter(printer);
    }

    protected PhotoPropertiesCsvSaver setPrinter(PrintWriter printer) {
        this.printer = printer;

        if (this.printer != null) {
            defineHeader(PhotoPropertiesCsvItem.MEDIA_CSV_STANDARD_HEADER);
        }
        return this;
    }

    public PhotoPropertiesCsvSaver setCompressFilePathMode(String pathRelativeTo) {
        this.compressFilePath = true;
        if (!StringUtils.isNullOrEmpty(pathRelativeTo)) {
            this.pathRelativeTo = FileNameUtil.getCanonicalPath(new File(pathRelativeTo)).toLowerCase();
        } else {
            this.pathRelativeTo = null;
        }
        return this;
    }

    protected String convertPath(String path) {
        String result = null;
        if (path != null) {
            final File pathAsFile = new File(path);
            if (pathRelativeTo != null) {
                result = FileNameUtil.makePathRelative(pathRelativeTo, pathAsFile);
            }
            if (result == null) result = pathAsFile.getName();
        }
        return result;
    }
    @Override
    public boolean save(IPhotoProperties item) {
        if (item != null) {
            csvLine.clear();
            PhotoPropertiesUtil.copy(csvLine, item, true, true);
            if (!csvLine.isEmpty()) {
                if (compressFilePath) {
                    csvLine.setPath(convertPath(csvLine.getPath()));
                }
                this.printer.write(csvLine.toString());
                this.printer.write(CsvItem.DEFAULT_CHAR_LINE_DELIMITER);
                return true;
            }
        }
        return false;
    }

    private void defineHeader(String header) {
        printer.write(header);
        printer.write(CsvItem.DEFAULT_CHAR_LINE_DELIMITER);

        String[] fields = header.split("["+CsvItem.DEFAULT_CSV_FIELD_DELIMITER + "]");
        csvLine.setHeader(Arrays.asList(fields));
        csvLine.setData(new String[csvLine.maxColumnIndex + 1]);
        csvLine.setFieldDelimiter(CsvItem.DEFAULT_CSV_FIELD_DELIMITER);
    }
}
