package de.k3b.android.androFotoFinder;

import android.os.Environment;

import java.io.File;

import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;

public class GlobalFiles extends Global {
    // is null for android-10 or later
    private static final File externalStorageDirectory = getExternalStorageDirectory();
    /**
     * defines the filesystem's directory where [Bookmark files](Bookmarks) are stored and loaded from.
     */
    public static IFile reportDir = (externalStorageDirectory == null)
            ? null
            : FileFacade.convert("Global reportDir", new File(externalStorageDirectory, "databases/sql"));
    /**
     * defines the filesystem's directory where crash reports are written to.
     */
    public static File logCatDir = (externalStorageDirectory == null)
            ? null
            : new File(Environment.getExternalStorageDirectory(), "copy/log");
    /**
     * #60 where osm-mapsforge-offline-maps (*.map) are found. defaults to /extDir/osmdroid/
     */
    public static File mapsForgeDir = (externalStorageDirectory == null)
            ? null
            : new File(Environment.getExternalStorageDirectory(), "osmdroid");
    /**
     * remember last picked geo-s
     */
    public static File pickHistoryFile = null; // initialized in app.onCreate with local database file

    private static File getExternalStorageDirectory() {
        File result = Environment.getExternalStorageDirectory();

        if (result == null) {
            // on android-10 or laterEnvironment.getExternalStorageDirectory() is not usable any more :-(
            result = AndroFotoFinderApp.instance.getExternalCacheDir();
        }
        return result;
    }
}
