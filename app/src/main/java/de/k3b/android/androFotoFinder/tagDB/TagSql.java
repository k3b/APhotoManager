package de.k3b.android.androFotoFinder.tagDB;

import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.Date;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.database.QueryParameter;

/**
 * Database related code to handle non standard image processing (Tags, Description)
 *
 * Created by k3b on 30.09.2016.
 */

public class TagSql extends FotoSql {
    /** used to query non-standard-image fields */
    public static final Uri SQL_TABLE_EXTERNAL_CONTENT_URI_FILE = MediaStore.Files.getContentUri("external");

    private static final String SQL_COL_EXT_TAGS = MediaStore.Video.Media.TAGS;
    private static final String SQL_COL_EXT_DESCRIPTION = MediaStore.Images.Media.DESCRIPTION;

    /** The date & time when last non standard media-scan took place
     *  <P>Type: INTEGER (long) as seconds since jan 1, 1970</P> */
    private static final String SQL_COL_EXT_LAST_EXT_SCAN = MediaStore.Video.Media.DURATION;

    private static final String EXT_FILTER_MEDIA_TYPE
            = MediaStore.Files.FileColumns.MEDIA_TYPE
            + "='" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE+"'";

    /** only rows containing all tags are visible */
    public static void addWhereTag(QueryParameter newQuery, String... tags) {
        String tagvalue = (Global.enableTagSupport) ? TagConverter.tagsAsString("%", tags) : null;
        if (tagvalue != null) {
            newQuery.addWhere(SQL_COL_EXT_TAGS + " like ?", tagvalue);
            switchFrom(newQuery, SQL_TABLE_EXTERNAL_CONTENT_URI_FILE);
        }
    }

    /** modifies the from part if not already set */
    private static void switchFrom(QueryParameter newQuery, Uri newTable) {
        boolean toFile = newTable.equals(SQL_TABLE_EXTERNAL_CONTENT_URI_FILE);
        Uri oldTable = (toFile)
                ? FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI
                : SQL_TABLE_EXTERNAL_CONTENT_URI_FILE;

        // must use different contentprovider that supports other columns
        if (newQuery.toFrom().compareTo(oldTable.toString()) == 0) {
            if (toFile) {
                // file table contain data for different media types.
                // We are only interested in images
                newQuery.addWhere(EXT_FILTER_MEDIA_TYPE);
            } else {
                // database image view does not contain media_type.
                // it is already filtered media_type=images
                // remove expression
                newQuery.removeWhere(EXT_FILTER_MEDIA_TYPE);
            }
            newQuery.replaceFrom(newTable.toString());
        }
    }

    public static void setTags(ContentValues values, String... tags) {
        values.put(SQL_COL_EXT_TAGS, TagConverter.tagsAsString("",tags));
        setLastScanDate(values, new Date());
    }

    public static void setDescription(ContentValues values, String description) {
        values.put(SQL_COL_EXT_DESCRIPTION, description);
        setLastScanDate(values, new Date());
    }

    private static void setLastScanDate(ContentValues values, Date lastScanDate) {
        Long now = (lastScanDate != null)
                ? lastScanDate.getTime() / 1000 // sec
                : null;
        values.put(SQL_COL_EXT_LAST_EXT_SCAN, now);
    }

    /** only rows are visible that needs to run the ext media scanner */
    public static void addWhereNeedsExtMediaScan(QueryParameter newQuery) {
        if (Global.enableTagSupport) {
            newQuery.addWhere(SQL_COL_EXT_LAST_EXT_SCAN + " is null");
            switchFrom(newQuery, SQL_TABLE_EXTERNAL_CONTENT_URI_FILE);
        }
    }
}
