package de.k3b.android.fotoviewer;

/**
 * Global Settings
 *
 * Created by k3b on 04.06.2015.
 */
public class Global {
    public static final String LOG_CONTEXT = "k3bFoto";

    /**
     * true: addToCompressQue several Log.d(...) to show what is going on.
     * debugEnabled is updated by the SettingsActivity
     */
    public static boolean debugEnabled = true;

    /** true: load static demo data; false load real data */
    public static boolean demoMode = false;

    /** true: load images from folder and its subfolders. false: do not load images from subfolders */
    public static boolean includeSubItems = true;


}
