/*
 * Copyright (c) 2015-2021 by k3b.
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
 
package de.k3b.android.androFotoFinder.imagedetail;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.net.Uri;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.IMediaRepositoryApi;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.android.util.ClipboardUtil;
import de.k3b.android.widget.ActivityWithCallContext;
import de.k3b.database.QueryParameter;
import de.k3b.io.DateUtil;
import de.k3b.io.XmpFile;
import de.k3b.io.filefacade.IFile;
import de.k3b.media.ExifInterfaceEx;
import de.k3b.media.PhotoPropertiesImageReader;
import de.k3b.media.XmpSegment;

/**
 * Creates a popup dialog that displays the MetaData.
 * Created by k3b on 15.07.2015.
 */
public class ImageDetailMetaDialogBuilder {
    private static final String NL = "\n";

    public static Dialog createImageDetailDialog(Activity context, IFile file, Uri imageUri, long imageId,
                                                 QueryParameter query,
                                                 long offset, Object... moreBlocks) {
        StringBuilder result = new StringBuilder();

        Object fileId = (file == null) ? imageUri : file;
        if (fileId == null) fileId = "";
        result
                .append(imageId)
                .append(":").append(fileId)
                .append("\n");
        appendExifInfo(context, result, file, imageUri, imageId);
        appendQueryInfo(result, query, offset);

        if ((moreBlocks != null) && (moreBlocks.length > 0)) {
            for (Object subBlock : moreBlocks) {
                if (subBlock != null) {
                    append(result, "\n");
                    append(result, fileId.toString());
                }
            }
        }
        return createImageDetailDialog(context, fileId.toString(), result.toString());
    }

    private static final String line = "------------------";

    private static void appendQueryInfo(StringBuilder result, QueryParameter query, long offset) {
        if (query != null) {
            result.append(NL).append(line).append(NL);
            result.append(NL).append("#").append(offset).append(": ").append(query.toSqlString()).append(NL).append(NL);
        }
    }

    private static void append(StringBuilder result, String block) {
        if (block != null) {
            result.append(NL).append(line).append(NL);
            result.append(NL).append(block).append(NL);
        }
    }

    private static final String dateFields = (","
            + TagSql.SQL_COL_DATE_ADDED + ","
            + TagSql.SQL_COL_EXT_XMP_LAST_MODIFIED_DATE + ","
            + TagSql.SQL_COL_LAST_MODIFIED
            + ",").toLowerCase();

    public static Dialog createImageDetailDialog(final Activity context, String title, String block, Object... moreBlocks) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        ScrollView sv = new ScrollView(context);
        sv.setVerticalScrollBarEnabled(true);
        final TextView view = new TextView(context);
        view.setId(R.id.action_details);

        StringBuilder result = new StringBuilder(block);
        if ((moreBlocks != null) && (moreBlocks.length > 0)) {
            for (Object subBlock : moreBlocks) {
                if (subBlock != null) {
                    append(result, subBlock.toString());
                }
            }
        }
        if (context instanceof ActivityWithCallContext) {
            append(result, ((ActivityWithCallContext) context).getCallContext());
        }
        view.setText(result.toString());

        sv.addView(view);
        builder.setView(sv);

        builder.setPositiveButton(android.R.string.ok, null); /*, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                this.finalize();
            }
        });*/

