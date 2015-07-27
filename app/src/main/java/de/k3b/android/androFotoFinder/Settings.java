/*
 * Copyright (c) 2015 by k3b.
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
