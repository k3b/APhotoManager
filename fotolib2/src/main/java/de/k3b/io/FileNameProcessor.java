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
    private final SimpleDateFormat mDateFormatter = new SimpleDateFormat();
    private String mName;
    private final DecimalFormat mNumberFormatter = new DecimalFormat();
    private final File mOutDir;

    // optimisationn as long as lastDateFormatted does not changed nextFileInstanceNumber is recycled
    private String mLastDateFormatted = null;
    private int mNextFileInstanceNumber = 0;
    private String mDateFormat;
    private String mNumberFormat;
    private static final File sSomeExampleSourceFile = new File("/a/Xxxxxxxx.jpg");

    /**
     * filename = outDir+dateFormat+name+numberFormat+fileExtension.
     * @param outDir
     */
    public FileNameProcessor(File outDir) {
        this.mOutDir = outDir;
    }
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
        this(outDir);
        set(dateFormat, name, numberFormat);
    }

    /**
     * filename = outDir+dateFormat+name+numberFormat+fileExtension.
     *  @param dateFormat null or dateformat
     *                    (see https://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html)
     *                    Example "yyMM" for two digit year plus two digit month
     * @param name          fix part of filename
     * @param numberFormat null or numberformat.
     *                      Example "000" always at least 3 digits
     */
    public void set(String dateFormat, String name, String numberFormat) {
        if (StringUtils.isNullOrEmpty(dateFormat)) {
            this.mDateFormat = null;
        } else {
            this.mDateFormat = dateFormat;
            this.mDateFormatter.applyPattern(dateFormat);
        }

        this.mName = name;

        if (StringUtils.isNullOrEmpty(numberFormat)) {
            this.mNumberFormat = null;
        } else {
            this.mNumberFormat = numberFormat;
            this.mNumberFormatter.applyPattern(numberFormat);
        }

        this.mNextFileInstanceNumber = 0;
    }

    protected String generateFileName(Date date, int instanceNumber, String fileExtension) {
        return generateFileName(getDateFormat(date), instanceNumber, fileExtension);
    }

    protected String generateFileName(String datePrefix, int instanceNumber, String fileExtension) {

        StringBuilder result = new StringBuilder();
        if (datePrefix != null) result.append(datePrefix);
        if (this.mName != null) result.append(this.mName);

        if (this.mNumberFormat != null) {
            result.append(this.mNumberFormatter.format(instanceNumber));
        } else if ((result.length() == 0) || (instanceNumber > 0)) {
            result.append(instanceNumber);
        }
        if (fileExtension != null) result.append(fileExtension);
        return result.toString();
    }

    private String getDateFormat(Date date) {
        return (this.mDateFormat != null) ? this.mDateFormatter.format((date == null) ? sSomeExampleSourceFile : date) : null;
    }

    protected boolean mustRename(String filenameWithoutPath) {
        boolean nameEmpty = StringUtils.isNullOrEmpty(this.mName);
        if ((mDateFormat == null) && (mNumberFormat == null) && nameEmpty) {
			// no rename parameters defined: do not rename
			return false;
		}

		if (nameEmpty) return true; // at least date or number is set

        return (filenameWithoutPath.indexOf(this.mName) < 0);
    }

    public File getNextFile(File file, Date date, int firstFileInstanceNumber) {
        String name = getFile(file).getName();
        if (!mustRename(name)) {
			// no rename rule or file already matches rules
			File result = new File(this.mOutDir, name);
			
			// change file name if result already exists
			return renameDuplicate(result);
		}
		
        final String dateFormatted = getDateFormat(date);
        final String fileExtension = FileUtils.getExtension(name);
        if ((mNextFileInstanceNumber < firstFileInstanceNumber) || (StringUtils.compare(dateFormatted, mLastDateFormatted) != 0)) {
            // date pattern has changed. must restart fileInstanceNumber from beginning
            mNextFileInstanceNumber = firstFileInstanceNumber;
        }
        // else reuse mNextFileInstanceNumber for the same date
        mLastDateFormatted = dateFormatted;
        File result = null;
        int tryCount = 0;
        do {
            result = new File(this.mOutDir, generateFileName(dateFormatted, mNextFileInstanceNumber, fileExtension));
            mNextFileInstanceNumber++;
            if (!fileOrSidecarExists(result)) return result; // filename not in use yet
            tryCount++;
        } while (tryCount < 32000);

        // give up after a lot of tries.
        throw new IllegalArgumentException("Cannot generate new unused Filename " + result);
    }

    public static File getFile(File _file) {
        return (_file != null) ? _file : sSomeExampleSourceFile;
    }
}
