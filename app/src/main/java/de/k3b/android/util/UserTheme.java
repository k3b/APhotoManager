/*
 * Copyright (c) 2019-2020 by vinceh121
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

package de.k3b.android.util;

import android.app.Activity;
import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;

/**
 * #162: change Theme at runtime
 */
public class UserTheme {
    public static final String PREF_KEY_USER_THEME = "user_theme";

    private static List<String> themeKeyList = null;
    private static int[] themeResIds = new int[]{R.style.AppTheme_Light, R.style.AppTheme_Dark};
    public static void setTheme(Activity act) {
        final String themeKey = getThemeKey(act);
        int themeID = getThemeID(act, themeKey);
        act.setTheme(themeID);
    }

    public static int getThemeID(Activity act, String themeKey) {
        if (themeKeyList == null) {
            themeKeyList = Arrays.asList(act.getResources().getStringArray(R.array.pref_themes_value_keys));
            if (themeKeyList.size() != themeResIds.length)
                throw new IllegalStateException("resource arrays pref_themes_xxx must have same size and order");
        }

        int index = 0;
        if ((themeKey != null) && !themeKey.isEmpty()) {
            index = themeKeyList.indexOf(themeKey);

            if (index < 0) {
                Log.e(Global.LOG_CONTEXT, "theme resource key " + themeKey +
                        " not found in " + themeKeyList);
                index = 0;
            }
        }
        return themeResIds[index];
    }

    public static String getThemeKey(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_KEY_USER_THEME, "Light");
    }
}
