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
package de.k3b.android.io;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import de.k3b.io.filefacade.FileFacade;

/**
 * Handles Translation from File to android specific DocumentFileUtils
 *
 * @// TODO: 25.03.2020 update cache if rename or delete dir
 */
public class DocumentFileTranslator {
    public static final String TAG = "k3b.DocFileTranslator";

    // used by android.support.v4.provider.DocumentFile
    public static final String TAG_DOCFILE = "DocumentFile";

    private static final String SAFROOTPREF_KEY_SAF_ROOT_PREFIX = "safroot-";
    private final Context context;

    /**
     * Mapping from known File to DocumentFile translation
     */
    private final Map<File, DocumentFile> dirCache = new HashMap<>();
    private static final File internalRootCandidate = new File("/storage/emulated/0");
    // for debugging
    private static int id = 1;
    private String mDebugPrefix;
    private static Root root = null;

    private DocumentFileTranslator(Context context, String namePrefix) {
        mDebugPrefix = namePrefix + "DocumentFileTranslator#" + (id++) + " ";
        this.context = context.getApplicationContext();
    }

    public static DocumentFileTranslator create(Context context) {
        if (root == null) {
            root = new Root(context.getApplicationContext());

        }
        return new DocumentFileTranslator(context, "").init();
    }

    private DocumentFileTranslator init() {
        File rootFile = getInternalStorageRoot();
        if (rootFile != null) {
            rootFile = rootFile.getAbsoluteFile();
            DocumentFile docRoot = DocumentFile.fromFile(rootFile);
            if ((docRoot != null) && docRoot.exists() && docRoot.isDirectory() && docRoot.canWrite()) {
                add(rootFile, docRoot);
            }
        }
        for (Map.Entry<String, String> enty : root.dir2uri.entrySet()) {
            add(new File(enty.getKey()), DocumentFile.fromTreeUri(context, Uri.parse(enty.getValue())));
        }
        return this;
    }

    private static File getInternalStorageRoot() {
        File dir = internalRootCandidate;
        if (!dir.exists()) dir = Environment.getExternalStorageDirectory();
        if ((dir != null) && dir.exists() && dir.isDirectory() && dir.canWrite()) return dir;

        return null;
    }

    public DocumentFileTranslator addRoot(File directory, Uri documentRootUri) {
        if (root.add(directory.getAbsolutePath(), documentRootUri.toString())) {
            add(directory, DocumentFile.fromTreeUri(context, documentRootUri));
        }
        return this;
    }

    public boolean isKnownRoot(File candidate) {
        if (candidate != null) {
            for (String rootFile : root.dir2uri.keySet()) {
                if (candidate.getAbsolutePath().startsWith(rootFile)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected DocumentFile getFromCache(File fileOrDir) {
        return dirCache.get(fileOrDir);
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
            if (FileFacade.debugLogFacade) {
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

    /**
     * gets existing DocumentFile that correspondws to fileOrDir
     * or null if not exists or no write permissions
     *
     * @param fileOrDir where DocumentFile is searched for
     * @param isDir     if null: return null if isDir is matchning
     * @return DocumentFile or null
     */
    public DocumentFile getDocumentFileOrDirOrNull(File fileOrDir, Boolean isDir) {
        DocumentFile result = null;
        final String context = FileFacade.debugLogFacade ? (mDebugPrefix + "getDocumentFile('" + fileOrDir.getAbsolutePath() +
                "') ") : null;
        try {
            result = getDocumentFileOrDirImpl(fileOrDir);
            if ((context != null) && (result == null)) {
                Log.i(TAG, context + "not found");
            }
        } catch (Exception ex) {
            Log.w(TAG, mDebugPrefix + "getDocumentFile('" + fileOrDir.getAbsolutePath() +
                    "') ", ex);

        }


        if ((result != null) && (isDir != null) && (result.isDirectory() != isDir)) {
            if (context != null) {
                Log.i(TAG, context + "wrong type isDirectory=" + result.isDirectory());
            }
            return null;
        }
        return result;
    }

    public InputStream openInputStream(DocumentFile doc) throws FileNotFoundException {
        if (doc != null) {
            return getContentResolver().openInputStream(doc.getUri());
        }
        return null;
    }

    public OutputStream createOutputStream(DocumentFile doc) throws FileNotFoundException {
        if (doc != null) {
            return getContentResolver().openOutputStream(doc.getUri());
        }
        return null;
    }

    public ContentResolver getContentResolver() {
        return context.getContentResolver();
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder().append(mDebugPrefix).append("[")
                .append(dirCache.size()).append("]: ").append(root);
        return result.toString();
    }

    public static String[] getRoots() {
        return (root != null) ? root.getRoots() : null;
    }

    private static class Root {
        private final Context context;
        private Map<String, String> dir2uri = new HashMap<>();

        public Root(Context context) {
            this.context = context.getApplicationContext();
            loadFromPrefs();
            if (FileFacade.debugLogFacade) {
                Log.i(TAG, "DocumentFileTranslator.Root.loaded(" + this + ")");
            }
        }

        protected void loadFromPrefs() {
            String fileUri = null;
            String docfileUri = null;
            try {
                final SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(this.context);
                int id = 0;
                do {
                    fileUri = prefs.getString(getPrefKeyFile(id), null);
                    docfileUri = prefs.getString(getPrefKeyDocfile(id), null);
                    id++;
                } while (add(fileUri, docfileUri));
            } catch (Exception ex) {
                Log.e(TAG, "err DocumentFileTranslator.Root(" + getPrefKey(id, "-") + "," + fileUri +
                        ", " + docfileUri + ") " + ex.getMessage(), ex);
            }
        }

        private static String getPrefKey(int id, String suffix) {
            return SAFROOTPREF_KEY_SAF_ROOT_PREFIX + id + suffix;
        }

        private static String getPrefKeyDocfile(int id) {
            return getPrefKey(id, "-docfile");
        }

        private static String getPrefKeyFile(int id) {
            return getPrefKey(id, "-file");
        }

        public boolean add(String fileUri, String docfileUri) {
            if ((fileUri != null) && (docfileUri != null)) {
                dir2uri.put(fileUri, docfileUri);
                saveToPrefs();
                return true;
            }
            return false;
        }

        private void saveToPrefs() {
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(context);

            SharedPreferences.Editor edit = prefs.edit();

            try {
                int id = 0;

                for (Map.Entry<String, String> enty : dir2uri.entrySet()) {
                    edit.putString(getPrefKeyFile(id), enty.getKey());
                    edit.putString(getPrefKeyDocfile(id), enty.getValue());
                    id++;
                }
                edit.remove(getPrefKeyFile(id));
                edit.remove(getPrefKeyDocfile(id));
            } catch (Exception ex) {
                Log.e(TAG, "err saveToPrefs(" + dir2uri + ")", ex);
            } finally {
                edit.commit();
                if (FileFacade.debugLogFacade) {
                    Log.i(TAG, "DocumentFileTranslator.Root.saveToPrefs(" + this + ")");
                }

            }
        }

        @Override
        public String toString() {
            final StringBuilder result = new StringBuilder().append("[");
            for (Map.Entry<String, String> enty : dir2uri.entrySet()) {
                result.append(enty.getKey()).append(" -> ").append(enty.getValue()).append(" ");
            }
            return result.append("]").toString();
        }

        public String[] getRoots() {
            return dir2uri.keySet().toArray(new String[dir2uri.size()]);
        }
    }
}
