/*
 * Copyright (c) 2015-2020 by k3b.
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
 
package de.k3b.android.util;

import android.os.Environment;

import java.io.File;

import de.k3b.android.io.DocumentFileTranslator;
import de.k3b.io.OSDirectory;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;

/**
 * Android sepecific helpers
 * Created by k3b on 22.06.2016.
 */
public class OsUtils {
    public static File[] getExternalStorageDirFiles() {
        // other alternatives ar described here http://stackoverflow.com/questions/5694933/find-an-external-sd-card-location

        // i.e. /mnt/sdcard0
        File extDir = Environment.getExternalStorageDirectory();

        // i.e. /mnt
        File mountRoot = (extDir == null) ? null :extDir.getParentFile();

        File[] files = (mountRoot != null) ? mountRoot.listFiles() : null;
        if (((files == null) || (files.length == 0)) && (mountRoot != null)) {
            // getExternalStorageDirectory = /storage/emulated/0 ==> /storage if emulated is protected
            mountRoot = mountRoot.getParentFile();
            files = (mountRoot != null) ? mountRoot.listFiles() : null;
            for (int i = files.length - 1; i >= 0; i--) {
                if (files[i].getName().compareToIgnoreCase("emulated") == 0) {
                    // emulated is protected so use emulated/0 instead
                    files[i] = new File(mountRoot.getAbsolutePath() + "/emulated/0");
                }
            }
        }
        return files;
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

    /**
     * create android specific dir root.
     *
     * @param factory null or factory that creates OSDirectory or subclass of OSDirectory.
     */
    public static OSDirectory getRootOSDirectory(OSDirectory factory) {
        // #103: bugfix
        // this works for android-4.4 an earlier and on rooted devices
        final String context = "OsUtils getRootOSDirectory ";
        OSDirectory rootDir = createOsDirectory(
                FileFacade.convert(context + 1, "/"), factory);

        if (rootDir.getChildren().size() == 0) {
            // load on demand has failed on non rooted device

            // externalStorageFile
            // android-4.2 : /mnt/sdcard/ + /mnt/extsd/
            // android-4.4 : /storage/sdcard0/ + /storage/sdcard1/
            // android-6ff   /storage/emulated/0/ + /storage/1234-5678/
            final IFile externalStorageFile = FileFacade.convert(context + 2, Environment.getExternalStorageDirectory());
            final OSDirectory externalStorageDir = rootDir.includeRoot(
                    externalStorageFile, null);

            String[] knownSafRoots = DocumentFileTranslator.getRoots();
            if (knownSafRoots != null) {
                for (String safRoot : knownSafRoots) {
                    rootDir.includeRoot(
                            FileFacade.convert(context + 3, safRoot), null);
                }
            }

            // legacy support: assume sd-card is sibling to externalStorageFile. (i.e. /mnt/sdcard may also has a sibling /mnt/extsd)
            final IFile mountRootFile = externalStorageFile.getParentFile();
            final IFile[] mounts = (mountRootFile == null) ? null : mountRootFile.listFiles();
            if ((mounts != null) && mounts.length > 1) {
                for (IFile mnt : mounts) {
                    rootDir.includeRoot(mnt, null);
                }
            }
        }
        return rootDir;
    }

    protected static int getChildCount(File rootFile) {
        final File[] files = rootFile.listFiles();
        return (files != null) ? files.length : 0;
    }

    private static OSDirectory createOsDirectory(IFile file, OSDirectory factory) {
        if (factory != null) return factory.createOsDirectory(file, null, null);
        return new OSDirectory(file, null, null);
    }

    public static File getDefaultPhotoRoot() {
        return new File(Environment.getExternalStorageDirectory(),Environment.DIRECTORY_DCIM);
    }
}
