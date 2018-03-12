/*
 * Copyright (c) 2015 by k3b.
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
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.io.DateUtil;
import de.k3b.media.ExifInterfaceEx;
import de.k3b.database.QueryParameter;
import de.k3b.media.ImageMetaReader;
import de.k3b.io.FileCommands;
import de.k3b.media.XmpSegment;

/**
 * Creates a popup dialog that displays the MetaData.
 * Created by k3b on 15.07.2015.
 */
public class ImageDetailMetaDialogBuilder {
    private static final String NL = "\n";

    public static Dialog createImageDetailDialog(Activity context, String filePath, long imageId,
                                                 QueryParameter query,
                                                 long offset) {
        StringBuilder result = new StringBuilder();
        result
                .append(imageId)
                .append(":").append(filePath)
                .append("\n");
        appendExifInfo(result, context, filePath, imageId);
        appendQueryInfo(result, query, offset);
        return createImageDetailDialog(context, filePath, result.toString());
    }

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

    public static Dialog createImageDetailDialog(Activity context, String title, String block, Object... moreBlocks) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        ScrollView sv = new ScrollView(context);
        sv.setVerticalScrollBarEnabled(true);
        TextView view = new TextView(context);

        if ((moreBlocks != null) && (moreBlocks.length > 0)) {
            StringBuilder result = new StringBuilder(block);
            for (Object subBlock : moreBlocks) {
                if (subBlock != null) {
                    append(result, subBlock.toString());
                }
            }
            view.setText(result.toString());
        } else {
            view.setText(block);
        }

        sv.addView(view);
        builder.setView(sv);

        builder.setPositiveButton(android.R.string.ok, null); /*, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                this.finalize();
            }
        });*/

        return builder.create();
    }

    private static final String dateFields = (","
            + TagSql.SQL_COL_DATE_ADDED + ","
            + TagSql.SQL_COL_EXT_XMP_LAST_MODIFIED_DATE + ","
            + TagSql.SQL_COL_LAST_MODIFIED
            + ",").toLowerCase();

    private static void appendExifInfo(StringBuilder result, Activity context, String filepath, long currentImageId) {
        try {
            getExifInfo_android(result, filepath);

            File jpegFile = new File(filepath);
            addExif(result, jpegFile);

            // #84 show long and short xmp file
            addXmp(result, FileCommands.getSidecar(filepath, true));
            addXmp(result, FileCommands.getSidecar(filepath, false));

            if (currentImageId != 0) {

                ContentValues dbContent = TagSql.getDbContent(context, currentImageId);
                if (dbContent != null) {
                    result.append(NL).append(line).append(NL);
                    result.append(NL).append(TagSql.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE).append(NL).append(NL);
                    // sort by keys
                    List<String> sortedKeys=new ArrayList(dbContent.keySet());
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
                }
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

    private static String line = "------------------";
    private static void addExif(StringBuilder builder, File file) throws IOException {
        if (file.exists()) {
            builder.append(NL).append(file).append(NL).append(NL);

            ImageMetaReader meta = new ImageMetaReader().load(file.getAbsolutePath(),null, null, "ImageDetailMetaDialogBuilder");
            if (meta != null) builder.append(meta.toString());
            builder.append(NL).append(line).append(NL);
        } else {
            builder.append(NL).append(file).append(" not found.").append(NL);
        }
    }

    private static void addXmp(StringBuilder builder, File file) throws IOException {
        if (file.exists()) {
            XmpSegment meta = new XmpSegment();
            meta.load(file, "ImageDetailMetaDialogBuilder");
            builder.append(NL).append(file).append(NL).append(NL);
            meta.appendXmp(null, builder);
            builder.append(NL).append(line).append(NL);
        } else {
            builder.append(NL).append(file).append(" not found.").append(NL);
        }
    }

    private static void getExifInfo_android(StringBuilder builder, String filepath) throws IOException {
        ExifInterfaceEx exif = new ExifInterfaceEx(filepath, null, null, "ImageDetailMetaDialogBuilder.getExifInfo_android");

        builder.append(NL).append(line).append(NL);
        builder.append(NL).append(filepath).append(NL).append(NL);
        if (exif.isValidJpgExifFormat()) builder.append(exif.getDebugString(NL));

        builder.append(NL).append(line).append(NL);
    }
}
