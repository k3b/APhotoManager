/*
 * Copyright 2018 The Android Open Source Project (Licensed under the Apache License, Version 2.0)
 * Copyright 2021 by k3b under (Licensed under the GPL v3 (the "License"))
 */

package de.k3b.androidx.documentfile;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Sourcecode taken from Android api-29/documentfile-1.0.0-sources.jar
 */
@RequiresApi(19)
class DocumentsContractApi19Original {
    private static final String TAG = "DocumentFileEx";

    // DocumentsContract API level 24.
    private static final int FLAG_VIRTUAL_DOCUMENT = 1 << 9;

    protected DocumentsContractApi19Original() {
    }

    public static boolean isVirtual(Context context, Uri self) {
        if (!DocumentsContract.isDocumentUri(context, self)) {
            return false;
        }

        return (getFlags(context, self) & FLAG_VIRTUAL_DOCUMENT) != 0;
    }

    @Nullable
    public static String getName(Context context, Uri self) {
        return queryForString(context, self, DocumentsContract.Document.COLUMN_DISPLAY_NAME, null);
    }

    @Nullable
    private static String getRawType(Context context, Uri self) {
        return queryForString(context, self, DocumentsContract.Document.COLUMN_MIME_TYPE, null);
    }

    @Nullable
    public static String getType(Context context, Uri self) {
        final String rawType = getRawType(context, self);
        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(rawType)) {
            return null;
        } else {
            return rawType;
        }
    }

    public static long getFlags(Context context, Uri self) {
        return queryForLong(context, self, DocumentsContract.Document.COLUMN_FLAGS, 0);
    }

    public static boolean isDirectory(Context context, Uri self) {
        return DocumentsContract.Document.MIME_TYPE_DIR.equals(getRawType(context, self));
    }

    public static boolean isFile(Context context, Uri self) {
        final String type = getRawType(context, self);
        return !DocumentsContract.Document.MIME_TYPE_DIR.equals(type) && !TextUtils.isEmpty(type);
    }

    public static long lastModified(Context context, Uri self) {
        return queryForLong(context, self, DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0);
    }

    public static long length(Context context, Uri self) {
        return queryForLong(context, self, DocumentsContract.Document.COLUMN_SIZE, 0);
    }

    public static boolean canRead(Context context, Uri self) {
        // Ignore if grant doesn't allow read
        if (context.checkCallingOrSelfUriPermission(self, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        // Ignore documents without MIME
        return !TextUtils.isEmpty(getRawType(context, self));
    }

    public static boolean canWrite(Context context, Uri self) {
        // Ignore if grant doesn't allow write
        if (context.checkCallingOrSelfUriPermission(self, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        final String type = getRawType(context, self);
        final int flags = queryForInt(context, self, DocumentsContract.Document.COLUMN_FLAGS, 0);

        // Ignore documents without MIME
        if (TextUtils.isEmpty(type)) {
            return false;
        }

        // Deletable documents considered writable
        if ((flags & DocumentsContract.Document.FLAG_SUPPORTS_DELETE) != 0) {
            return true;
        }

        // Writable normal files considered writable
        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(type)
                && (flags & DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE) != 0) {
            // Directories that allow create considered writable
            return true;
        } else return !TextUtils.isEmpty(type)
                && (flags & DocumentsContract.Document.FLAG_SUPPORTS_WRITE) != 0;
    }

    public static boolean exists(Context context, Uri self) {
        final ContentResolver resolver = context.getContentResolver();

        Cursor c = null;
        try {
            c = resolver.query(self, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID}, null, null, null);
            return c.getCount() > 0;
        } catch (Exception e) {
            Log.w(TAG, "Failed query: " + e);
            return false;
        } finally {
            closeQuietly(c);
        }
    }

    @Nullable
    private static String queryForString(Context context, Uri self, String column,
                                         @Nullable String defaultValue) {
        final ContentResolver resolver = context.getContentResolver();

        Cursor c = null;
        try {
            c = resolver.query(self, new String[]{column}, null, null, null);
            if (c.moveToFirst() && !c.isNull(0)) {
                return c.getString(0);
            } else {
                return defaultValue;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed query: " + e);
            return defaultValue;
        } finally {
            closeQuietly(c);
        }
    }

    private static int queryForInt(Context context, Uri self, String column,
                                   int defaultValue) {
        return (int) queryForLong(context, self, column, defaultValue);
    }

    private static long queryForLong(Context context, Uri self, String column,
                                     long defaultValue) {
        final ContentResolver resolver = context.getContentResolver();

        Cursor c = null;
        try {
            c = resolver.query(self, new String[]{column}, null, null, null);
            if (c.moveToFirst() && !c.isNull(0)) {
                return c.getLong(0);
            } else {
                return defaultValue;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed query: " + e);
            return defaultValue;
        } finally {
            closeQuietly(c);
        }
    }

    private static void closeQuietly(@Nullable AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }
}
