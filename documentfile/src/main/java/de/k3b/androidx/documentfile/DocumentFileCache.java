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

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DocumentFileCache {
    static final String TAG = DocumentFileEx.TAG;
    public static boolean debug = false;
    public static boolean debug_find = false;

    private final Map<String, RootTreeDocumentFile> fileRootPath2RootDoc = new HashMap<>();

    private static @NonNull
    String getKey(@NonNull File file) {
        String result = file.getAbsolutePath().toLowerCase();
        if (!result.endsWith("/")) result += "/";

        assert result.startsWith("/");
        assert result.endsWith("/");
        return result;
    }

    /**
     * Android file system consist of a tree of root file systems that map to a File-Root
     */
    public @NonNull
    TreeDocumentFile register(@NonNull Context context, @NonNull Uri uri, @NonNull File file) {
        RootTreeDocumentFile result = new RootTreeDocumentFile(context, uri, getKey(file));
        if (debug) {
            Log.i(TAG, "register([" + file + "]) => '" + uri + "' in " + this);
        }
        fileRootPath2RootDoc.put(result.pathPrefix, result);
        return result;
    }

    public @Nullable
    TreeDocumentFile find(@NonNull File file) {
        String path = getKey(file);
        RootTreeDocumentFile root = findRootTreeDocumentFile(path);
        if (root != null) {
            TreeDocumentFile result = root.find(path);
            if (debug_find) {
                Log.d(TAG, "find([" + file + "]) => [" + result + "] in " + this);
            }
            return result;
        }
        Log.w(TAG, "Failed to find root for " + file);
        return null;
    }

    private @Nullable
    RootTreeDocumentFile findRootTreeDocumentFile(@NonNull String path) {
        for (Map.Entry<String, RootTreeDocumentFile> entry : fileRootPath2RootDoc.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    static class RootTreeDocumentFile extends TreeDocumentFile {
        /**
         * without trailing "/"
         */
        private final Map<String, TreeDocumentFile> path2Doc = new HashMap<>();
        /**
         * including trailing "/"
         * i.e. /storage/emulated/0/ or /storage/abcd-1234/
         */
        String pathPrefix;

        private RootTreeDocumentFile(@NonNull Context context, @NonNull Uri uri, @NonNull String pathPrefix) {
            super(null, context, uri);
            assert pathPrefix.endsWith("/");
            this.pathPrefix = pathPrefix;
            path2Doc.put(withoutTrailing(pathPrefix), this);
        }

        @NonNull
        private static String withoutTrailing(@NonNull String path) {
            assert path.endsWith("/");
            String result = path.substring(0, path.length() - 1);
            assert !path.endsWith("/");
            return result;
        }

        @Nullable
        protected TreeDocumentFile find(@NonNull String path) {
            assert !path.endsWith("/");
            assert path.startsWith(pathPrefix);

            String state = "";
            TreeDocumentFile result = path2Doc.get(path);
            if (result == null) {
                int pos = path.lastIndexOf("/");
                if (pos >= 0) {
                    String parentPath = path.substring(0, pos);
                    TreeDocumentFile parent = find(parentPath);
                    if (parent != null) {
                        String name = path.substring(pos);
                        result = parent.findDirByName(name);
                        if (result != null) {
                            state = "added from filesystem";
                            path2Doc.put(path, result);
                        } else {
                            state = "not in filesystem";
                        }
                    }
                }
            } else {
                state = "found in cache";
            }
            if (debug_find) {
                Log.d(TAG, "find('" + path + "') => " + state + "[" + result + "] in " + this);
                return null;
            }
            return result;
        }

        @Override
        public String toString() {
            return "'" + pathPrefix + "'(" + this.path2Doc.size() + ") => " + super.toString();
        }
    }
}
