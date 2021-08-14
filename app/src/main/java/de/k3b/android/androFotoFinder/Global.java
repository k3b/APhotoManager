/*
 * Copyright (c) 2017-2020 by k3b.
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

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.Menu;

import java.io.File;
import java.util.Locale;

import de.k3b.android.util.MenuUtils;

/**
 * Global Settings used throughout the app.
 *
 * Non-final values can be modified by the app settings
 *
 * Created by k3b on 04.06.2015.
 */
public class Global {
    /**
     * LOG_CONTEXT is used as logging source for filtering logging messages that belong to this
     */
    public static final String LOG_CONTEXT = GlobalInit.LOG_CONTEXT;

    /**
     * local settings: which language should the gui use
     */
    public static final String PREF_KEY_USER_LOCALE = "user_locale";
    public static final boolean USE_ANDROID_FACADE = false;

    /**
     * Global.xxxxx. Non final values may be changed in SettingsActivity
     */

    /** #64: {@link Global#showEditChooser} : true => open editor via chooser. false: donot present chooser */
    public static boolean showEditChooser = false;
    public static boolean debugEnabled = false;
    public static boolean debugEnabledViewItem = false;
    public static boolean debugEnabledSql = false;
    public static boolean debugEnabledMap = false;
    public static boolean debugEnabledMemory = false;
    public static boolean locked = false; // true: modification is password-locked
    public static String passwordHash = ""; // password to unlock

    /** The maximum number of **Blue selection markers** in the [Geographic-Map](geographic-map). */
    public static int maxSelectionMarkersInMap = 255;

    /** #53, #83 if image-width-height is bigger than this show thumbnail in image detail view.
     * (memoryefficient, fast, but low-quality). -1: default to screenresolution */
    public static int imageDetailThumbnailIfBiggerThan = -1;

    /** where thumbnails are strored. defaults to /extDir/DCIM/.thumbCache */
    public static File thumbCacheRoot = null;

    /** defines the [Image-View's](Image-View) timing of menu command **slideshow** */
    public static int slideshowIntervalInMilliSecs = 1500;

    /** defines the timespan after which the [Image-View's](Image-View) ActionBar is hidden */
    public static int actionBarHideTimeInMilliSecs = 2000;

    /** If checked [multi selection mode](Gallery-View#Multiselection) in [Gallery-View](Gallery-View) is canceled after a command from Actionbar or Menu */
    public static boolean clearSelectionAfterCommand = false;

    /** true update only if media scanner is not running. false=risky=always allow.  */
    public static boolean mustCheckMediaScannerRunning = true;

    /** true every time a .nomedia dir/file is opend remeove items from db.  */
    public static final boolean mustRemoveNOMEDIAfromDB = true;

    // #127: Folderpicker: moving pathbar makes it discoverable. stop animation once the pathbar is touched
    public static final boolean showPathBarAnimation = true;

    public static final String reportExt = ".query";

    public static int pickHistoryMax = 25;

    /**
     * false: cmd setGeo => form(GeoEditActivity) => mapPicker
     */
    public static final boolean geoNoEdit = true;
    public static final boolean saveXmpAsHumanReadable = true;

    /**
     * true: cancel exifChange if DatabaseUpdate fails by throwing IOException
     */
    public static final boolean cancelExifChangeIfDatabaseUpdateFails = false;

    public static class Media {
        /**
         * Support extra parameters true: experimental. Not fully implemented yet.
         */
        public static final boolean enableIptcMediaScanner = true;

        /**
         * true: if there is no xmp-file or entry xmp-entry in csv mark this
         * SQL_COL_EXT_XMP_LAST_MODIFIED_DATE=EXT_LAST_EXT_SCAN_NO_XMP*.
         */
        public static final boolean enableXmpNone = enableIptcMediaScanner && true;
    }

    /** #26 which image resolution should the "non zoomed imageView" have? */
    public static boolean initialImageDetailResolutionHigh = false; // false: MediaStore.Images.Thumbnails.MINI_KIND; true: FULL_SCREEN_KIND;
    public static boolean mapsForgeEnabled = false;

    // #153: Feature-Toggel: set to false while rename multible is not implemented completely
    public final static boolean allowRenameMultible = false;

    // #155: Feature-Toggel: set to false while android10 incompatibility is not fixed
    public final static boolean allow_emulate_ao10 = !isAndroid10OrAbove() && false;
    // #155: fix android10 incompatibility
    // Build.VERSION_CODES.??ANDROID10?? = 29
    public static boolean useAo10MediaImageDbReplacement = isAndroid10OrAbove();

    // #169 Android SAF DocumentFileEx performance improvements fast find.
    // automatically Disabled, if there are saf-Write operations
    // automatically Enabled, if there are listFiles or listDirs
    public static boolean android_DocumentFile_find_cache = true;

    /**
     * map with blue selection markers: how much to area to increase
     */
    public static final double mapMultiselectionBoxIncreaseByProcent = 100.0;
    /**
     * map with blue selection markers: minimum size of zoom box in degrees
     */
    public static final double mapMultiselectionBoxIncreaseMinSizeInDegrees = 0.01;

    private static boolean isAndroid10OrAbove() {
        // #155: fix android10 incompatibility
        // Build.VERSION_CODES.??ANDROID10?? = 29
        return Build.VERSION.SDK_INT >= 29;
    }

    public static void debugMemory(String modul, String message) {
        if (Global.debugEnabledMemory) {
            Runtime r = Runtime.getRuntime();
            String formattedMessage = String.format(Locale.US, "memory : (total/free/avail) = (%3$dK/%4$dK/%5$dK)\t- %1$s.%2$s",
                    modul, message, r.totalMemory()/1024, r.freeMemory()/1024, r.maxMemory()/1024);

            Log.d(Global.LOG_CONTEXT, formattedMessage);
        }
    }

    /** Remember ininial language settings. This allows setting "switch back to device language" after changing app locale */
    public static Locale systemLocale = Locale.getDefault();

    /** move some pre-defined menu-actions into the "more..." submenue */
    public static void fixMenu(Context context, Menu menu) {
        MenuUtils.mov2SubMenu(menu, context.getString(R.string.more_menu_title),
                R.id.action_details,
                R.id.action_slideshow,
                R.id.cmd_filemanager,
                R.id.action_view_context_mode,
                // R.id.cmd_settings,
                R.id.cmd_selection_add_all,
                R.id.cmd_selection_remove_all,
                R.id.cmd_about,
                R.id.cmd_more,
                R.id.cmd_show_geo,
                R.id.cmd_gallery,
                R.id.cmd_app_unpin2,
                R.id.cmd_app_pin,
                R.id.cmd_show_geo_as
        );
    }
}
