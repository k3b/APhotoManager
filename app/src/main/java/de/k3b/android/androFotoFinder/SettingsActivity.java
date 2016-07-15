/*
 * Copyright (c) 2015-2016 by k3b.
 *
 * This file is part of AndroFotoFinder.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */

package de.k3b.android.androFotoFinder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import java.io.File;

import de.k3b.FotoLibGlobal;
import de.k3b.android.androFotoFinder.imagedetail.HugeImageLoader;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.LocalizedActivity;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.log.LogManager;

public class SettingsActivity extends PreferenceActivity {
    private SharedPreferences prefsInstance = null;
    private ListPreference defaultLocalePreference;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LocalizedActivity.fixLocale(this);
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        if (Global.debugEnabled && (intent != null)){
            Log.d(Global.LOG_CONTEXT, "SettingsActivity onCreate " + intent.toUri(Intent.URI_INTENT_SCHEME));
        }

        this.addPreferencesFromResource(R.xml.preferences);
        prefsInstance = PreferenceManager
                .getDefaultSharedPreferences(this);
        global2Prefs(this.getApplication());
        defaultLocalePreference =
                (ListPreference) findPreference(Global.PREF_KEY_USER_LOCALE);

        defaultLocalePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setLanguage((String) newValue);
                LocalizedActivity.recreate(SettingsActivity.this);
                return true; // change is allowed
            }
        });

        findPreference("debugClearLog").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                onDebugClearLogCat();
                return false; // donot close
            }
        });
        findPreference("debugSaveLog").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                onDebugSaveLogCat();
                return false; // donot close
            }
        });
        updateSummary();
    }

    @Override
    public void onPause() {
        prefs2Global(this);
        super.onPause();
    }

    public static void global2Prefs(Context context) {
        fixDefaults(context, null);

        SharedPreferences prefsInstance = PreferenceManager
                .getDefaultSharedPreferences(context);

        SharedPreferences.Editor prefs = prefsInstance.edit();
        prefs.putBoolean("debugEnabled", Global.debugEnabled);
        prefs.putBoolean("debugEnabledViewItem", Global.debugEnabledViewItem);
        prefs.putBoolean("debugEnabledSql", Global.debugEnabledSql);
        prefs.putBoolean("debugEnabledMemory", Global.debugEnabledMemory);

        // #26
        prefs.putBoolean("initialImageDetailResolutionHigh", Global.initialImageDetailResolutionHigh);

        prefs.putBoolean("debugEnableLibs", PhotoViewAttacher.DEBUG);

        prefs.putBoolean("clearSelectionAfterCommand", Global.clearSelectionAfterCommand);

        prefs.putString("imageDetailThumbnailIfBiggerThan", "" + Global.imageDetailThumbnailIfBiggerThan);
        prefs.putString("maxSelectionMarkersInMap", "" + Global.maxSelectionMarkersInMap);
        prefs.putString("slideshowIntervalInMilliSecs", "" + Global.slideshowIntervalInMilliSecs);
        prefs.putString("actionBarHideTimeInMilliSecs", "" + Global.actionBarHideTimeInMilliSecs);
        prefs.putString("pickHistoryMax", "" + Global.pickHistoryMax);

        prefs.putString("reportDir", (Global.reportDir != null) ? Global.reportDir.getAbsolutePath() : null);
        prefs.putString("logCatDir", (Global.logCatDir != null) ? Global.logCatDir.getAbsolutePath() : null);
        prefs.putString("thumbCacheRoot", (Global.thumbCacheRoot != null) ? Global.thumbCacheRoot.getAbsolutePath() : null);
        prefs.putString("pickHistoryFile", (Global.pickHistoryFile != null) ? Global.pickHistoryFile.getAbsolutePath() : null);

        prefs.apply();

    }

    public static void prefs2Global(Context context) {
        File previousCacheRoot = Global.thumbCacheRoot;
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext());
        Global.debugEnabled                     = getPref(prefs, "debugEnabled", Global.debugEnabled);
        FotoLibGlobal.debugEnabled = Global.debugEnabled;

        Global.debugEnabledViewItem             = getPref(prefs, "debugEnabledViewItem", Global.debugEnabledViewItem);
        Global.debugEnabledSql                  = getPref(prefs, "debugEnabledSql", Global.debugEnabledSql);
        Global.debugEnabledMemory               = getPref(prefs, "debugEnabledMemory", Global.debugEnabledMemory);

        // #26
        Global.initialImageDetailResolutionHigh = getPref(prefs, "initialImageDetailResolutionHigh", Global.initialImageDetailResolutionHigh);

        // one setting for several 3d party debug-flags
        PhotoViewAttacher.DEBUG                 = getPref(prefs, "debugEnableLibs", PhotoViewAttacher.DEBUG);
        HugeImageLoader.DEBUG                   = PhotoViewAttacher.DEBUG;
        ThumbNailUtils.DEBUG = PhotoViewAttacher.DEBUG;
        LogManager.setDebugEnabled(PhotoViewAttacher.DEBUG);
        com.nostra13.universalimageloader.utils.L.writeDebugLogs(PhotoViewAttacher.DEBUG);
        com.nostra13.universalimageloader.utils.L.writeLogs(PhotoViewAttacher.DEBUG);

        Global.clearSelectionAfterCommand       = getPref(prefs, "clearSelectionAfterCommand", Global.clearSelectionAfterCommand);

        Global.imageDetailThumbnailIfBiggerThan = getPref(prefs, "imageDetailThumbnailIfBiggerThan"     , Global.imageDetailThumbnailIfBiggerThan);

        Global.maxSelectionMarkersInMap         = getPref(prefs, "maxSelectionMarkersInMap"     , Global.maxSelectionMarkersInMap);
        Global.slideshowIntervalInMilliSecs = getPref(prefs, "slideshowIntervalInMilliSecs", Global.slideshowIntervalInMilliSecs);
        Global.actionBarHideTimeInMilliSecs     = getPref(prefs, "actionBarHideTimeInMilliSecs" , Global.actionBarHideTimeInMilliSecs);
        Global.pickHistoryMax = getPref(prefs, "pickHistoryMax"               , Global.pickHistoryMax);

        Global.reportDir                        = getPref(prefs, "reportDir", Global.reportDir);
        Global.logCatDir                        = getPref(prefs, "logCatDir", Global.logCatDir);

        Global.thumbCacheRoot                  = getPref(prefs, "thumbCacheRoot", Global.thumbCacheRoot);
        Global.pickHistoryFile                  = getPref(prefs, "pickHistoryFile", Global.pickHistoryFile);

        /*
        // bool
        debugEnabled
        debugEnabledViewItem
        debugEnabledSql
        debugEnabledMemory
        initialImageDetailResolutionHigh
        clearSelectionAfterCommand

        // int
        maxSelectionMarkersInMap
        slideshowIntervalInMilliSecs
        actionBarHideTimeInMilliSecs

        // file
        reportDir
        logCatDir

        */

        fixDefaults(context, previousCacheRoot);
    }

    private static void fixDefaults(Context context, File previousCacheRoot) {
        // default: a litte bit more than screen size
        if ((Global.imageDetailThumbnailIfBiggerThan < 0) && (context instanceof Activity)) {
            Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            Global.imageDetailThumbnailIfBiggerThan = (int) (1.2 * Math.max(size.x, size.y));
        }

        if (!isValidThumbDir(Global.thumbCacheRoot)) {
            File defaultThumbRoot = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),".thumbCache");
            if (!isValidThumbDir(defaultThumbRoot)) {
                defaultThumbRoot = context.getDir(".thumbCache", MODE_PRIVATE);
                isValidThumbDir(defaultThumbRoot);
            }

            Global.thumbCacheRoot = defaultThumbRoot;
            global2Prefs(context);
        }

        if ((previousCacheRoot != null) && (!previousCacheRoot.equals(Global.thumbCacheRoot))) {
            ThumbNailUtils.init(context, previousCacheRoot);
        }
    }

    private static boolean isValidThumbDir(File thumbCacheRoot) {
        if (thumbCacheRoot == null) return false;

        File parent = thumbCacheRoot.getParentFile();
        if ((parent != null) && (parent.exists())) {
            thumbCacheRoot.mkdirs();
            return thumbCacheRoot.canWrite();
        }
        return false;
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
        return prefs.getBoolean(key, defaultValue);

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
    // This is used to show the status of some preference in the description
    private void updateSummary() {
        final String languageKey = prefsInstance.getString(Global.PREF_KEY_USER_LOCALE, "");
        setLanguage(languageKey);
        AboutDialogPreference about =
                (AboutDialogPreference) findPreference("about");
        about.setTitle(AboutDialogPreference.getAboutTitle(this));
    }

    private void setLanguage(String languageKey) {
        int index = defaultLocalePreference.findIndexOfValue(languageKey);
        String summary = "";

        if (index >= 0) {
            String[] names = this.getResources().getStringArray(R.array.pref_locale_names);
            if (index < names.length) {
                summary = names[index];
            }
        }
        defaultLocalePreference.setSummary(summary);
    }

    private void onDebugClearLogCat() {
        ((AndroFotoFinderApp) getApplication()).clear();
        Toast.makeText(this, R.string.settings_debug_clear_title, Toast.LENGTH_SHORT).show();
        Log.e(Global.LOG_CONTEXT, "SettingsActivity-ClearLogCat()");
    }

    private void onDebugSaveLogCat() {
        Log.e(Global.LOG_CONTEXT, "SettingsActivity-SaveLogCat()");
        ((AndroFotoFinderApp) getApplication()).saveToFile();
    }

}
