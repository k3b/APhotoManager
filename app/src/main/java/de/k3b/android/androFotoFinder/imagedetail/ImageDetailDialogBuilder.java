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
import android.media.ExifInterface;
import android.widget.ScrollView;
import android.widget.TextView;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.database.QueryParameter;

/**
 * Created by k3b on 15.07.2015.
 */
public class ImageDetailDialogBuilder {
    private static final String NL = "\n";

    public static Dialog createImageDetailDialog(Activity context, String filePath, long imageId,
                                                 QueryParameter query,
                                                 long offset) {
        StringBuilder result = new StringBuilder();
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

    public static Dialog createImageDetailDialog(Activity context, String title, String block, String... moreBlocks) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        ScrollView sv = new ScrollView(context);
        sv.setVerticalScrollBarEnabled(true);
        TextView view = new TextView(context);

        if ((moreBlocks != null) && (moreBlocks.length > 0)) {
            StringBuilder result = new StringBuilder(block);
            for (String subBlock : moreBlocks) {
                append(result, subBlock);
            }
            view.setText(result.toString());
        } else {
            view.setText(block);
        }

        sv.addView(view);
        builder.setView(sv);

        builder.setPositiveButton(R.string.btn_ok, null); /*, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                this.finalize();
            }
        });*/

        return builder.create();
    }

    private static void appendExifInfo(StringBuilder result, Activity context, String filepath, long currentImageId) {
        try {
            if (currentImageId != 0) {
                getExifInfo_android(result, filepath);

                File jpegFile = new File(filepath);
                addExif(result, jpegFile);

                int ext = filepath.lastIndexOf(".");

                String xmpFilePath = (ext >= 0) ? (filepath.substring(0, ext) + ".xmp") : (filepath + ".xmp");
                File xmpFile = new File(xmpFilePath);
                addExif(result, xmpFile);


                ContentValues dbContent = FotoSql.getDbContent(context, currentImageId);
                if (dbContent != null) {
                    result.append(NL).append(line).append(NL);
                    result.append(NL).append(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI).append(NL).append(NL);
                    for (Map.Entry<String, Object> item : dbContent.valueSet()) {
                        result.append(item.getKey()).append("=").append(item.getValue()).append(NL);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ImageProcessingException e) {
            e.printStackTrace();
        }
    }

    private static String line = "------------------";
    private static void addExif(StringBuilder builder, File file) throws ImageProcessingException, IOException {
        if (file.exists()) {
            builder.append(NL).append(file).append(NL).append(NL);

            Metadata metadata = ImageMetadataReader.readMetadata(file);
            for (Directory directory : metadata.getDirectories()) {
                builder.append(NL).append(directory).append(NL).append(NL);

                for (Tag tag : directory.getTags()) {
                    String description = tag.getDescription();
                    if (description == null)
                        description = "#" + tag.getTagType();

                    builder.append(tag.getTagName()).append(" : ").append(description).append(NL);
                }
            /*
            //
            // Each Directory may also contain error messages
            //
            if (directory.hasErrors()) {
                for (String error : directory.getErrors()) {
                    System.err.println("ERROR: " + error);
                }
            }
            */
                builder.append(NL).append(line).append(NL);
            }
        } else {
            builder.append(NL).append(file).append(" not found.").append(NL);
        }
    }

    private static void getExifInfo_android(StringBuilder builder, String filepath) throws IOException {
        ExifInterface exif = new ExifInterface(filepath);

        builder.append(NL).append(line).append(NL);
        builder.append(NL).append(filepath).append(NL).append(NL);

        builder.append("Date & Time: ").append(getExifTag(exif, ExifInterface.TAG_DATETIME)).append("\n\n");
        builder.append("Flash: ").append(getExifTag(exif, ExifInterface.TAG_FLASH)).append("\n");
        builder.append("Focal Length: ").append(getExifTag(exif, ExifInterface.TAG_FOCAL_LENGTH)).append("\n\n");

        float[] latLong=new float[2];
        if (exif.getLatLong(latLong)) {
            builder.append("GPS Date & Time: ").append(getExifTag(exif, ExifInterface.TAG_GPS_DATESTAMP)).append(" "
            ).append(getExifTag(exif, ExifInterface.TAG_GPS_TIMESTAMP)).append("\n\n");
            builder.append("GPS Latitude: ").append(latLong[0]).append("\n");
            builder.append("GPS Longitude: ").append(latLong[1]).append("\n");
            builder.append("GPS Altitude: ").append(exif.getAltitude(0)).append("\n");
            builder.append("GPS Processing Method: ").append(getExifTag(exif, ExifInterface.TAG_GPS_PROCESSING_METHOD)).append("\n");
        }
        builder.append("Image Length: ").append(getExifTag(exif,ExifInterface.TAG_IMAGE_LENGTH)).append("\n");
        builder.append("Image Width: ").append(getExifTag(exif,ExifInterface.TAG_IMAGE_WIDTH)).append("\n\n");
        builder.append("Camera Make: ").append(getExifTag(exif,ExifInterface.TAG_MAKE)).append("\n");
        builder.append("Camera Model: ").append(getExifTag(exif,ExifInterface.TAG_MODEL)).append("\n");
        builder.append("Camera Orientation: ").append(getExifTag(exif,ExifInterface.TAG_ORIENTATION)).append("\n");
        builder.append("Camera White Balance: ").append(getExifTag(exif, ExifInterface.TAG_WHITE_BALANCE)).append("\n");
        builder.append(NL).append(line).append(NL);
    }

    private static String getExifTag(ExifInterface exif,String tag){
        String attribute = exif.getAttribute(tag);

        return (null != attribute ? attribute : "");
    }

}
