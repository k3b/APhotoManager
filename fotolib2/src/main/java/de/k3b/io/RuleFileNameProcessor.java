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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.k3b.FotoLibGlobal;

/**
 * #93: rule based file renaming for same target directory.
 *
 * Date format pattern see https://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html
 * Created by k3b on 03.08.2017.
 */

public class RuleFileNameProcessor extends FileProcessor implements IFileNameProcessor {
    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);

    public static final String APM_FILE_NAME = ".apm";
    /** i.e "yyMM" for year and month each with 2 digits */
    private String mDateFormat;
    private final SimpleDateFormat mDateFormatter = new SimpleDateFormat();
    private String mName;
    private String mNumberFormat;
    private final DecimalFormat mNumberFormatter = new DecimalFormat();
    private final File mOutDir;

    // optimisationn as long as lastDateFormatted does not changed nextFileInstanceNumber is recycled
    private String mLastDateFormatted = null;
    private int mNextFileInstanceNumber = 0;
    private static final File sSomeExampleSourceFile = new File("/a/Xxxxxxxx.jpg");

    /**
     * filename = outDir+dateFormat+name+numberFormat+fileExtension.
     * @param outDir
     */
    public RuleFileNameProcessor(File outDir) {
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
    public RuleFileNameProcessor(String dateFormat, String name, String numberFormat, File outDir) {
        this(outDir);
        set(dateFormat, name, numberFormat);
    }

    public RuleFileNameProcessor(RuleFileNameProcessor ancestor, File newDir) {
        this(newDir);
        if (ancestor != null) {
            String name = ancestor.mName;
            final File oldDir = ancestor.mOutDir;
            if ((newDir != null) && (!StringUtils.isNullOrEmpty(name) & (oldDir != null)
                    && (newDir != oldDir))) {
                name = replace(name,ancestor.getDirBaseName(),this.getDirBaseName());
                name = replace(name,ancestor.getParentDirBaseName(),this.getParentDirBaseName());
            }
            set(ancestor.mDateFormat, name,ancestor.mNumberFormat);
        }
    }

    private static String replace(String name, String oldDir, String newDir) {
        if (!StringUtils.isNullOrEmpty(oldDir) && !StringUtils.isNullOrEmpty(newDir)) {
            return name.replace(oldDir, newDir);
        }
        return name;
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
        final String DBG_CONTEXT = getClass().getSimpleName() + ".set@" + this.mOutDir;
        this.mDateFormat = null;
        this.mNumberFormat = null;
        this.mNextFileInstanceNumber = 0;

        if (!StringUtils.isNullOrEmpty(dateFormat)) {
            try {
                this.mDateFormatter.applyPattern(dateFormat);
                this.mDateFormat = dateFormat;
            } catch(Exception e) {
                logger.warn(ListUtils.toString(" ", getClass().getSimpleName(),"set@",this.mOutDir
                        ,":illegal dateFormat ",dateFormat), e);
            }
        }

        if (!StringUtils.isNullOrEmpty(numberFormat)) {
            try {
                this.mNumberFormatter.applyPattern(numberFormat);
                this.mNumberFormat = numberFormat;
                this.mNextFileInstanceNumber = 1;
            } catch(Exception e) {
                logger.warn(ListUtils.toString(" ", getClass().getSimpleName() ,".set@",this.mOutDir
                        , ":illegal numberFormat " , numberFormat), e);
            }
        }

        if (!StringUtils.isNullOrEmpty(name)) {
            this.mName = name;
        }
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

        return (!filenameWithoutPath.contains(this.mName));
    }

    /**
     * Calculate next free file name for sourceFile. Sourcefiles should be ordered asc by sourceFileDate
     *
     * @param firstFileInstanceNumber  number where numbering starts with. -1 : auto
     * @return next absoulte renamed file.
     */
    @Override
    public File getNextFile(File sourceFile, Date sourceFileDate, int firstFileInstanceNumber) {
        String name = getFile(sourceFile).getName();

        if (!mustRename(name)) {
			// no rename rule or file already matches rules
			File result = new File(this.mOutDir, name);

            // usecase: apply auto where inFile is already in outdir: no modification
            if ((sourceFile != null) && sourceFile.equals(result)) return result;

			// change file name if result already exists
			return renameDuplicate(result);
		}

		if (firstFileInstanceNumber < 0) {
            firstFileInstanceNumber = StringUtils.isNullOrEmpty(mNumberFormat) ? 0 : 1;
        }
        final String dateFormatted = getDateFormat(sourceFileDate);
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

        final String msg = getClass().getSimpleName() + ".mustRename@" + this.mOutDir
                + ": Cannot generate new unused Filename " + result;
        logger.warn(msg);

        // give up after a lot of tries.
        throw new IllegalArgumentException(msg);
    }

    public static File getFile(File _file) {
        return (_file != null) ? _file : sSomeExampleSourceFile;
    }

    @Override
    public String toString() {
        return ListUtils.toString(" ", this.getClass().getSimpleName(),  mDateFormat, mName, mNumberFormat, ": +", mNextFileInstanceNumber);
    }

    public String getDirBaseName() {
        if (mOutDir != null) return getBaseName(mOutDir.getName());
        return null;
    }

    public String getParentDirBaseName() {
        File parent = (mOutDir != null) ? mOutDir.getParentFile() : null;
        if (parent != null) return getBaseName(parent.getName());
        return null;
    }

    /** Get name without leading numbers. i.e. getBaseName("01701Test001") ==> "Test". package to allow unittesting */
    static String getBaseName(String name) {
        if (name != null) {
            int len = name.length();
            int begin = 0;
            while ((begin < len) && !isLetter(name, begin)) begin++;
            int end = len - 1;
            while ((end >= 0) && !isLetter(name, end)) end--;

            if ((begin < len) && (begin >= 0)) return name.substring(begin,end+1);
        }
        return null;
    }

    private static boolean isLetter(String name, int offset) {
        final int c = name.charAt(offset);
        final boolean result = Character.isLetter(c);
        return result;
    }

    public static String translateName(RuleFileNameProcessor srcData, File outDir) {
        RuleFileNameProcessor translated = new RuleFileNameProcessor(srcData, outDir);
        return translated.mName;
    }
}
