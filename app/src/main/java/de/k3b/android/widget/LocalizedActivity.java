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

package de.k3b.android.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.util.Locale;

import de.k3b.android.androFotoFinder.Global;

/**
 * An activity that can change the locale (language) of its content.
 *
 * Inspired by http://stackoverflow.com/questions/13181847/change-the-locale-at-runtime
 *
 * Created by k3b on 07.01.2016.
 */

public abstract class LocalizedActivity extends Activity {
    /** if myLocale != Locale.Default : activity must be recreated in on resume */
    private Locale myLocale = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        fixLocale(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Locale has changed by other Activity ?
        if ((myLocale != null) && (myLocale.getLanguage() != Locale.getDefault().getLanguage())) {
            myLocale = null;
            recreate();
        }
    }

    /**
     * Set Activity-s locale to SharedPreferences-setting.
     * Must be called before
     */
    public static void fixLocale(Context context)
    {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        String language = prefs.getString(Global.PREF_KEY_USER_LOCALE, "");
        Locale locale = Global.systemLocale; // in case that setting=="use android-locale"
        if ((language != null) && (!language.isEmpty())) {
            locale = new Locale(language); // overwrite "use android-locale"
        }

        if (locale != null) {
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            Resources resources = context.getResources();
            resources.updateConfiguration(config, resources.getDisplayMetrics());
            // recreate();

            if (context instanceof LocalizedActivity) {
                ((LocalizedActivity) context).myLocale = locale;
            }
        }
    }

    /** force all open activity to recreate */
    public static void recreate(Activity child) {
        Activity context = child;
        while (context != null) {
            context.recreate();
            context = context.getParent();
        }

    }
}
