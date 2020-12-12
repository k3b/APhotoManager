package de.k3b.android.androFotoFinder.media;

import android.content.ContentValues;
import android.util.Log;

import java.io.IOException;
import java.util.Date;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.io.FileUtils;
import de.k3b.io.StringUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.io.filefacade.IFile;
import de.k3b.media.ExifInterfaceEx;
import de.k3b.media.PhotoPropertiesUtil;

/**
 * Android specific Version of {@link ExifInterfaceEx} that updates the
 * Database, when saving exif changes.
 */
public class AndroidExifInterfaceEx extends ExifInterfaceEx {
    // set to true to log what happens to database-ID when changing exif
    private static final boolean DBG_RENAME_IN_DB_ENABLED = true;

    private boolean overwriteOriginal;
    private String inPath;
    private String outPath;
    private Boolean hasXmp;

    public static void init() {
        setFactory(new Factory() {
            @Override
            public ExifInterfaceEx create() {
                return new AndroidExifInterfaceEx();
            }
        });
    }

    @Override
    public void saveAttributes(IFile inFile, IFile outFile,
                               boolean deleteInFileOnFinish, Boolean hasXmp) throws IOException {
        super.saveAttributes(inFile, outFile, deleteInFileOnFinish, hasXmp);
        this.hasXmp = hasXmp;
    }

    @Override
    protected IFile renameSouraceFileBeforeReplaceOrThrow(IFile oldSourcefile, String newName) throws IOException {
        debugIdPaths("renameSouraceFileBeforeReplaceOrThrow begin", oldSourcefile.getAbsolutePath(), newName);
        this.overwriteOriginal = true;
        this.inPath = oldSourcefile.getAbsolutePath();
        this.outPath = this.inPath + TMP_FILE_SUFFIX;

        if (!renameInDatabase(":renameSouraceFileBeforeReplaceOrThrow", this.inPath, this.outPath, false)) {
            this.outPath = null; // failed
        }

        final IFile result = super.renameSouraceFileBeforeReplaceOrThrow(oldSourcefile, newName);
        debugIdPaths("renameSouraceFileBeforeReplaceOrThrow end", oldSourcefile.getAbsolutePath(), newName);
        return result;
    }

    @Override
    protected void beforeCloseSaveOutputStream() {
        if (this.outPath != null) {
            renameInDatabase(":beforeCloseSaveOutputStream", this.outPath, this.inPath, true);
            this.outPath = null;
        }
        super.beforeCloseSaveOutputStream();
    }

    // TODO additional database parameters (see scanner)
    // DateLastModified, xmpDate, ....
    private boolean renameInDatabase(String dbgContext, String fromPath, String toPath, boolean thransferExif) {
        ContentValues values = new ContentValues();
        if (thransferExif) {
            PhotoPropertiesMediaDBContentValues mediaValueAdapter = new PhotoPropertiesMediaDBContentValues().set(values, null);

            PhotoPropertiesUtil.copyNonEmpty(mediaValueAdapter, this);

            Date lastModified = new Date();
            TagSql.setFileModifyDate(values, lastModified);
            if (this.hasXmp != null) {
                if (this.hasXmp) {
                    TagSql.setXmpFileModifyDate(values, lastModified);
                } else {
                    TagSql.setXmpFileModifyDate(values, TagSql.EXT_LAST_EXT_SCAN_NO_XMP);
                }
            }
        }
        values.put(FotoSql.SQL_COL_PATH, toPath);
        debugIdPaths(dbgContext + " renameInDatabase begin", fromPath, toPath);
        final int execResultCount = FotoSql.getMediaDBApi().
                execUpdate(this.getClass().getSimpleName() + dbgContext, fromPath, values, null);

        debugIdPaths(dbgContext + " renameInDatabase end " + execResultCount, fromPath, toPath);
        if ((execResultCount != 1) && DBG_RENAME_IN_DB_ENABLED) {
// !!!! debug ausgabe path+ id failed
        }
        return 1 == execResultCount;
    }

    private void debugIdPaths(String dbgContext, String... paths) {
        if (DBG_RENAME_IN_DB_ENABLED) {
            StringBuilder sqlWhere = new StringBuilder();
            for (String path : paths) {
                if (sqlWhere.length() > 0) {
                    sqlWhere.append(" OR ");
                }
                sqlWhere.append("(").append(FotoSql.SQL_COL_PATH).append(" like '")
                        .append(FileUtils.replaceExtension(path, "")).append("%')");
            }

            // to prevent adding visibility
            sqlWhere.append(" and " +
                    FotoSql.SQL_COL_EXT_MEDIA_TYPE +
                    " is not null");
            final SelectedFiles selectedfiles = FotoSql.getSelectedfiles(sqlWhere.toString(), VISIBILITY.PRIVATE_PUBLIC);
            Log.d(Global.LOG_CONTEXT, dbgContext + "\n\t["
                    + StringUtils.appendMessage(null, paths)
                    + "] :\n\t\t"
                    + selectedfiles.toIdString() + " -> " + selectedfiles.toPathListString());
        }
    }
}
