package de.k3b.android.androFotoFinder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import java.io.File;

import uk.co.senab.photoview.HugeImageLoader;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.log.LogManager;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.preferences);
        global2Prefs(this.getApplication());
    }

    @Override
    public void onPause() {
        prefs2Global(this.getApplication());
        super.onPause();
    }

    public static void global2Prefs(Context context) {
        final SharedPreferences prefsInstance = PreferenceManager
                .getDefaultSharedPreferences(context);

        SharedPreferences.Editor prefs = prefsInstance.edit();
        prefs.putBoolean("debugEnabled", Global.debugEnabled);
        prefs.putBoolean("debugEnabledViewItem", Global.debugEnabledViewItem);
        prefs.putBoolean("debugEnabledSql", Global.debugEnabledSql);
        prefs.putBoolean("debugEnabledMemory", Global.debugEnabledMemory);

        prefs.putBoolean("debugEnableLibs", PhotoViewAttacher.DEBUG);

        prefs.putBoolean("clearSelectionAfterCommand", Global.clearSelectionAfterCommand);

        prefs.putString("maxSelectionMarkersInMap", "" + Global.maxSelectionMarkersInMap);
        prefs.putString("slideshowIntervallInMilliSecs", "" + Global.slideshowIntervallInMilliSecs);
        prefs.putString("actionBarHideTimeInMilliSecs", "" + Global.actionBarHideTimeInMilliSecs);
        prefs.putString("pickHistoryMax", "" + Global.pickHistoryMax);

        prefs.putString("reportDir", (Global.reportDir != null) ? Global.reportDir.getAbsolutePath() : null);
        prefs.putString("logCatDir", (Global.logCatDir != null) ? Global.logCatDir.getAbsolutePath() : null);
        prefs.putString("pickHistoryFile", (Global.pickHistoryFile != null) ? Global.pickHistoryFile.getAbsolutePath() : null);

        prefs.commit();

    }

    public static void prefs2Global(Context context) {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        Global.debugEnabled                     = getPref(prefs, "debugEnabled", Global.debugEnabled);
        Global.debugEnabledViewItem             = getPref(prefs, "debugEnabledViewItem", Global.debugEnabledViewItem);
        Global.debugEnabledSql                  = getPref(prefs, "debugEnabledSql", Global.debugEnabledSql);
        Global.debugEnabledMemory               = getPref(prefs, "debugEnabledMemory", Global.debugEnabledMemory);

        // one setting for several 3d party debug-flags
        PhotoViewAttacher.DEBUG                 = getPref(prefs, "debugEnableLibs", PhotoViewAttacher.DEBUG);
        HugeImageLoader.DEBUG                   = PhotoViewAttacher.DEBUG;
        LogManager.enableDebug(PhotoViewAttacher.DEBUG);

        Global.clearSelectionAfterCommand       = getPref(prefs, "clearSelectionAfterCommand", Global.clearSelectionAfterCommand);

        Global.maxSelectionMarkersInMap         = getPref(prefs, "maxSelectionMarkersInMap"     , Global.maxSelectionMarkersInMap);
        Global.slideshowIntervallInMilliSecs    = getPref(prefs, "slideshowIntervallInMilliSecs", Global.slideshowIntervallInMilliSecs);
        Global.actionBarHideTimeInMilliSecs     = getPref(prefs, "actionBarHideTimeInMilliSecs" , Global.actionBarHideTimeInMilliSecs);
        Global.pickHistoryMax = getPref(prefs, "pickHistoryMax"               , Global.pickHistoryMax);

        Global.reportDir                        = getPref(prefs, "reportDir", Global.reportDir);
        Global.logCatDir                        = getPref(prefs, "logCatDir", Global.logCatDir);

        Global.pickHistoryFile                  = getPref(prefs, "pickHistoryFile", Global.pickHistoryFile);

        /*
        // bool
        debugEnabled
        debugEnabledViewItem
        debugEnabledSql
        debugEnabledMemory
        clearSelectionAfterCommand

        // int
        maxSelectionMarkersInMap
        slideshowIntervallInMilliSecs
        actionBarHideTimeInMilliSecs

        // file
        reportDir
        logCatDir

        */

    }

    /** load File preference from SharedPreferences */
    private static File getPref(SharedPreferences prefs, String key, File defaultValue) {
        String def = (defaultValue != null) ? defaultValue.getAbsolutePath() : null;
        String value         = prefs.getString(key, def);

        if ((def == null) || (def.trim().length() == 0)) return null;
        return new File(value);
    }

    /** load value from SharedPreferences */
    private static int getPref(SharedPreferences prefs, String key, int defaultValue) {
        String def = "" + defaultValue ;
        String value         = prefs.getString(key, def);

        if ((def == null) || (def.trim().length() == 0)) return defaultValue;
        return Integer.valueOf(value);
    }

    /** load value from SharedPreferences */
    private static boolean getPref(SharedPreferences prefs, String key, boolean defaultValue) {
        return prefs.getBoolean(key,defaultValue);

        /*
        String def = "" + defaultValue ;
        String value         = prefs.getString(key, def);

        if ((def == null) || (def.trim().length() == 0)) return defaultValue;
        return Boolean.valueOf(value);
        */
    }

    public static void show(Activity parent) {
        Intent intent = new Intent(parent, SettingsActivity.class);
        parent.startActivity(intent);
    }
}
