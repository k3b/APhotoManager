package de.k3b.android.util;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;

import java.io.File;

import de.k3b.android.androFotoFinder.Common;

/**
 * Created by k3b on 09.09.2015.
 */
public class IntentUtil implements Common {

    /** get uri from data. if there is no data from EXTRA_STREAM */
    @Nullable
    public static Uri getUri(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            Object stream = intent.getExtras().get(EXTRA_STREAM);
            if (stream != null) {
                uri = Uri.parse(stream.toString());
            }
        }
        return uri;
    }

    /** return null if uri is not file scheam */
    @Nullable
    public static File getFile(Uri uri) {
        if ((uri != null) && ("file".equals(uri.getScheme()))) {
            final String canonicalPath;
            return new File(uri.getPath());
        }
        return null;
    }

}
