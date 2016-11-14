package de.k3b.android.util;

import android.content.ContentValues;
import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Date;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.media.MediaContentValues;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.io.FileUtils;
import de.k3b.media.IMetaApi;
import de.k3b.media.MediaUtil;
import de.k3b.media.MediaXmpSegment;
import de.k3b.tagDB.Tag;
import de.k3b.tagDB.TagRepository;

/**
 * Extensions to Android Media Scanner that are not supported by original scanner.
 *
 * Created by k3b on 04.10.2016.
 */

public class MediaScannerEx extends MediaScanner {
    public MediaScannerEx(Context context) {
        super(context);
    }

    @Override
    protected void getExifFromFile(ContentValues values, File file) {
        super.getExifFromFile(values, file);

        // for first tests generate test data
        if (false && Global.enableNonStandardMediaFields) {
            addTags(values,"test1", "test2");
            TagSql.setDescription(values,"test");
            TagSql.setRating(values, 3);
        }
    }

    /** updates values with current values of file.
     * Override: also get xmp data (i.e. Tags) */
    @Override
    protected int getExifValues(MediaContentValues dest, File file, ExifInterfaceEx exif) {
        int changes = 0;

        File xmpFile = FileUtils.getXmpFile(file.getAbsolutePath());
        MediaXmpSegment xmp = null;
        if ((xmpFile != null) && xmpFile.exists() && xmpFile.isFile()) {
            xmp = new MediaXmpSegment();
            try {
                xmp.load(new FileInputStream(xmpFile));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            xmpFile = null;
        }

        if (Global.xmpOverwritesExif) {
            // xmp overwrites exif so execute first exif then xmp
            changes += super.getExifValues(dest, file, exif);
            changes += MediaUtil.copy(dest, xmp, false, true);
        } else {
            // exif overwrites xmp so execute first xmp then exif
            changes += MediaUtil.copy(dest, xmp, false, true);
            changes += super.getExifValues(dest, file, exif);
        }

        return changes;
    }

    /** Override: make shure that TagDB is saved */
    @Override
    public int updateMediaDatabase_Android42(Context context, String[] oldPathNames, String... newPathNames) {
        int result = super.updateMediaDatabase_Android42(context, oldPathNames, newPathNames);
        if ((result > 0) && (Global.enableNonStandardMediaFields)) {
            TagRepository.getInstance().save();
        }
        return result;
    }
    
    private int addTags(ContentValues values, String... tags) {
        TagSql.setTags(values,tags);
        return TagRepository.getInstance().include(Tag.toList(tags));
    }

}
