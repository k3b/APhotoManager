/*
 * Copyright (c) 2015-2018 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;

import java.io.File;

import de.k3b.FotoLibGlobal;
import de.k3b.android.androFotoFinder.imagedetail.HugeImageLoader;
import de.k3b.android.util.MediaScanner;
import de.k3b.android.util.MediaScannerExifInterface;
import de.k3b.android.util.MediaScannerImageMetaReader;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.LocalizedActivity;
import de.k3b.tagDB.TagRepository;
import io.github.lonamiwebs.stringlate.utilities.Api;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.log.LogManager;

public class SettingsActivity extends PreferenceActivity {
    private static Boolean sOldEnableNonStandardIptcMediaScanner = null;
    private SharedPreferences prefsInstance = null;
    private ListPreference defaultLocalePreference;  // #21: Support to change locale at runtime
    private ListPreference mediaUpdateStrategyPreference;

    private int INSTALL_REQUEST_CODE = 1927;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LocalizedActivity.fixLocale(this);	// #21: Support to change locale at runtime
        super.onCreate(savedInstanceState);

        if (Global.debugEnabled) {
            // todo create junit integration tests with arabic locale from this.
            StringFormatResourceTests.test(this);
        }

        final Intent intent = getIntent();
        if (Global.debugEnabled && (intent != null)){
            Log.d(Global.LOG_CONTEXT, "SettingsActivity onCreate " + intent.toUri(Intent.URI_INTENT_SCHEME));
        }

        this.addPreferencesFromResource(R.xml.preferences);
        prefsInstance = PreferenceManager
                .getDefaultSharedPreferences(this);
        global2Prefs(this.getApplication());

		// #21: Support to change locale at runtime
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

        mediaUpdateStrategyPreference =
                (ListPreference) findPreference("mediaUpdateStrategy");
        mediaUpdateStrategyPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                FotoLibGlobal.mediaUpdateStrategy = (String) newValue;
                setPref(FotoLibGlobal.mediaUpdateStrategy, mediaUpdateStrategyPreference, R.array.pref_media_update_strategy_names);
                return true;
            }
        });
        setPref(FotoLibGlobal.mediaUpdateStrategy, mediaUpdateStrategyPreference, R.array.pref_media_update_strategy_names);

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
        findPreference("translate").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                onTranslate();
                return false; // donot close
            }
        });
		
		// #21: Support to change locale at runtime
        updateSummary();
    }

    @Override
    public void onPause() {
        prefs2Global(this);
        super.onPause();
    }

    public static void global2Prefs(Context context) {
        fixDefaults(context, null, null);

        SharedPreferences prefsInstance = PreferenceManager
                .getDefaultSharedPreferences(context);

        SharedPreferences.Editor prefs = prefsInstance.edit();
        prefs.putBoolean("debugEnabled", Global.debugEnabled);
        prefs.putBoolean("debugEnabledViewItem", Global.debugEnabledViewItem);
        prefs.putBoolean("debugEnabledSql", Global.debugEnabledSql);
        prefs.putBoolean("debugEnabledMap", Global.debugEnabledMap);

        prefs.putBoolean("debugEnabledMemory", Global.debugEnabledMemory);

        prefs.putBoolean("debugEnabledJpgMetaIo", FotoLibGlobal.debugEnabledJpgMetaIo);
        prefs.putBoolean("debugEnabledJpg", FotoLibGlobal.debugEnabledJpg);

        /** #100: true: private images get the extension ".jpg-p" which hides them from other gallery-apps and image pickers.  */
        prefs.putBoolean("renamePrivateJpg", FotoLibGlobal.renamePrivateJpg);

        // #26
        prefs.putBoolean("initialImageDetailResolutionHigh", Global.initialImageDetailResolutionHigh);

        prefs.putBoolean("debugEnableLibs", PhotoViewAttacher.DEBUG);

        prefs.putBoolean("clearSelectionAfterCommand", Global.clearSelectionAfterCommand);
        prefs.putBoolean("xmp_file_schema_long", FotoLibGlobal.preferLongXmpFormat);

        prefs.putBoolean("mapsForgeEnabled", Global.mapsForgeEnabled);

        prefs.putBoolean("locked", Global.locked);
        prefs.putString("passwordHash", Global.passwordHash);

        prefs.putString("imageDetailThumbnailIfBiggerThan", "" + Global.imageDetailThumbnailIfBiggerThan);
        prefs.putString("maxSelectionMarkersInMap", "" + Global.maxSelectionMarkersInMap);
        prefs.putString("slideshowIntervalInMilliSecs", "" + Global.slideshowIntervalInMilliSecs);
        prefs.putString("actionBarHideTimeInMilliSecs", "" + Global.actionBarHideTimeInMilliSecs);
        prefs.putString("pickHistoryMax", "" + Global.pickHistoryMax);

        prefs.putString("reportDir", (Global.reportDir != null) ? Global.reportDir.getAbsolutePath() : null);
        prefs.putString("logCatDir", (Global.logCatDir != null) ? Global.logCatDir.getAbsolutePath() : null);
        prefs.putString("thumbCacheRoot", (Global.thumbCacheRoot != null) ? Global.thumbCacheRoot.getAbsolutePath() : null);
        prefs.putString("mapsForgeDir", (Global.mapsForgeDir != null) ? Global.mapsForgeDir.getAbsolutePath() : null);

        prefs.putString("pickHistoryFile", (Global.pickHistoryFile != null) ? Global.pickHistoryFile.getAbsolutePath() : null);

        prefs.putString("mediaUpdateStrategy", FotoLibGlobal.mediaUpdateStrategy);

        prefs.apply();

    }

    public static void prefs2Global(Context context) {
        File previousCacheRoot = Global.thumbCacheRoot;
        File previousMapsForgeDir = Global.mapsForgeDir;

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext());
        Global.debugEnabled                     = getPref(prefs, "debugEnabled", Global.debugEnabled);
        FotoLibGlobal.debugEnabled = Global.debugEnabled;

        Global.debugEnabledViewItem             = getPref(prefs, "debugEnabledViewItem", Global.debugEnabledViewItem);
        Global.debugEnabledSql                  = getPref(prefs, "debugEnabledSql", Global.debugEnabledSql);

        Global.debugEnabledMap                  = getPref(prefs, "debugEnabledMap", Global.debugEnabledMap);

        Global.debugEnabledMemory               = getPref(prefs, "debugEnabledMemory", Global.debugEnabledMemory);

        Global.locked                           = getPref(prefs, "locked", Global.locked);
        Global.passwordHash                     = getPref(prefs, "passwordHash", Global.passwordHash);

        FotoLibGlobal.debugEnabledJpg = getPref(prefs, "debugEnabledJpg", FotoLibGlobal.debugEnabledJpg);
        FotoLibGlobal.debugEnabledJpgMetaIo     = getPref(prefs, "debugEnabledJpgMetaIo", FotoLibGlobal.debugEnabledJpgMetaIo);

        /** #100: true: private images get the extension ".jpg-p" which hides them from other gallery-apps and image pickers.  */
        FotoLibGlobal.renamePrivateJpg          = getPref(prefs, "renamePrivateJpg", FotoLibGlobal.renamePrivateJpg);

        // one setting for several 3d party debug-flags
        boolean debug3rdParty                   = getPref(prefs, "debugEnableLibs", PhotoViewAttacher.DEBUG);

        PhotoViewAttacher.DEBUG                 = debug3rdParty;
        HugeImageLoader.DEBUG                   = debug3rdParty;
        ThumbNailUtils.DEBUG = debug3rdParty;
        LogManager.setDebugEnabled(debug3rdParty);
        com.nostra13.universalimageloader.utils.L.writeDebugLogs(debug3rdParty);
        com.nostra13.universalimageloader.utils.L.writeLogs(debug3rdParty);

        // details osmdroid debugging only if Global.debugEnabledMap && debug3rdParty
        // OpenStreetMapTileProviderConstants.DEBUG_TILE_PROVIDERS = Global.debugEnabledMap && debug3rdParty;
        // OpenStreetMapTileProviderConstants.DEBUGMODE = Global.debugEnabledMap && debug3rdParty;

        // #26
        Global.initialImageDetailResolutionHigh = getPref(prefs, "initialImageDetailResolutionHigh", Global.initialImageDetailResolutionHigh);

        Global.clearSelectionAfterCommand       = getPref(prefs, "clearSelectionAfterCommand", Global.clearSelectionAfterCommand);
        FotoLibGlobal.preferLongXmpFormat       = getPref(prefs, "xmp_file_schema_long", FotoLibGlobal.preferLongXmpFormat);

        Global.mapsForgeEnabled                 = getPref(prefs, "mapsForgeEnabled", Global.mapsForgeEnabled);

                
        Global.imageDetailThumbnailIfBiggerThan = getPref(prefs, "imageDetailThumbnailIfBiggerThan"     , Global.imageDetailThumbnailIfBiggerThan);

        Global.maxSelectionMarkersInMap         = getPref(prefs, "maxSelectionMarkersInMap"     , Global.maxSelectionMarkersInMap);
        Global.slideshowIntervalInMilliSecs = getPref(prefs, "slideshowIntervalInMilliSecs", Global.slideshowIntervalInMilliSecs);
        Global.actionBarHideTimeInMilliSecs     = getPref(prefs, "actionBarHideTimeInMilliSecs" , Global.actionBarHideTimeInMilliSecs);
        Global.pickHistoryMax = getPref(prefs, "pickHistoryMax"               , Global.pickHistoryMax);

        Global.reportDir                        = getPref(prefs, "reportDir", Global.reportDir);

        Global.logCatDir                        = getPref(prefs, "logCatDir", Global.logCatDir);

        Global.thumbCacheRoot                   = getPref(prefs, "thumbCacheRoot", Global.thumbCacheRoot);
        Global.mapsForgeDir                     = getPref(prefs, "mapsForgeDir", Global.mapsForgeDir);

        Global.pickHistoryFile                  = getPref(prefs, "pickHistoryFile", Global.pickHistoryFile);

        FotoLibGlobal.mediaUpdateStrategy       = getPref(prefs, "mediaUpdateStrategy", FotoLibGlobal.mediaUpdateStrategy);

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

        fixDefaults(context, previousCacheRoot, previousMapsForgeDir);
    }

    private static void fixDefaults(Context context, File previousCacheRoot, File previousMapsForgeDir) {
        boolean mustSave = false;

        // default: a litte bit more than screen size
        if ((Global.imageDetailThumbnailIfBiggerThan < 0) && (context instanceof Activity)) {
            Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            Global.imageDetailThumbnailIfBiggerThan = (int) (1.2 * Math.max(size.x, size.y));
            mustSave = true;
        }

        if (!isValidThumbDir(Global.thumbCacheRoot)) {
            File defaultThumbRoot = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), ".thumbCache");
            if (!isValidThumbDir(defaultThumbRoot)) {
                defaultThumbRoot = context.getDir(".thumbCache", MODE_PRIVATE);
                isValidThumbDir(defaultThumbRoot);
            }

            Global.thumbCacheRoot = defaultThumbRoot;
            mustSave = true;
        }

        if (Global.mapsForgeDir == null) // || (!previousMapsForgeDir.exists()))
        {
            File externalStorageDirectory = Environment.getExternalStorageDirectory();
            if (externalStorageDirectory == null) externalStorageDirectory = Environment.getDataDirectory();
            Global.mapsForgeDir = new File(externalStorageDirectory, "osmdroid");
            mustSave = true;
        }

        if (mustSave) {
            global2Prefs(context);
        }
        if ((previousCacheRoot != null) && (!previousCacheRoot.equals(Global.thumbCacheRoot))) {
            ThumbNailUtils.init(context, previousCacheRoot);
        }
        TagRepository.setInstance(Global.reportDir);

        // true if first run or change
        if ((sOldEnableNonStandardIptcMediaScanner == null) || (sOldEnableNonStandardIptcMediaScanner.booleanValue() != Global.Media.enableIptcMediaScanner)) {
            MediaScanner.setInstance((Global.Media.enableIptcMediaScanner) ? new MediaScannerImageMetaReader(context) : new MediaScannerExifInterface(context));
            sOldEnableNonStandardIptcMediaScanner = Global.Media.enableIptcMediaScanner;
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
        String value         = prefs.getString(key, null);
        if (isNullOrEmpty(value)) return defaultValue;

        return new File(value);
    }

    /** load value from SharedPreferences */
    private static String getPref(SharedPreferences prefs, String key, String defaultValue) {
        String value = prefs.getString(key, defaultValue);
        if (isNullOrEmpty(value)) return defaultValue;
        return value;
    }

    /** load value from SharedPreferences */
    private static int getPref(SharedPreferences prefs, String key, int defaultValue) {
        String value         = prefs.getString(key, null);
        if (isNullOrEmpty(value)) return defaultValue;

        // #73 fix NumberFormatException
        try {
            return Integer.valueOf(value);
        } catch (Exception ex) {
            Log.i(Global.LOG_CONTEXT, "SettingsActivity.getPref(key=" + key
                    +"): " + value+
                    " => " + ex.getMessage(),ex);
        }
        return defaultValue;
    }

    private static boolean isNullOrEmpty(String value) {
        return (value == null) || (value.trim().length() == 0);
    }

    /** load value from SharedPreferences */
    private static boolean getPref(SharedPreferences prefs, String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    public static void show(Activity parent) {
        Intent intent = new Intent(parent, SettingsActivity.class);
        parent.startActivity(intent);
    }
	
	// #21: Support to change locale at runtime
    // This is used to show the status of some preference in the description
    private void updateSummary() {
        final String languageKey = prefsInstance.getString(Global.PREF_KEY_USER_LOCALE, "");
        setLanguage(languageKey);
        AboutDialogPreference about =
                (AboutDialogPreference) findPreference("about");
        about.setTitle(AboutDialogPreference.getAboutTitle(this));
    }

	// #21: Support to change locale at runtime
    private void setLanguage(String languageKey) {
        setPref(languageKey, defaultLocalePreference, R.array.pref_locale_names);
    }

    private void setPref(String key, ListPreference listPreference, int arrayResourceId) {
        int index = listPreference.findIndexOfValue(key);
        String summary = "";

        if (index >= 0) {
            String[] names = this.getResources().getStringArray(arrayResourceId);
            if (index < names.length) {
                summary = names[index];
            }
        }
        listPreference.setSummary(summary);

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

    private void onTranslate() {
        if (!Api.isInstalled(this)) {
            // either ask or catch ActivityNotFoundException

            if (Api.canInstall(this)) {
                // either ask or catch ActivityNotFoundException
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle(R.string.settings_translate_title);
                builder.setMessage(R.string.message_translate_not_installed)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            final DialogInterface dialog,
                                            final int id) {
                                        Log.i(Global.LOG_CONTEXT, "SettingsActivity-Stringlate-start-install");
                                        Api.install(SettingsActivity.this, INSTALL_REQUEST_CODE);
                                    }
                                }
                        )
                        .setNegativeButton(android.R.string.no,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            final DialogInterface dialog,
                                            final int id) {
                                        dialog.cancel();
                                    }
                                }
                        );

                builder.create().show();
            } else { // neither stringlate nor f-droid-app-store installed
                Log.i(Global.LOG_CONTEXT, "SettingsActivity-Stringlate-install-appstore not found");
            }
        } else { // stringlate already installed
            translate();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INSTALL_REQUEST_CODE) {
            if (Api.isInstalled(this)) {
                Log.i(Global.LOG_CONTEXT, "SettingsActivity-Stringlate-install-success");
                translate();
            } else {
                Log.i(Global.LOG_CONTEXT, "SettingsActivity-Stringlate-install-error or canceled");
            }
        }
    }

    private void translate() {
        Log.i(Global.LOG_CONTEXT, "SettingsActivity-Stringlate-install-success");
        Api.translate(this, "https://github.com/k3b/APhotoManager");
    }
}
