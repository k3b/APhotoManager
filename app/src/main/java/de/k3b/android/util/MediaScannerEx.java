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
    private Tag mImportRoot = null;

    public MediaScannerEx(Context context) {
        super(context);
    }

    @Override
    protected void getExifFromFile(ContentValues values, File file) {
        super.getExifFromFile(values, file);

        // for first tests generate test data
        if (false && Global.Media.enableNonStandardMediaFields) {
            addTags(values, null, "test1", "test2");
            TagSql.setDescription(values, null, "test");
            TagSql.setRating(values, null, 3);
        }
    }

    /**
     * updates values with current values of file.
     * Override: also get xmp data (i.e. Tags)
     */
    @Override
    protected int getExifValues(MediaContentValues dest, File file, ExifInterfaceEx exif) {
        int changes = 0;
        long xmpFileModifyDate = TagSql.EXT_LAST_EXT_SCAN_UNKNOWN;

        File xmpFile = FileUtils.getXmpFile(file.getAbsolutePath());
        MediaXmpSegment xmp = null;
        if ((xmpFile != null) && xmpFile.exists() && xmpFile.isFile()) {
            xmp = new MediaXmpSegment();
            try {
                TagSql.setXmpFileModifyDate(dest.getContentValues(), xmpFile.lastModified());
                xmp.load(new FileInputStream(xmpFile));
                TagRepository.getInstance().includeString(getImportRoot(), xmp.getTags());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            xmpFile = null;
        } else if (Global.Media.enableXmpNone) {
            xmpFileModifyDate = TagSql.EXT_LAST_EXT_SCAN_NO_XMP;
        }

        if (Global.Media.xmpOverwritesExif) {
            // xmp overwrites exif so execute first exif then xmp
            changes += super.getExifValues(dest, file, exif);
            changes += MediaUtil.copy(dest, xmp, false, true);
        } else {
            // exif overwrites xmp so execute first xmp then exif
            changes += MediaUtil.copy(dest, xmp, false, true);
            changes += super.getExifValues(dest, file, exif);
        }

        if (xmpFileModifyDate != TagSql.EXT_LAST_EXT_SCAN_UNKNOWN) {
            TagSql.setXmpFileModifyDate(dest.getContentValues(), xmpFileModifyDate);
        }
        return changes;
    }

    /**
     * Override: make shure that TagDB is saved
     */
    @Override
    public int updateMediaDatabase_Android42(Context context, String[] oldPathNames, String... newPathNames) {
        int result = super.updateMediaDatabase_Android42(context, oldPathNames, newPathNames);
        if ((result > 0) && (Global.Media.enableNonStandardMediaFields)) {
            TagRepository.getInstance().save();
        }
        return result;
    }

    private int addTags(ContentValues values,  Date xmpFileModifyDate, String... tags) {
        TagSql.setTags(values, xmpFileModifyDate, tags);
        return TagRepository.getInstance().include(getImportRoot(), Tag.toList(tags));
    }

    /** get or create parent-tag where alle imports are appendend as children */
    public Tag getImportRoot() {
        if (mImportRoot == null) {
            mImportRoot = TagRepository.getInstance().getImportRoot();
        }
        return mImportRoot;
    }
}