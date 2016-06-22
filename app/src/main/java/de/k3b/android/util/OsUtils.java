package de.k3b.android.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Created by k3b on 22.06.2016.
 */
public class OsUtils {
    public static String[] getExternalStorageDirs() {
        // other alternatives ar described here http://stackoverflow.com/questions/5694933/find-an-external-sd-card-location

        // i.e. /mnt/sdcard0
        File extDir = Environment.getExternalStorageDirectory();

        // i.e. /mnt
        File mountRoot = (extDir == null) ? null :extDir.getParentFile();
        if (mountRoot != null) {

            // assuming all dirs that are below mountRoot
            File[] mountFiles = mountRoot.listFiles();
            String [] mounts = new String[mountFiles.length];
            for (int i = mountFiles.length -1; i >= 0; i--) {
                mounts[i] = mountFiles[i].getAbsolutePath();
            }

            return mounts;

            // return relative files. we need absolute
            // return mountRoot.list();
        }

        if (extDir != null) return new String[] {extDir.getAbsolutePath()};
        return null;
    }
}
