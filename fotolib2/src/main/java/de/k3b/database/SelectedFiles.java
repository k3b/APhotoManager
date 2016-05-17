package de.k3b.database;

import java.io.File;

/**
 * Unmodifyable list of file names.
 *
 * Created by k3b on 17.05.2016.
 */
public class SelectedFiles  {
    private static final String DELIMITER = ",";
    private final String[] mFileNames;
    private final Long[] mIds;

    public SelectedFiles(String fileNameListAsString, String idListAsString) {
        this(fileNameListAsString.split(DELIMITER), idListAsString);
    }

    public SelectedFiles(String[] fileNameList, String idListAsString) {
        mFileNames = fileNameList;
        SelectedItems ids = new SelectedItems().parse(idListAsString);
        mIds = ids.toArray(new Long[ids.size()]);
    }

    /** convert String array of path-s to array of Files */
    public static File[] getFiles(String[] fileNames) {
        // getFileNames();
        if ((fileNames == null) || (fileNames.length == 0)) return null;

        File[] result = new File[fileNames.length];
        int i = 0;
        for (String name : fileNames) {
            result[i++] = new File(name);
        }

        return result;
    }

    /** converts this into komma seperated list of names */
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (mFileNames != null) {
            boolean mustAddDelimiter = false;
            for (String item : mFileNames) {
                if (mustAddDelimiter) {
                    result.append(DELIMITER);
                }
                mustAddDelimiter = true;
                result.append(item);
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

    public Long getId(int i) {
        if ((i >= 0) && (i < mIds.length)) return mIds[i];
        return null;
    }
}
