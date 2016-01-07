package de.k3b.android.widget;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
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
public class LocalizedActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        fixLocale(this);
        super.onCreate(savedInstanceState);
    }

    public static void fixLocale(Context context)
    {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        String language = prefs.getString(Global.PREF_KEY_USER_LOCALE, "");
        Locale locale = Global.systemLocale;
        if ((language != null) && (!language.isEmpty())) {
            locale = new Locale(language);
        }
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        Resources resources = context.getResources();
        resources.updateConfiguration(config, resources.getDisplayMetrics());
        // recreate();
    }
}
