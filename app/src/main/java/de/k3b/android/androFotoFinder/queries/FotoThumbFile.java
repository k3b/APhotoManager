package de.k3b.android.androFotoFinder.queries;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import de.k3b.android.util.MediaScanner;
import de.k3b.android.util.OsUtils;

/**
 * Thumb (database independant) file related functions
 * Created by k3b on 30.06.2016.
 */
public class FotoThumbFile {
    private static final String THUMBNAIL_DIR_NAME = ".thumbnails";
    private final SharedPreferences mPrefs;
    private File mThumbRoot;

    public FotoThumbFile(Context context) {
        mPrefs = PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext());
        String thumbRoot = mPrefs.getString("ThumbRoot", null);

        mThumbRoot = (thumbRoot != null) ? new File(thumbRoot) : null;
    }

    public static File getTumbDir(String fullPath, int maxDepth) {
        File candidateParent  = (fullPath != null) ? new File(fullPath) : null;
        while ((candidateParent != null) && (maxDepth >= 0)) {
            File thumbsDir = new File(candidateParent, THUMBNAIL_DIR_NAME);
            if (thumbsDir.exists()) return thumbsDir;
            maxDepth--;
            candidateParent = candidateParent.getParentFile();
        }
        return null;
    }

    public File getThumbRoot() {
        return mThumbRoot;
    }

    public FotoThumbFile setThumbRoot(File newValue) {
        mThumbRoot = newValue;

        mPrefs.edit()
                .putString("ThumbRoot", (newValue != null) ? newValue.getAbsolutePath() : null)
                .commit();
        return this;
    }

    /** #53 Optimisation: fast thumbnail load without media-db-search if it has been restructured
     * @return null if no fast thumbnail was found
     * */
     public File getThumbFileIfExist(long id, long thumbType) {
        File thumb = getThumbFile(mThumbRoot, id, thumbType);
        if ((thumb != null) && thumb.exists()) return thumb;
        return null;
    }

    /** calculate the full thumb file path */
    public static File getThumbFile(File thumbDirRoot, long id, long thumbType) {
        if (thumbDirRoot != null) {
            // i.e img_23.1/1234523.jpg (.23 id ending with "23)
            String name = String.format("img_%2$02d.%1$d/%3$d.jpg", thumbType, id % 100, id);
            return new File(thumbDirRoot, name);
        }
        return null;
    }

    /** get all existing getThumbRoot-Dirs */
    public static File[] getThumbRootFiles() {
        File[] mountFiles = OsUtils.getExternalStorageDirFiles();
        if (mountFiles != null) {
            for (int i = mountFiles.length -1; i >= 0; i--) {
                final File rootCandidate = getThumbDir(mountFiles[i]);
                mountFiles[i] = rootCandidate.exists() ?  rootCandidate : null;
            }

            return mountFiles;
        }
        return null;
    }

    /** getThumbDir("/mnt/sdcard") = "/mnt/sdcard/DCIM/.thumbnails" */
    public static File getThumbDir(File root) {
        // /mnt/sdcard/DCIM/.thumbnails/12345.jpg

        return OsUtils.buildPath(root, Environment.DIRECTORY_DCIM, THUMBNAIL_DIR_NAME);
    }

    static String[] getFileNamesList(File thumbDirRoot) {
        // !!!! todo test auf extsd via repair
        List<String> result = new ArrayList<String>();
        getFileNamesList(thumbDirRoot.getAbsolutePath().length()+1, thumbDirRoot, result);

        return result.toArray(new String[result.size()]);
    }

    private static void getFileNamesList(int start, File thumbDirRoot, List<String> result) {
        final File[] rootFiles = thumbDirRoot.listFiles(MediaScanner.JPG_FILENAME_FILTER);

        for(File f : rootFiles) {
            result.add(f.getAbsolutePath().substring(start));
        }

        File[] subDirs = thumbDirRoot.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return ((file != null) && (file.isDirectory()) && (!file.getName().startsWith(".")));
            }
        });

        if ((subDirs != null) && (subDirs.length > 0)) {
            for(File f : subDirs) {
                getFileNamesList(start, f, result);
            }

        }
    }
}
