package io.github.lonamiwebs.stringlate.utilities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.List;

// Stringlate API
// https://github.com/LonamiWebs/Stringlate/blob/master/src/app/src/main/java/io/github/lonamiwebs/stringlate/utilities/Api.java
//
// External applications are free to copy and redistribute this
// file on their projects in order to make a fair use of it.
//
// Otherwise, they are free to use this file as reference and
// implement their own solutions.
public class Api {

    //region Available actions

    // ACTION_TRANSLATE:
    //   Opens Stringlate with one of the following three behaviours:
    //
    //   * If EXTRA_GIT_URL is not given, Stringlate will finish() with
    //     no further action being taken.
    //
    //   * If EXTRA_GIT_URL is given, despite being valid or not:
    //     a) If this repository has already been added by the user before,
    //        the `Translate` activity will be opened automatically.
    //
    //     b) If this repository has not yet been added, the `Add repository`
    //        fragment will be shown, with the given URL automatically entered.
    public static final String ACTION_TRANSLATE = "io.github.lonamiwebs.stringlate.TRANSLATE";

    // used to install stringlate from fdroid-app-store
    // works on my android-4.4 phone :-)
    public static final String APP_PKG_NAME = "io.github.lonamiwebs.stringlate";
    public static final String APP_MARKET_URL = "market://details?id=" + APP_PKG_NAME;
    // public static final String APP_MARKET_URL = "market://search?q=" + APP_PKG_NAME;

    // see http://stackoverflow.com/questions/6813322/install-uninstall-apks-programmatically-packagemanager-vs-intents
    // does not work on my android-4.4 phone
    public static final String APP_PACKAGE_URL = "package:" + APP_PKG_NAME;
    //endregion

    //region Extras

    // EXTRA_GIT_URL
    //   Used to pass a String representing the git URL pointing to the
    //   source code of the application that should be opened for translation.
    public static final String EXTRA_GIT_URL = "GIT_URL";

    //endregion

    //region Mime type

    // You *must* manually define an intent's `mime-type` when using a
    // custom action with data (http://stackoverflow.com/a/12315898/4759433).
    //
    // Since there is no uri implied (only custom extras) it is recommended
    // to use "text/plain", but any mime type (existing or not) can be used.
    public static final String MIME_TYPE = "text/plain";

    //endregion

    //region Public methods

    /**
     * ACTION_TRANSLATE wrapper.
     *
     * example use: api.translate(getActivity(), "https://github.com/LonamiWebs/Stringlate");
     *
     * @param context activity that asks for translation service.
     * @throws ActivityNotFoundException
     */
    public static void translate(final Context context, final String gitUrl) throws ActivityNotFoundException {
        Intent intent = createTranslateIntent(gitUrl);
        context.startActivity(intent);
    }

    /**
     * @return true, if Stringlate ver 0.9.9 or up is installed
     */
    public static boolean isInstalled(final Context context) {
        Intent intent = createTranslateIntent("");
        return isInstalled(context, intent);
    }

    /**
     * @return true, if an appstore is installed that can be used to install stringlate
     */
    public static boolean canInstall(final Context context) {
        Intent intent = createInstallIntent();
        return isInstalled(context, intent);
    }

    /**
     * Installs Stringlate from f-droid app store
     *
     * @param context activity that asks stringlate installation.
     * @param requestCode If > 0, this code will be returned in
     *                    onActivityResult() when the activity exits.
     */
    public static void install(final Activity context, int requestCode) throws ActivityNotFoundException {
        Intent intent = createInstallIntent();
        if (requestCode > 0) {
            context.startActivityForResult(intent, requestCode);
        } else {
            context.startActivity(intent);
        }
    }

    /**
     * Local helper to test if an intent is executable
     *
     * @return true, is installed
     */
    private static boolean isInstalled(Context context, Intent intent) {
        final List<ResolveInfo> list = ((PackageManager)context.getPackageManager()).queryIntentActivities(intent, 0);

        return ((list != null) && (list.size() > 0));
    }

    /**
     * Local helper to create the translate intent
     */
    @NonNull
    private static Intent createTranslateIntent(String gitUrl) {
        Intent intent = new Intent(ACTION_TRANSLATE);
        intent.setType(MIME_TYPE);
        intent.putExtra(EXTRA_GIT_URL, gitUrl);
        // intent.setClassName("io.github.lonamiwebs.stringlate.activities.repositories","RepositoriesActivity");
        return intent;
    }

    /**
     * Local helper to create the install intent
     */
    @NonNull
    private static Intent createInstallIntent() {
        // see http://stackoverflow.com/questions/6813322/install-uninstall-apks-programmatically-packagemanager-vs-intents
        // does not work on my android-4.4 phone :-(
        // Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE, Uri.parse(APP_PACKAGE_URL));
        // intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);

        // works on my android-4.4 phone :-)
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(APP_MARKET_URL));
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        return intent;
    }

    //endregion
}
