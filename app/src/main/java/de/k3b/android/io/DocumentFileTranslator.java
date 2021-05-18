/*
 * Copyright (c) 2020-2021 by k3b.
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
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.io.filefacade.FileFacade;

/**
 * Handles Translation from File to android specific DocumentFileUtils
 * <p>
 * // TODO: 25.03.2020 update cache if rename or delete dir
 */
public class DocumentFileTranslator {
    public static final String TAG = "k3b.DocFileTranslator";
    public static final boolean debugLogSAFCache = true;

    // used by android.support.v4.provider.DocumentFile
    public static final String TAG_DOCFILE = "DocumentFile";

    private static final String SAFROOTPREF_KEY_SAF_ROOT_PREFIX = "safroot-";
    private final Context context;

    /**
     * Livecycle: Is incremented to indicate that {@link #getDirCache()} is invalid and must be reloaded
     */
    private static int dirCacheGlobalGeneratetionID = 1;

    /**
     * Livecycle: {@link #dirCacheGeneratetionID} != {@link #dirCacheGlobalGeneratetionID} means
     * that the {@link #getDirCache()} is invalid and must be reloaded
     */
    private int dirCacheGeneratetionID = dirCacheGlobalGeneratetionID;

    /**
     * Mapping from known File to DocumentFile-Directory translation
     */
    private final Map<File, DocumentFile> dirCache = new HashMap<>();
    protected DocumentFileCache documentFileCache = new DocumentFileCache();
    private static final File internalRootCandidate = new File("/storage/emulated/0");
    // for debugging
    private static int id = 1;
    private final String mDebugPrefix;
    private static Root root = null;

    private DocumentFileTranslator(Context context, String namePrefix) {
        mDebugPrefix = namePrefix + "DocumentFileTranslator#" + (id++) + " ";
        this.context = context.getApplicationContext();
    }

    public static DocumentFileTranslator create(Context context) {
        if (root == null) {
            root = new Root(context.getApplicationContext());

        }
        return new DocumentFileTranslator(context, "").initCache();
    }

    /**
     * Livecycle: Indicate that {@link #getDirCache()} is invalid and must be reloaded
     *
     * @param eventSourceOrNull the class that initiated the invalidate
     */
    public static void invalidateDirCache(Object eventSourceOrNull) {
        if (eventSourceOrNull instanceof DocumentFileTranslator) {
            ((DocumentFileTranslator) eventSourceOrNull).dirCacheGeneratetionID++;
        }
        dirCacheGlobalGeneratetionID++;
    }

    private DocumentFileTranslator initDirCache() {
        File rootFile = getInternalStorageRoot();
        String debugContext = "init";
        if (rootFile != null) {
            rootFile = rootFile.getAbsoluteFile();
            DocumentFile docRoot = DocumentFile.fromFile(rootFile);
            if ((docRoot != null) && docRoot.exists() && docRoot.isDirectory() && docRoot.canWrite()) {
                add2DirCache(debugContext, rootFile, docRoot);
            }
        }
        for (Map.Entry<String, String> enty : root.dir2uri.entrySet()) {
            add2DirCache(debugContext, new File(enty.getKey()), DocumentFile.fromTreeUri(context, Uri.parse(enty.getValue())));
        }
        return this;
    }

    private static File getInternalStorageRoot() {
        File dir = internalRootCandidate;
        if (!dir.exists()) dir = Environment.getExternalStorageDirectory();
        if ((dir != null) && dir.exists() && dir.isDirectory() && dir.canWrite()) return dir;

        return null;
    }

