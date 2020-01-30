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

import de.k3b.android.androFotoFinder.R;

/**
 * #162: change Theme at runtime
 */
public class UserTheme {
    public static final String PREF_KEY_USER_THEME = "user_theme";

    public static void setTheme(Activity act) {
        final String themeKey = getThemeKey(act);
        int themeID = ("Dark".compareTo(themeKey) != 0)
                ? R.style.AppTheme_Light
                : R.style.AppTheme_Dark;
        act.setTheme(themeID);
    }

    public static String getThemeKey(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_KEY_USER_THEME, "Light");
    }
}
