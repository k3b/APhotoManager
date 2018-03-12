/*
 * Copyright (c) 2015-2018 by k3b.
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

package de.k3b.io.collections;

import java.io.File;
import java.util.Date;

/**
 * The Multi-selection data for all photo commands.
 * Unmodifyable list of file names and optional their IDs.
 *
 * Created by k3b on 17.05.2016.
 */
public class SelectedFiles  {
    private static final String DELIMITER = ",";
    private static final String SORUNDER = "'";
    private final String[] mFileNames;
    private final Long[] mIds;
    private final Date[] mDatesPhotoTaken;

    public static String[] getFileNameList(String fileNameListAsString) {
        return (fileNameListAsString != null) ? fileNameListAsString.split(DELIMITER) : null;
    }

    public static SelectedFiles create(String fileNameListAsString, String idListAsString, String selectedDates) {
        Date[] dates = null;
        if (selectedDates != null) {
            Long[] dateIds = parseIds(selectedDates);
            if ((dateIds != null) && (dateIds.length > 0)) {
                dates = new Date[dateIds.length];
                for(int i = 0; i < dateIds.length; i++) {
                    Long dateId = dateIds[i];
                    dates[i] = ((dateId != null) && (dateId.longValue() != 0)) ? new Date(dateId.longValue()) : null;
                }
            }
        }
        return new SelectedFiles(getFileNameList(fileNameListAsString), parseIds(idListAsString), dates);
    }

    public SelectedFiles(String[] fileNameList, Long[] ids, Date[] datesPhotoTaken) {
        mFileNames = fileNameList;
        if (mFileNames != null) {
            for (int i = mFileNames.length -1; i >= 0; i--) {
                mFileNames[i] = reomoveApostrophes(mFileNames[i]);
            }
        }
        mIds = ids;
        mDatesPhotoTaken = datesPhotoTaken;
    }

    private static Long[] parseIds(String listAsString) {
        Long[] result = null;

        if ((listAsString != null) && (listAsString.length() > 0)) {
            String itemsAsString[] = listAsString.split(DELIMITER);
            result = new Long[itemsAsString.length];
            for (int i= 0; i < itemsAsString.length; i++) {
                result[i] = Long.valueOf(itemsAsString[i]);
            }
        }

        return result;
    }

    /** removes SORUNDER from beginning/end if present. Package to allow unittests */
    static String reomoveApostrophes(String fileName) {
        if ((fileName != null) && (fileName.length() > 2)
                && (fileName.startsWith(SORUNDER)) && (fileName.endsWith(SORUNDER))) {
            return fileName.substring(1, fileName.length()-1);
        }
        return fileName;
    }

    /** convert String array of path-s to array of Files */
    public static File[] getFiles(String[] fileNames) {
        // getFileNames();
        if ((fileNames == null) || (fileNames.length == 0)) return null;

        File[] result = new File[fileNames.length];
        int i = 0;
        for (String name : fileNames) {
            if (name != null) {
                result[i++] = new File(name);
            }
        }

        if (i == 0) return null;
        return result;
    }

    public int getNonEmptyNameCount() {
        int result = 0;
        if (mFileNames != null) {
            for (String item : mFileNames) {
                if (item != null) {
                    result++;
                }
            }
        }
        return result;
    }

    /** converts this into komma seperated list of names */
    public String toString() {
        String[] mFileNames = this.mFileNames;
        return toString(SORUNDER, mFileNames);
    }

    public static <T> String toString(String SORUNDER, Date[] values) {
        if ((values != null) && (values.length > 0)) {
            Long[] lvalue = new Long[values.length];
            for (int i = 0; i < values.length; i++) {
                lvalue[i] = (values[i] == null) ? 0 : values[i].getTime();
            }
            return toString(SORUNDER, lvalue);
        }
        return null;
    }

    public static <T> String toString(String SORUNDER, T[] values) {
        // Arrays.asList()
        StringBuilder result = new StringBuilder();
        if (values != null) {
            boolean mustAddDelimiter = false;
            for (T item : values) {
                if (item != null) {
                    if (mustAddDelimiter) {
                        result.append(DELIMITER);
                    }
                    mustAddDelimiter = true;
                    result.append(SORUNDER).append(item).append(SORUNDER);
                }
            }
        }
        return result.toString();
    }

    /** converts this into komma seperated list of names */
    public String toIdString() {
        return toString("", this.mIds);
    }

    /** converts this into komma seperated list of names */
    public String toDateString() {
        return toString("", this.mDatesPhotoTaken);
    }

    public int size() {
        return (mFileNames == null) ? 0 : mFileNames.length;
    }

    public String[] getFileNames() {
        return mFileNames;
    }

    public String getFileName(int i) {
        if ((mFileNames != null) && (i >= 0) && (i < mFileNames.length)) return mFileNames[i];
        return null;
    }
    public File getFile(int i) {
        String name = getFileName(i);
        if (name != null) return new File(name);
        return null;
    }

    public Long getId(int i) {
        if ((i >= 0) && (i < mIds.length)) return mIds[i];
        return null;
    }

    public Long[] getIds() {
        return mIds;
    }

    /** needed for AutoRenaming which is based on DatesPhotoTaken.
     * return null if unknwon */
    public Date[] getDatesPhotoTaken() {
        return mDatesPhotoTaken;
    }
}
