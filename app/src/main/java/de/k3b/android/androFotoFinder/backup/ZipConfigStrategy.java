package de.k3b.android.androFotoFinder.backup;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;

import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.queries.AndroidAlbumUtils;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.util.IntentUtil;
import de.k3b.database.QueryParameter;
import de.k3b.io.AlbumFile;
import de.k3b.io.FileNameUtil;
import de.k3b.io.FileUtils;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.StringUtils;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.zip.IZipConfig;
import de.k3b.zip.LibZipGlobal;
import de.k3b.zip.ZipConfigDto;
import de.k3b.zip.ZipConfigRepository;

/**
 * infer existing or new ZipConfig from current context (intent)
 */
public class ZipConfigStrategy {
    private static String mDebugPrefix = "ZipConfigStrategy ";

    /**
     * Priority:
     * * selectedFiles create "Photos" if selectedFiles is not empty
     * * uri: ZipConfigFromUri
     * * uri-album: ZipConfigFrom(albumName)
     * * uri-album: create ZipConfig from album with albumname
     * * uri-file: ZipConfigFrom(childFolderName)
     * * query+filter
     */
    public static IZipConfig getOrCreate(String debugContext,
                                         Activity context,
                                         SelectedFiles selectedFiles,
                                         Uri uri,
                                         IGalleryFilter filter, QueryParameter query) {
        String dbgSource = null;
        IZipConfig config = null;
        boolean inferPath = true;

        if ((selectedFiles != null) && (selectedFiles.size() > 0)) {
            config = new ZipConfigDto(null);
            config.setZipName(getSelectedFilesName(context));
            QueryParameter queryWithIds = new QueryParameter(FotoSql.queryDetail);
            FotoSql.setWhereSelectionPks(queryWithIds, selectedFiles.toIdString());
            inferPath = false;
            inferMissingParameters(config, queryWithIds, null, inferPath);
            dbgSource = "create '" + config.getZipName() + "' from selection " + selectedFiles.toIdString();
            new ZipConfigRepository(config).save();
        }

        if ((null == config) && (uri != null)) {
            config = loadFromExistingZipConfigUri(uri, context);
            dbgSource = "load existing '" + config.getZipName() + "' for zipConfigUri " + uri;

            if (null == config) {
                config = loadOrCreateFromExistingAlbumUri(context, uri);
                dbgSource = "load or create '" + config.getZipName() + "' from Existing Album Uri " + uri;
                inferPath = false;
            }
            if (null == config) {
                config = loadOrCreateFromExistingFileDirUri(context, uri, inferPath);
                dbgSource = "load or create '" + config.getZipName() + "' from Existing file Uri " + uri;
            }
        }

        if (config == null) {
            config = new ZipConfigDto(null);
            inferMissingParameters(config, query, filter, inferPath);
            dbgSource = "create '" + config.getZipName() + "' from query+filter ";
        }

        if (LibZipGlobal.debugEnabled) {
            Log.d(LibZipGlobal.LOG_TAG, mDebugPrefix + context.getClass().getSimpleName()
                    + ": getOrCreate(" + dbgSource + ")");
        }

        if (!(config instanceof Serializable)) {
            return new ZipConfigDto(config);
        }
        return config;
    }

    private static IZipConfig loadOrCreateFromExistingFileDirUri(Activity context, Uri uri, boolean inferPath) {
        IZipConfig config = null;
        if (uri != null) {
            File file = IntentUtil.getExistingFileOrNull(context, uri);
            if ((file != null) && (!file.isDirectory())) file = file.getParentFile();
            String name = (file != null) ? ZipConfigDto.fixZipBaseName(file.getName()) : null;
            if (!StringUtils.isNullOrEmpty(name)) {
                config = ZipConfigRepository.getZipConfigOrNull(name);
                if (config == null) {
                    config = new ZipConfigDto(null);
                    GalleryFilterParameter uriPathfilter = new GalleryFilterParameter().setFolderAndBelow(file.getAbsolutePath());
                    inferMissingParameters(config, null, uriPathfilter, inferPath);
                }
            }
        }
        return config;
    }

    /**
     * config/query/filter has been successfully loaded. Add missing fields to config from query/filter.
     */
    private static void inferMissingParameters(IZipConfig config, QueryParameter baseQuery, IGalleryFilter filter, boolean inferPath) {
        String albumFile = null;
        if ((filter != null) && (AlbumFile.isQueryFile(filter.getPath()))) {
            albumFile = filter.getPath();
            if (StringUtils.isNullOrEmpty(config.getZipName())) {
                config.setZipName(new File(AlbumFile.fixPath(albumFile)).getName());
            }
            filter = new GalleryFilterParameter().get(filter).setPath(null);
        }
        QueryParameter merged = getAsMergedQuery(baseQuery, filter);
        if ((merged != null) && merged.hasWhere()) {
            if (StringUtils.isNullOrEmpty(config.getFilter())) {
                config.setFilter(merged.toReParseableString());
            }

            String path = FileNameUtil.fixPath(FotoSql.getFilePath(merged, false));
            if (path != null) {
                if (inferPath && StringUtils.isNullOrEmpty(config.getZipRelPath())) {
                    config.setZipRelPath(path);
                }
                if (StringUtils.isNullOrEmpty(config.getZipName())) {
                    config.setZipName(new File(path).getName());
                }
            }

            if (config.getDateModifiedFrom() == null) {
                long dateMin = FotoSql.parseDateModifiedMin(merged, false);
                if (dateMin != 0) config.setDateModifiedFrom(new Date(dateMin));
            }
        }
    }

    private static QueryParameter getAsMergedQuery(QueryParameter baseQuery, IGalleryFilter filter) {
        if (GalleryFilterParameter.isEmpty(filter)) return baseQuery;
        return AndroidAlbumUtils.getAsMergedNewQuery(
                baseQuery, filter);
    }

    private static String getSelectedFilesName(Activity context) {
        return context.getString(R.string.gallery_title);
    }

    private static IZipConfig loadOrCreateFromExistingAlbumUri(Activity context, Uri uri) {
        IZipConfig config = null;
        QueryParameter album = AndroidAlbumUtils.getQueryFromUri(mDebugPrefix, context, null, uri, null);
        if (album != null) {
            final String zipName = ZipConfigDto.fixZipBaseName(uri.getLastPathSegment());
            config = ZipConfigRepository.getZipConfigOrNull(zipName);

            if (config == null) {
                config = new ZipConfigDto(null);
                inferMissingParameters(config, album, null, false);
                if (!StringUtils.isNullOrEmpty(zipName)) {
                    config.setZipName(zipName);
                }
            }
        }
        return config;
    }

    public static IZipConfig loadFromExistingZipConfigUri(Uri uri, Context context) {
        if ((uri != null) && ZipConfigRepository.isZipConfig(uri.toString())) {
            InputStream inputsteam = null;
            try {
                inputsteam = context.getContentResolver().openInputStream(uri);
                final IZipConfig config = new ZipConfigRepository(null).load(inputsteam, uri);
                if (config != null)
                    config.setZipName(ZipConfigDto.fixZipBaseName(uri.getLastPathSegment()));
                return config;
            } catch (Exception ex) {
                // file not found or no permission
                Log.w(LibZipGlobal.LOG_TAG, mDebugPrefix + context.getClass().getSimpleName()
                        + "-loadZipConfig(" + uri + ") failed " + ex.getClass().getSimpleName(), ex);
            } finally {
                FileUtils.close(inputsteam, uri);
            }
        }
        return null;
    }

}
