package de.k3b.android.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Created by k3b on 22.06.2016.
 */
public class OsUtils {
    public static File[] getExternalStorageDirFiles() {
        // other alternatives ar described here http://stackoverflow.com/questions/5694933/find-an-external-sd-card-location

        // i.e. /mnt/sdcard0
        File extDir = Environment.getExternalStorageDirectory();

        // i.e. /mnt
        File mountRoot = (extDir == null) ? null :extDir.getParentFile();

        return  (mountRoot != null) ? mountRoot.listFiles() : null;
    }

    private static boolean isAllowed(File mountFile) {
        return ((mountFile != null) && mountFile.isDirectory() && !mountFile.isHidden());
    }

    /**
     * Append path segments to given base path, returning result.
     */
    public static File buildPath(File base, String... segments) {
        File cur = base;
        for (String segment : segments) {
            if (cur == null) {
                cur = new File(segment);
            } else {
                cur = new File(cur, segment);
            }
        }
        return cur;
    }
}
