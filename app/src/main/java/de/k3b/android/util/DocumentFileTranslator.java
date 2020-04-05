/*
 * Copyright (c) 2020 by k3b.
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
package de.k3b.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles Translation from File to android specific DocumentFileUtils
 *
 * @// TODO: 25.03.2020 update cache if rename or delete dir
 */
public class DocumentFileTranslator {
    public static final String TAG = "k3b.DocFileUtils";
    private static final String SAFROOTPREF_KEY_SAF_ROOT_PREFIX = "safroot-";
    private static DocumentFileTranslator rootsettings = null;
    private final Context context;
    public static final boolean debugDocFile = true;

    /**
     * Mapping from known File to DocumentFile translation
     */
    private final Map<File, DocumentFile> dirCache = new HashMap<>();
    private PrefIO prefIO = null;
    private static final File internalRootCandidate = new File("/storage/emulated/0");
    // for debugging
    private static int id = 1;
    private String mDebugPrefix;

    private DocumentFileTranslator(Context context, String namePrefix) {
        mDebugPrefix = namePrefix + "DocumentFileTranslator#" + (id++) + " ";
        this.context = context;
    }

    public static DocumentFileTranslator create(Context context) {
        if (rootsettings == null) {
            rootsettings = new DocumentFileTranslator(context.getApplicationContext(), "Root-");
            rootsettings.loadFromPrefs();
        }
        return new DocumentFileTranslator(context, "").init();
    }

    private void loadFromPrefs() {
        prefIO = new PrefIO();
        prefIO.loadFromPrefs();
    }

    private static File getInternalStorageRoot() {
        File dir = internalRootCandidate;
        if (!dir.exists()) dir = Environment.getExternalStorageDirectory();
        if ((dir != null) && dir.exists() && dir.isDirectory() && dir.canWrite()) return dir;

        return null;
    }

    private DocumentFileTranslator init() {
        if ((rootsettings != null) && (this != rootsettings)) {
            for (Map.Entry<File, DocumentFile> enty : rootsettings.dirCache.entrySet()) {
                add(enty.getKey(), enty.getValue());
            }
        }
        return this;
    }

    public DocumentFileTranslator addRoot(File directory, DocumentFile documentFileDir) {
        add(directory, documentFileDir);
        if (prefIO != null) {
            // i am root so save data
            prefIO.saveToPrefs();
        } else {
            rootsettings.addRoot(directory, documentFileDir);
        }

        return this;
    }

    protected DocumentFile getFromCache(File fileOrDir) {
        return dirCache.get(fileOrDir);
    }

    private boolean add(String fileUri, String docfileUri) {
        if ((fileUri != null) && (docfileUri != null)) {
            add(new File(fileUri), DocumentFile.fromTreeUri(context, Uri.parse(docfileUri)));
            return true;
        }
        return false;
    }

    /**
     * add mapping from file-sdcard, -usbstick, -networkstorage to documentFileDirRoot
     *
     * @param directory
     * @param documentFileDir
     * @return
     */
    public DocumentFileTranslator add(File directory, DocumentFile documentFileDir) {
        if ((documentFileDir != null) && documentFileDir.isDirectory()) {
            if (debugDocFile) {
                Uri uri = (documentFileDir != null) ? documentFileDir.getUri() : null;
                Log.d(TAG, mDebugPrefix + "dirCache.put(" + directory +
                        " -> " + uri + ")");
            }
            dirCache.put(directory, documentFileDir);
        }
        return this;
    }

    private DocumentFile getDocumentFileOrDirImpl(File fileOrDir) {
        DocumentFile result = null;
        if (fileOrDir != null) {
            result = getFromCache(fileOrDir);
            if (result == null) {
                DocumentFile parent = getDocumentFileOrDirImpl(fileOrDir.getParentFile());
                if (parent != null) {
                    result = parent.findFile(fileOrDir.getName());

                    if ((result != null) && result.isDirectory()) {
                        add(fileOrDir, result);
                    }
                }
            }
        }
        return result;
    }

    /**
     * corresponds to {@link File#mkdirs()} if directory does not exist.
     *
     * @return the found or created directory
     */
    public DocumentFile getOrCreateDirectory(File directory) {
        DocumentFile result = null;
        if (directory != null) {
            result = getFromCache(directory);
            if (result == null) {
                DocumentFile parent = getOrCreateDirectory(directory.getParentFile());
                if ((parent != null) && parent.isDirectory()) {
                    result = parent.findFile(directory.getName());

                    if (result == null) {
                        result = parent.createDirectory(directory.getName());
                    }

                    if (result != null) {
                        add(directory, result);
                    }
                }
            }
        }
        return result;
    }

    public DocumentFile getDocumentFileOrDir(File fileOrDir, boolean isDir) {
        DocumentFile result = null;
        final String context = mDebugPrefix + "getDocumentFile('" + fileOrDir.getAbsolutePath() +
                "') ";
        try {
            result = getDocumentFileOrDirImpl(fileOrDir);
            if (result == null) {
                Log.i(TAG, context + "not found");
            }
        } catch (Exception ex) {
            Log.w(TAG, context, ex);

        }
        if ((result != null) && (result.isDirectory() != isDir)) {
            Log.i(TAG, context + "wrong type isDirectory=" + result.isDirectory());
            return null;
        }
        return result;
    }

    private class PrefIO {
        private void saveToPrefs() {
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(context);

            SharedPreferences.Editor edit = prefs.edit();

            try {
                int id = 0;

                for (Map.Entry<File, DocumentFile> enty : dirCache.entrySet()) {
                    edit.putString(getPrefKeyFile(id), enty.getKey().getAbsolutePath());
                    edit.putString(getPrefKeyDocfile(id), enty.getValue().getUri().toString());
                    id++;
                }
                edit.remove(getPrefKeyFile(id));
                edit.remove(getPrefKeyDocfile(id));
            } catch (Exception ex) {
                Log.e(TAG, mDebugPrefix + "err saveToPrefs(" + dirCache + ")", ex);
            } finally {
                edit.commit();
            }
        }

        private String getPrefKeyDocfile(int id) {
            return getPrefKey(id, "-docfile");
        }

        private String getPrefKeyFile(int id) {
            return getPrefKey(id, "-file");
        }

        private String getPrefKey(int id, String suffix) {
            return SAFROOTPREF_KEY_SAF_ROOT_PREFIX + id + suffix;
        }

        private void loadFromPrefs() {
            int id = 0;
            String docfileUri = null;
            String fileUri = null;

            File root = getInternalStorageRoot();
            if (root != null) {
                root = root.getAbsoluteFile();
                DocumentFile docRoot = DocumentFile.fromFile(root);
                if ((docRoot != null) && docRoot.exists() && docRoot.isDirectory() && docRoot.canWrite()) {
                    add(root, docRoot);
                }
            }
            try {
                final SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(context);

                do {
                    fileUri = prefs.getString(getPrefKeyFile(id), null);
                    docfileUri = prefs.getString(getPrefKeyDocfile(id), null);
                    id++;
                } while (add(fileUri, docfileUri));
            } catch (Exception ex) {
                Log.e(TAG, mDebugPrefix + "err loadFromPrefs(" + getPrefKey(id, "-") + "," + fileUri +
                        ", " + docfileUri + ")", ex);
            }

        }

    }
}
