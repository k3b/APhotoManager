package de.k3b.android.androFotoFinder.backup;

import android.content.Context;

import de.k3b.android.GuiUtil;
import de.k3b.android.androFotoFinder.AndroFotoFinderApp;
import de.k3b.zip.CompressJob;
import de.k3b.zip.ZipLog;

/**
 * adds
 * * Settings specific text adding for clipboard text
 * * android resource based error messages
 * * app specific footer text for text entries
 */
public class ApmZipCompressJob extends CompressJob {
    private final Context context;

    /**
     * Creates a job.
     * @param zipLog if not null collect diagnostics/debug messages to debugLogMessages.
     * @param fileLogInZip
     */
    public ApmZipCompressJob(Context context, ZipLog zipLog, String fileLogInZip) {
        super(zipLog, fileLogInZip);
        this.context = context;
    }
    //############ processing ########

    /** footer added to text collector. null means no text. */
    @Override protected String getTextFooter() {
        String result = "# " + AndroFotoFinderApp.getGetTeaserText(this.context, AndroFotoFinderApp.LINK_URL_CSV); // "Collected with ToGoZip version ...";

        final String versionName = GuiUtil.getAppVersionName(context);
        if (versionName != null) {
            result = result.replace("$versionName$", versionName);
        }

        return result;
    }
}
