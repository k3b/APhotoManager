package de.k3b;

/**
 * Public Global stuff for the lib.
 * Created by k3b on 03.03.2016.
 */
public class FotoLibGlobal {
    /** LOG_CONTEXT is used as logging source for filtering logging messages that belong to this */
    public static final String LOG_TAG = "k3bFotoLib2";

    /**
     * Global.xxxxx. Non final values may be changed from outside (SettingsActivity or commandline parameter)
     */
    public static boolean debugEnabled = false;

    /** false do not follow symlinks when scanning Directories.  */
    public static final boolean ignoreSymLinks = false;
}
