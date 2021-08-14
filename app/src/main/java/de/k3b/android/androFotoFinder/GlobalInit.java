package de.k3b.android.androFotoFinder;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;

import de.k3b.android.androFotoFinder.queries.DatabaseHelper;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.GlobalMediaContentObserver;
import de.k3b.android.androFotoFinder.queries.IMediaRepositoryApi;
import de.k3b.android.androFotoFinder.queries.MediaContent2DBUpdateService;
import de.k3b.android.androFotoFinder.queries.MediaContentproviderRepository;
import de.k3b.android.androFotoFinder.queries.MediaDBRepository;
import de.k3b.android.androFotoFinder.queries.MediaRepositoryApiWrapper;
import de.k3b.android.androFotoFinder.queries.MergedMediaRepository;
import de.k3b.android.osmdroid.forge.MapsForgeSupport;
import de.k3b.android.util.PhotoChangeNotifyer;
import de.k3b.android.widget.LocalizedActivity;
import de.k3b.database.QueryParameter;

public class GlobalInit {
    public static final String LOG_CONTEXT = "k3bFoto";
    private static boolean mustInitialize = true;

    public static void initIfNeccessary(Application applicationContext) {
        if (mustInitialize) {
            mustInitialize = false;
            GlobalFiles.pickHistoryFile = applicationContext.getDatabasePath("pickHistory.geouri.txt");

            SettingsActivity.prefs2Global(applicationContext);

            ThumbNailUtils.init(applicationContext, null);

            // #60: configure some of the mapsforge settings first
            MapsForgeSupport.createInstance(applicationContext);
        }
    }

    public static void setMediaImageDbReplacement(Context context, boolean useMediaImageDbReplacement) {
        final IMediaRepositoryApi oldMediaDBApi = FotoSql.getMediaDBApi();
        if ((oldMediaDBApi == null) || (Global.useAo10MediaImageDbReplacement != useMediaImageDbReplacement)) {

            // menu must be recreated
            LocalizedActivity.setMustRecreate();

            Global.useAo10MediaImageDbReplacement = useMediaImageDbReplacement;


            if (Global.useAo10MediaImageDbReplacement) {
                registerAo10MediaImageDbReplacement(context);
            } else {
                registerMediaContentProvider(context, oldMediaDBApi);
            }
        }
    }

    private static void registerMediaContentProvider(Context context, IMediaRepositoryApi oldMediaDBApi) {
        final MediaContentproviderRepository mediaContentproviderRepository = new MediaContentproviderRepository(context);
        PhotoChangeNotifyer.unregisterContentObserver(context, GlobalMediaContentObserver.getInstance(context));
        if ((oldMediaDBApi != null) && (MediaContent2DBUpdateService.instance != null)) {
            // switching from mediaImageDbReplacement to Contentprovider
            MediaContent2DBUpdateService.instance.clearMediaCopy();
        }
        FotoSql.setMediaDBApi(mediaContentproviderRepository, null);
        MediaContent2DBUpdateService.instance = null;
    }

    /**
     * Android-10-ff use copy of media database for reading to circumvent android-10-media-contentprovider-restrictions
     */
    private static IMediaRepositoryApi registerAo10MediaImageDbReplacement(Context context) {
        File databaseFile = DatabaseHelper.getDatabasePath(context);
        try {
            final SQLiteDatabase writableDatabase = DatabaseHelper.getWritableDatabase(context);
            //!!! throws SQLiteCantOpenDatabaseException("Failed to open database '/storage/emulated/0/databases/APhotoManager.db'") if no permission

            final MediaDBRepository mediaDBRepository = new MediaDBRepository(writableDatabase);
            final MediaContentproviderRepository mediaContentproviderRepository = new MediaContentproviderRepository(context);

            // read from copy database, write to both: copy-database and content-provider
            final MergedMediaRepository mediaDBApi = new MergedMediaRepository(mediaDBRepository, mediaContentproviderRepository);
            FotoSql.setMediaDBApi(mediaDBApi, mediaDBRepository);

            MediaContent2DBUpdateService.instance = new MediaContent2DBUpdateService(context, writableDatabase);

            if (FotoSql.getCount(new QueryParameter().addWhere("1 = 1")) == 0) {
                // database is empty; reload from Contentprovider
                MediaContent2DBUpdateService.instance.rebuild(
                        "registerAo10MediaImageDbReplacement", context, null);
            }

            PhotoChangeNotifyer.registerContentObserver(context, GlobalMediaContentObserver.getInstance(context));

            return mediaDBApi;
        } catch (RuntimeException ignore) {
            Log.w(LOG_CONTEXT,
                    "Cannot open Database (missing permissions) "
                            + DatabaseHelper.getDatabasePath(context) + " "
                            + ignore.getMessage(), ignore);
            FotoSql.setMediaDBApi(new MediaDBRepositoryLoadOnDemand(context), null);
        }
        return null;
    }

    /**
     * if Open Database failes because of missing File permissions
     * postpone opening database until permission is granted
     */
    private static class MediaDBRepositoryLoadOnDemand extends MediaRepositoryApiWrapper {

        private final Context context;

        public MediaDBRepositoryLoadOnDemand(Context context) {
            super(null);
            this.context = context;
        }

        @Override
        protected IMediaRepositoryApi getReadChild() {
            initIfNeccessary(AndroFotoFinderApp.instance);
            return registerAo10MediaImageDbReplacement(context);
        }
    }

}
