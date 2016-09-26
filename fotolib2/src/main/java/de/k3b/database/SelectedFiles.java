package de.k3b.database;

import java.io.File;

/**
 * Unmodifyable list of file names and optional their IDs.
 *
 * Created by k3b on 17.05.2016.
 */
public class SelectedFiles  {
    private static final String DELIMITER = ",";
    private static final String SORUNDER = "'";
    private final String[] mFileNames;
    private final Long[] mIds;

    public SelectedFiles(String fileNameListAsString, String idListAsString) {
        this((fileNameListAsString != null) ? fileNameListAsString.split(DELIMITER) : null, idListAsString);
    }

    public SelectedFiles(String[] fileNameList, String idListAsString) {
        this(fileNameList, parseIds(idListAsString));
    }

    public SelectedFiles(String[] fileNameList, Long[] ids) {
        mFileNames = fileNameList;
        if (mFileNames != null) {
            for (int i = mFileNames.length -1; i >= 0; i--) {
                mFileNames[i] = reomoveApostrophes(mFileNames[i]);
            }
        }
        mIds = ids;
    }

    public SelectedFiles(SelectedItems items, SelectedItems.Id2FileNameConverter id2FileNameConverter) {
        this(items.getFileNames(id2FileNameConverter), items.getIds());
    }

    private static Long[] parseIds(String idListAsString) {
        if (idListAsString == null) return null;

        SelectedItems ids = new SelectedItems().parse(idListAsString);
        return ids.toArray(new Long[ids.size()]);
    }
    /** removes SORUNDER from beginning/end if present */
    public static String reomoveApostrophes(String fileName) {
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
        StringBuilder result = new StringBuilder();
        if (mFileNames != null) {
            boolean mustAddDelimiter = false;
            for (String item : mFileNames) {
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
        StringBuilder result = new StringBuilder();
        if (mIds != null) {
            boolean mustAddDelimiter = false;
            for (Long item : mIds) {
                if (mustAddDelimiter) {
                    result.append(DELIMITER);
                }
                mustAddDelimiter = true;
                result.append(item);
            }
        }
        return result.toString();
    }

    public int size() {
        return (mFileNames == null) ? 0 : mFileNames.length;
    }

    public String[] getFileNames() {
        return mFileNames;
    }

    public String getFileName(int i) {
        if ((i >= 0) && (i < mFileNames.length) && (mFileNames != null)) return mFileNames[i];
        return null;
    }

    public Long getId(int i) {
        if ((i >= 0) && (i < mIds.length)) return mIds[i];
        return null;
    }

    public Long[] getIds() {
        return mIds;
    }
}