        builder.setNeutralButton(android.R.string.copy, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ClipboardUtil.addDirToClipboard(context, view.getText(), false);
            }
        });

        return builder.create();
    }

    private static void appendExifInfo(Activity context, StringBuilder result, IFile jpegFile, Uri imageUri, long currentImageId) {
        try {
            appendExifInfoFromFile_android(context, result, jpegFile, imageUri);

            appendExifFromFile_drewnoakes(context, result, jpegFile, imageUri);

            if (jpegFile != null) {
                // #84 show long and short xmp file
                appendXmpFromFile(result, XmpFile.getSidecar(jpegFile, true));
                appendXmpFromFile(result, XmpFile.getSidecar(jpegFile, false));
            }

            if (currentImageId != 0 || jpegFile != null) {
                String filePathOrNull = jpegFile != null ? jpegFile.getCanonicalPath() : null;
                appendDbInfo(result, FotoSql.getMediaLocalDatabase(), currentImageId, filePathOrNull,
                        "LocalDatabase");
                appendDbInfo(result, FotoSql.getMediaDBApi(), currentImageId, filePathOrNull,
                        TagSql.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void appendDate(StringBuilder result, String key, Object value) {
        if (dateFields.contains(key.toLowerCase())) {
            appendDate(result, value, 1000); // value in secs since 1970. Java needs millisecs
        } else if (TagSql.SQL_COL_DATE_TAKEN.compareToIgnoreCase(key) == 0) {
            appendDate(result, value, 1); // value in millisecs since 1970. same as Java
        }
    }

    private static void appendDate(StringBuilder result, Object value, int factor) {
        try {
            long milliSecs = Long.parseLong(value.toString());
            Date d = (milliSecs == 0) ? null : new Date(milliSecs * factor);
            if (d != null) {
                result.append("=").append(DateUtil.toIsoDateTimeString(d)).append("z");
            }
        } catch (Exception ignore) {

        }
    }

    private static void appendDbInfo(StringBuilder result, IMediaRepositoryApi api, long currentImageId, String filePathOrNull, String callContext) {
        if (api != null) {
            result.append(NL).append(line).append(NL);

            String search = callContext + "(id=" + currentImageId + ", path='" + filePathOrNull + "')";
            ContentValues dbContent = api.getDbContent(currentImageId, filePathOrNull);
            if (dbContent != null) {
                appendFileMessage(result, search, NL);
                // sort by keys
                List<String> sortedKeys = new ArrayList(dbContent.keySet());
                Collections.sort(sortedKeys);
                for (String key : sortedKeys) {
                    Object value = dbContent.get(key);
                    String sValue = (value != null) ? value.toString() : null;
                    if ((sValue != null) && (sValue.length() > 0) && (sValue.compareTo("0") != 0)) {
                        // show only non empty values
                        result.append(key).append("=").append(sValue);
                        appendDate(result, key, value);
                        result.append(NL);
                    }
                }
            } else {
                appendFileNotFoundMessage(result, search);
            }
        }
    }

    private static void appendExifFromFile_drewnoakes(Activity context, StringBuilder builder, IFile file, Uri imageUri) throws IOException {
        PhotoPropertiesImageReader meta = null;
        Object fileId = file;
        if (file != null && file.exists()) {
            meta = new PhotoPropertiesImageReader().load(
                    file, file.openInputStream(), null, "ImageDetailMetaDialogBuilder");
        } else if (imageUri != null) {
            fileId = imageUri;
            meta = new PhotoPropertiesImageReader().load(
                    (IFile) null,
                    context.getContentResolver().openInputStream(imageUri),
                    null, "ImageDetailMetaDialogBuilder(" +
                            imageUri + ")");
        }

        if (meta != null) {
            appendFileMessage(builder, fileId, NL);

            builder.append(meta.toString());
            builder.append(NL).append(line).append(NL);
        } else {
            appendFileNotFoundMessage(builder, fileId);
        }
    }

    private static void appendXmpFromFile(StringBuilder builder, IFile file) throws IOException {
        if (file.exists()) {
            XmpSegment meta = new XmpSegment();
            meta.load(file.openInputStream(), "ImageDetailMetaDialogBuilder");
            appendFileMessage(builder, file, NL);
            meta.appendXmp(null, builder);
            builder.append(NL).append(line).append(NL);
        } else {
            appendFileNotFoundMessage(builder, file);
        }
    }

    private static void appendExifInfoFromFile_android(Activity context, StringBuilder builder, IFile filepath, Uri imageUri) throws IOException {
        Object fileId = filepath;
        ExifInterfaceEx exif = null;
        if (filepath != null && filepath.exists()) {
            exif = ExifInterfaceEx.create(filepath, null, null, "ImageDetailMetaDialogBuilder.getExifInfo_android");
        } else if (imageUri != null) {
            fileId = imageUri;
            exif = ExifInterfaceEx.create(
                    (IFile) null,
                    context.getContentResolver().openInputStream(imageUri),
                    null, "ImageDetailMetaDialogBuilder.getExifInfo_android(" +
                            imageUri + ")");
        }

        if (exif != null) {
            builder.append(NL).append(line).append(NL);
            appendFileMessage(builder, fileId, NL);
            if (exif.isValidJpgExifFormat()) builder.append(exif.getDebugString(NL));

            builder.append(NL).append(line).append(NL);
        }
    }

    private static void appendFileNotFoundMessage(StringBuilder builder, Object file) {
        appendFileMessage(builder, file, " not found.");
    }

    private static void appendFileMessage(StringBuilder builder, Object file, String message) {
        builder.append(NL).append(file).append(message).append(NL);
    }
}
