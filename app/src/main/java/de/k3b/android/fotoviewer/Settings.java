package de.k3b.android.fotoviewer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by k3b on 04.06.2015.
 */
public class Settings {

//    private static int minPunchOutTreshholdInSecs = 1;

    public static void init(final Context context) {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        Global.debugEnabled = Settings.getPrefValue(prefs, "debugEnabled",
                Global.debugEnabled);

        /*
        Settings.minPunchOutTreshholdInSecs = Settings.getPrefValue(prefs,
                "minPunchOutTreshholdInSecs",
                Settings.minPunchOutTreshholdInSecs);
        */
    }

    /**
     * Since this value comes from a text-editor it is stored as string.
     * Conversion to int must be done yourself.
     */
    private static int getPrefValue(final SharedPreferences prefs,
                                    final String key, final int notFoundValue) {
        try {
            return Integer.parseInt(prefs.getString(key,
                    Integer.toString(notFoundValue)));
        } catch (final ClassCastException ex) {
            Log.w(Global.LOG_CONTEXT, "getPrefValue-Integer(" + key + ","
                    + notFoundValue + ") failed: " + ex.getMessage());
            return notFoundValue;
        }
    }

    private static boolean getPrefValue(final SharedPreferences prefs,
                                        final String key, final boolean notFoundValue) {
        try {
            return prefs.getBoolean(key, notFoundValue);
        } catch (final ClassCastException ex) {
            Log.w(Global.LOG_CONTEXT, "getPrefValue-Boolean(" + key + ","
                    + notFoundValue + ") failed: " + ex.getMessage());
            return notFoundValue;
        }
    }

}
