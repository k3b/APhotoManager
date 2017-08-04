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

package de.k3b.io;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * #93: rule based file renaming for same target directory.
 *
 * Date format pattern see https://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html
 * Created by k3b on 03.08.2017.
 */

public class FileNameProcessor extends FileProcessor {
    public static final String APM_FILE_NAME = ".apm";
    /** i.e "yyMM" for year and month each with 2 digits */
    private final SimpleDateFormat dateFormatter;
    private final String name;
    private final DecimalFormat numberFormatter;
    private final File outDir;

    // optimisationn as long as lastDateFormatted does not changed nextFileInstanceNumber is recycled
    private String lastDateFormatted = null;
    private int nextFileInstanceNumber = 0;

    /**
     * filename = outDir+dateFormat+name+numberFormat+fileExtension.
     *  @param dateFormat null or dateformat
     *                    (see https://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html)
     *                    Example "yyMM" for two digit year plus two digit month
     * @param name          fix part of filename
     * @param numberFormat null or numberformat.
 *                      Example "000" always at least 3 digits
     * @param outDir
     */
    public FileNameProcessor(String dateFormat, String name, String numberFormat, File outDir) {
        this.dateFormatter = (dateFormat == null) ? null : new SimpleDateFormat(dateFormat);
        this.name = name;
        this.numberFormatter = (numberFormat == null) ? null : new DecimalFormat(numberFormat);

        this.outDir = outDir;
    }

    protected String generateFileName(Date date, int instanceNumber, String fileExtension) {
        return generateFileName(getDateFormat(date), instanceNumber, fileExtension);
    }

    protected String generateFileName(String date, int instanceNumber, String fileExtension) {

        StringBuilder result = new StringBuilder();
        result.append(date);
        if (this.name != null) result.append(this.name);

        if (this.numberFormatter != null) {
            result.append(this.numberFormatter.format(instanceNumber));
        } else if (instanceNumber > 0) {
            result.append(instanceNumber);
        }
        if (fileExtension != null) result.append(fileExtension);
        return result.toString();
    }

    private String getDateFormat(Date date) {
        return (this.dateFormatter != null) ? this.dateFormatter.format(date) : "";
    }

    protected boolean mustRename(String filenameWithoutPath) {
        return (filenameWithoutPath.indexOf(this.name) < 0);
    }

    public File getNextFile(File file, Date date, int firstFileInstanceNumber) {
        String name = file.getName();
        if (!mustRename(name)) return new File(this.outDir, name);

        final String dateFormatted = getDateFormat(date);
        final String fileExtension = FileUtils.getExtension(name);
        if (StringUtils.compare(dateFormatted, lastDateFormatted) != 0) {
            // date pattern has changed. must restart fileInstanceNumber from beginning
            nextFileInstanceNumber = firstFileInstanceNumber;
        }
        // else reuse nextFileInstanceNumber for the same date
        lastDateFormatted = dateFormatted;
        File result = null;
        int tryCount = 0;
        do {
            result = new File(this.outDir, generateFileName(dateFormatted, nextFileInstanceNumber, fileExtension));
            nextFileInstanceNumber++;
            if (!fileOrSidecarExists(result)) return result; // filename not in use yet
            tryCount++;
        } while (tryCount < 32000);

        // give up after a lot of tries.
        throw new IllegalArgumentException("Cannot generate new unused Filename " + result);
    }
}
