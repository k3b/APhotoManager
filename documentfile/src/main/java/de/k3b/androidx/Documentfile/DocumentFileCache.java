package de.k3b.androidx.Documentfile;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DocumentFileCache {
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
        fileRootPath2RootDoc.put(result.pathPrefix, result);
        return result;
    }

    public @Nullable
    TreeDocumentFile find(@NonNull File file) {
        String path = getKey(file);
        RootTreeDocumentFile result = findRootTreeDocumentFile(path);
        if (result != null) {
            return result.find(path);
        }
        return result;
    }

    private @Nullable
    RootTreeDocumentFile findRootTreeDocumentFile(@NonNull String path) {
        for (String root : fileRootPath2RootDoc.keySet()) {
            if (path.startsWith(root)) {
                return fileRootPath2RootDoc.get(root);
            }
        }
        return null;
    }

    private static class RootTreeDocumentFile extends TreeDocumentFile {
        /**
         * without trailing "/"
         */
        private final Map<String, TreeDocumentFile> path2Doc = new HashMap<>();
        /**
         * including trailing "/"
         * i.e. /storage/emulated/0/ or /storage/abcd-1234/
         */
        String pathPrefix;

        RootTreeDocumentFile(@NonNull Context context, @NonNull Uri uri, @NonNull String pathPrefix) {
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
        public TreeDocumentFile find(@NonNull String path) {
            assert !path.endsWith("/");
            assert path.startsWith(pathPrefix);

            TreeDocumentFile result = path2Doc.get(path);
            if (result == null) {
                int pos = path.lastIndexOf("/");
                if (pos >= 0) {
                    String parentPath = path.substring(0, pos);
                    TreeDocumentFile parent = find(parentPath);
                    if (parent != null) {
                        String name = path.substring(pos);
                        result = (TreeDocumentFile) parent.findFile(name);
                        if (result != null) {
                            path2Doc.put(path, result);
                        }
                    }
                }
            }
            return result;
        }
    }
}
