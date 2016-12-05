/*
 * Copyright (c) 2015-2016 by k3b.
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

import java.io.PrintWriter;
import java.util.Arrays;

import de.k3b.csv2db.csv.CsvItem;

/**
 * Created by k3b on 13.10.2016.
 */

public class MediaCsvSaver {
    private final PrintWriter printer;
    private final MediaCsvItem csvLine;

    public MediaCsvSaver(PrintWriter printer) {
        this.printer = printer;
        csvLine = new MediaCsvItem();
        defineHeader(MediaCsvItem.MEDIA_CSV_STANDARD_HEADER);
    }

    public void save(IMetaApi item) {
        if (item != null) {
            csvLine.clear();
            MediaUtil.copy(csvLine, item, true, true);
            if (!csvLine.isEmpty()) {
                this.printer.write(csvLine.toString());
                this.printer.write(CsvItem.DEFAULT_CHAR_LINE_DELIMITER);
            }
        }
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
