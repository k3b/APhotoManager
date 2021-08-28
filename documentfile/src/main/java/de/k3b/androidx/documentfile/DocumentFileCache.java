/*
 * Copyright 2021 by k3b (Licensed under the GPL v3+ (the "License"))
 */
package de.k3b.androidx.documentfile;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Once all android roots (sd-card, internal-memory,usb-memory) are
 * {@link DocumentFileCache#registerRoodDir(Context, Uri, File)}-ed and Android-Write permission is granted
 * DocumentFileCache can translate File to android-TreeDocumentFile
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DocumentFileCache {
    static final String TAG = DocumentFileEx.TAG;
    public static boolean debug = false;
    public static boolean debug_find = false;

    /**
     * Translates file-root-dir to saf-DocumentFile-root-dir vor every root-storage-device-directory
     */
    private final Map<String, RootTreeDocumentFile> fileRootDirPath2RootDoc = new HashMap<>();

    /**
     * translates a dirFile to a key for hash-maps
     */
    private static @NonNull
    String getKey(@NonNull File dirFile) {
        String result = dirFile.getAbsolutePath().toLowerCase();
        if (!result.endsWith("/")) result += "/";

        assert result.startsWith("/");
        assert result.endsWith("/");
        return result;
    }

    /**
     * Register an Android file system rootDirUri with a corresponding rootDirFile.
     */
    public @NonNull
    TreeDocumentFile registerRoodDir(@NonNull Context context, @NonNull Uri rootDirUri, @NonNull File rootDirFile) {
        RootTreeDocumentFile rootDoc = new RootTreeDocumentFile(context, rootDirUri, rootDirFile);
        if (debug) {
            Log.i(TAG, "register([" + rootDirFile + "]) => '" + rootDirUri + "' in " + this);
        }
        fileRootDirPath2RootDoc.put(rootDoc.pathPrefix, rootDoc);
        return rootDoc;
    }

    /**
     * Translates from dirFile to TreeDocumentFile
     */
    public @Nullable
    TreeDocumentFile findDirectory(@NonNull File dirFile) {
        String path = getKey(dirFile);
        RootTreeDocumentFile root = findRootDirTreeDocumentFile(path);
        if (root != null) {
            TreeDocumentFile result = root.findDir(path);
            if (debug_find) {
                Log.d(TAG, "find([" + dirFile + "]) => [" + result + "] in " + this);
            }
            return result;
        }
        Log.w(TAG, "Failed to find root for " + dirFile);
        return null;
    }

    private @Nullable
    RootTreeDocumentFile findRootDirTreeDocumentFile(@NonNull String path) {
        for (Map.Entry<String, RootTreeDocumentFile> entry : fileRootDirPath2RootDoc.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** TreeDocumentFile for a storage root that has a cache of known storage-sub-dirs */
    static class RootTreeDocumentFile extends TreeDocumentFile {
        /**
         * without trailing "/"
         */
        private final Map<String, TreeDocumentFile> dirFilePath2Doc = new HashMap<>();
        /**
         * including trailing "/"
         * i.e. /storage/emulated/0/ or /storage/abcd-1234/
         */
        String pathPrefix;

        private RootTreeDocumentFile(@NonNull Context context, @NonNull Uri rootDirUri, @NonNull File rootDirFile) {
            super(null, context, rootDirUri);
            this.pathPrefix = getKey(rootDirFile);
            dirFilePath2Doc.put(withoutTrailing(this.pathPrefix), this);
        }

        @NonNull
        private static String withoutTrailing(@NonNull String path) {
            assert path.endsWith("/");
            String result = path.substring(0, path.length() - 1);
            assert !path.endsWith("/");
            return result;
        }

        @Nullable
        protected TreeDocumentFile findDir(@NonNull String dirFilePath) {
            assert !dirFilePath.endsWith("/");
            assert dirFilePath.startsWith(pathPrefix);

            String state = "";
            TreeDocumentFile result = dirFilePath2Doc.get(dirFilePath);
            if (result == null) {
                int pos = dirFilePath.lastIndexOf("/");
                if (pos >= 0) {
                    String parentPath = dirFilePath.substring(0, pos);
                    TreeDocumentFile parentDir = findDir(parentPath);
                    if (parentDir != null) {
                        String name = dirFilePath.substring(pos);
                        result = parentDir.findDirByName(name);
                        if (result != null) {
                            state = "added from filesystem";
                            dirFilePath2Doc.put(dirFilePath, result);
                        } else {
                            state = "not in filesystem";
                        }
                    }
                }
            } else {
                state = "found in cache";
            }
            if (debug_find) {
                Log.d(TAG, "find('" + dirFilePath + "') => " + state + "[" + result + "] in " + this);
            }
            return result;
        }

        @Override
        public String toString() {
            return "'" + pathPrefix + "'(" + this.dirFilePath2Doc.size() + ") => " + super.toString();
        }
    }

}