    private DocumentFileTranslator initCache() {
        initDirCache();
        // !!! one DocumentFileCache per dir ????
        documentFileCache = new DocumentFileCache();
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

    public DocumentFileTranslator addRoot2DirCache(File directory, Uri documentRootUri) {
        if (root.add(directory.getAbsolutePath(), documentRootUri.toString())) {
            add2DirCache("addRoot2DirCache", directory, DocumentFile.fromTreeUri(context, documentRootUri));
            invalidateDirCache(this);
            root.saveToPrefs();
        }
        return this;
    }

    private Map<File, DocumentFile> getDirCache() {
        if (dirCacheGeneratetionID != dirCacheGlobalGeneratetionID) {
            // dirCache was invalidated from outside. Must be re-created
            dirCache.clear();
            dirCacheGeneratetionID = dirCacheGlobalGeneratetionID;
            initCache();
        }
        return dirCache;
    }

    private DocumentFile getFromDirCache(String debugContext, File fileOrDir, boolean isDir) {
        DocumentFile result = getDirCache().get(fileOrDir);
        if (result == null && DocumentFileTranslator.debugLogSAFCache) {
            Log.i(FileFacade.LOG_TAG,
                    ((debugContext == null) ? "" : debugContext)
                            + this.getClass().getSimpleName()
                            + ".getFromCache(" + fileOrDir
                            + ",dir=" + isDir
                            + ") ==> failed");

        }
        return result;
    }

    /**
     * add mapping from file-sdcard, -usbstick, -networkstorage to documentFileDirRoot
     */
    private DocumentFileTranslator add2DirCache(String debugContext, File directory, DocumentFile documentFileDir) {
        if ((documentFileDir != null) && documentFileDir.isDirectory()) {
            if (FileFacade.debugLogSAFFacade || DocumentFileTranslator.debugLogSAFCache) {
                Uri uri = (documentFileDir != null) ? documentFileDir.getUri() : null;
                Log.d(TAG, mDebugPrefix + "dirCache.put(" + directory +
                        " -> " + uri + ") because of " + debugContext);
            }
            getDirCache().put(directory, documentFileDir);
        }
        return this;
    }

    private DocumentFile getDocumentFileOrDirImpl(String debugContext, File fileOrDir, boolean isDir) {
        DocumentFile result = null;
        if (fileOrDir != null) {
            result = getFromDirCache(debugContext, fileOrDir, isDir);
            if (result == null) {
                DocumentFile parent = getDocumentFileOrDirImpl(debugContext, fileOrDir.getParentFile(), true);
                if (parent != null) {
                    result = findFile(debugContext, parent, fileOrDir, isDir);
                }
            }
        }
        return result;
    }

    private DocumentFile findFile(String debugContext, DocumentFile parentDoc, File fileOrDir, boolean isDir) {
        String displayName = fileOrDir.getName();
        File parentFile = fileOrDir.getParentFile();
        if (isDir) {
            // The original parentDoc.findFile(fileOrDir.getName()) is implemented
            // as expensive, frequent called parentDoc.listFiles().
            // Optimisation: Sideeffect fill the dir cache while searching for file.
            DocumentFile foundDoc = null;
            for (DocumentFile childDoc : parentDoc.listFiles()) {
                String childDocName = childDoc.getName();
                if (foundDoc == null && displayName.equals(childDocName)) {
                    foundDoc = childDoc;
                }
                if (childDoc.isDirectory()) {
                    add2DirCache(debugContext + " findFile ", new File(parentFile, childDocName), foundDoc);
                }
            }
            return foundDoc;
        } else {
            return documentFileCache.findFile(debugContext + " findFile ", parentDoc, parentFile, displayName);
        }
    }

    /**
     * corresponds to {@link File#mkdirs()} if directory does not exist.
     *
     * @return the found or created directory
     */
    public DocumentFile getOrCreateDirectory(String debugContext, File directory) {
        DocumentFile result = null;
        if (directory != null) {
            result = getFromDirCache(debugContext, directory, true);
            if (result == null) {
                DocumentFile parent = getOrCreateDirectory(debugContext, directory.getParentFile());
                if ((parent != null) && parent.isDirectory()) {
                    result = findFile(debugContext, parent, directory, true);

                    if (result == null) {
                        if ((Global.android_DocumentFile_find_cache && FileFacade.debugLogSAFFacade) || DocumentFileTranslator.debugLogSAFCache) {
                            Log.i(FileFacade.LOG_TAG, this.getClass().getSimpleName()
                                    + " CreateDirectory: enableCache(false)");
                        }

                        Global.android_DocumentFile_find_cache = false;
                        result = parent.createDirectory(directory.getName());
                        add2DirCache(debugContext + " created dir ", directory, result);
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
    public DocumentFile getDocumentFileOrDirOrNull(String debugContext, File fileOrDir, Boolean isDir) {
        DocumentFile result = null;
        String path = fileOrDir != null ? fileOrDir.getAbsolutePath() : "";
        try {
            final String context = (FileFacade.debugLogSAFFacade || DocumentFileTranslator.debugLogSAFCache)
                    ? (debugContext + ":" + mDebugPrefix + "getDocumentFile('" + path + "') ")
                    : null;

            result = getDocumentFileOrDirImpl(context, fileOrDir, isDir == Boolean.TRUE);
            if ((context != null) && (result == null)) {
                Log.i(TAG, context + "not found");
            }
        } catch (Exception ex) {
            Log.w(TAG, debugContext + ":" + mDebugPrefix + "getDocumentFile('" + path +
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

    public InputStream openInputStream(Uri readUri) throws FileNotFoundException {
        return context.getContentResolver().openInputStream(readUri);
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

    public static void clearCache() {
        if (root != null) {
            root.dir2uri.clear();
        }
    }

    public static String pathFromUri(String uri) {
        if (root == null) return null;
        return root.pathFromUri(uri);
    }

    private static class Root {
        private final Context context;
        private final Map<String, String> dir2uri = new HashMap<>();

        public Root(Context context) {
            this.context = context.getApplicationContext();
            loadFromPrefs();
            if (FileFacade.debugLogSAFFacade || DocumentFileTranslator.debugLogSAFCache) {
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
                if (FileFacade.debugLogSAFFacade || DocumentFileTranslator.debugLogSAFCache) {
                    Log.i(TAG, "DocumentFileTranslator.Root.saveToPrefs(" + this + ")");
                }

            }
        }

        public String pathFromUri(String uri) {
            for (Map.Entry<String, String> entry : dir2uri.entrySet()) {
                if (uri.startsWith(entry.getValue())) {
                    String[] relPath = URLDecoder.decode(uri.substring(entry.getValue().length())).split(":");
                    return new File(entry.getKey(), relPath[relPath.length - 1]).getAbsolutePath();
                }
            }
            return null;
        }

        @Override
        public String toString() {
            final StringBuilder result = new StringBuilder().append("[");
            for (Map.Entry<String, String> entry : dir2uri.entrySet()) {
                result.append(entry.getKey()).append(" -> ").append(entry.getValue()).append(" ");
            }
            return result.append("]").toString();
        }

        public String[] getRoots() {
            return dir2uri.keySet().toArray(new String[dir2uri.size()]);
        }

        public void clearCache() {
            dir2uri.clear();
        }
    }
}
