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
     * @param context
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

    public static void recreate(Activity context) {
        while (context != null) {
            context.recreate();
            context = context.getParent();
        }

    }
}
