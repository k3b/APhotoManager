package de.k3b.android.fotoviewer.imagedetail;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.media.ExifInterface;
import android.view.LayoutInflater;
import android.widget.ScrollView;
import android.widget.TextView;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import java.io.File;
import java.io.IOException;

import de.k3b.android.fotoviewer.R;

/**
 * Created by k3b on 15.07.2015.
 */
public class ImageDetailDialogBuilder {
    private static final String NL = "\n";

    public static Dialog createImageDetailDialog(Activity context, String filePath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(filePath);

        ScrollView sv = new ScrollView(context);
        sv.setVerticalScrollBarEnabled(true);
        TextView view = new TextView(context);

        view.setText(getExifInfo(filePath));

        sv.addView(view);
        builder.setView(sv);

        builder.setPositiveButton(R.string.ok, null); /*, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                this.finalize();
            }
        });*/

        return builder.create();
    }

    private static String getExifInfo(String filepath) {
        StringBuilder builder = new StringBuilder();

        try {
            getExifInfo_android(builder, filepath);

            File jpegFile = new File(filepath);
            addExif(builder, jpegFile);

            int ext = filepath.lastIndexOf(".");

            String xmpFilePath = (ext >= 0) ? (filepath.substring(0,ext)+".xmp" ) : (filepath + ".xmp");
            File xmpFile = new File(xmpFilePath);
            addExif(builder, xmpFile);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ImageProcessingException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    private static String line = "------------------";
    private static void addExif(StringBuilder builder, File file) throws ImageProcessingException, IOException {
        if (file.exists()) {
            builder.append(NL).append(line).append(NL);
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
            }
        } else {
            builder.append(NL).append(file).append(" not found.").append(NL);
        }
    }

    private static void getExifInfo_android(StringBuilder builder, String filepath) throws IOException {
        ExifInterface exif = new ExifInterface(filepath);

        builder.append(NL).append(line).append(NL);
        builder.append(NL).append(filepath).append(NL).append(NL);

        builder.append("Date & Time: " + getExifTag(exif,ExifInterface.TAG_DATETIME) + "\n\n");
        builder.append("Flash: " + getExifTag(exif,ExifInterface.TAG_FLASH) + "\n");
        builder.append("Focal Length: " + getExifTag(exif, ExifInterface.TAG_FOCAL_LENGTH) + "\n\n");

        float[] latLong=new float[2];
        if (exif.getLatLong(latLong)) {
            builder.append("GPS Date & Time: " + getExifTag(exif, ExifInterface.TAG_GPS_DATESTAMP) + " "
                    + getExifTag(exif, ExifInterface.TAG_GPS_TIMESTAMP) + "\n\n");
            builder.append("GPS Latitude: " + latLong[0] + "\n");
            builder.append("GPS Longitude: " + latLong[1] + "\n");
            builder.append("GPS Altitude: " + exif.getAltitude(0) + "\n");
            builder.append("GPS Processing Method: " + getExifTag(exif, ExifInterface.TAG_GPS_PROCESSING_METHOD) + "\n");
        }
        builder.append("Image Length: " + getExifTag(exif,ExifInterface.TAG_IMAGE_LENGTH) + "\n");
        builder.append("Image Width: " + getExifTag(exif,ExifInterface.TAG_IMAGE_WIDTH) + "\n\n");
        builder.append("Camera Make: " + getExifTag(exif,ExifInterface.TAG_MAKE) + "\n");
        builder.append("Camera Model: " + getExifTag(exif,ExifInterface.TAG_MODEL) + "\n");
        builder.append("Camera Orientation: " + getExifTag(exif,ExifInterface.TAG_ORIENTATION) + "\n");
        builder.append("Camera White Balance: " + getExifTag(exif, ExifInterface.TAG_WHITE_BALANCE) + "\n");
    }

    private static String getExifTag(ExifInterface exif,String tag){
        String attribute = exif.getAttribute(tag);

        return (null != attribute ? attribute : "");
    }

}
