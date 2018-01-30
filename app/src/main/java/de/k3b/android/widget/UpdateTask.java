package de.k3b.android.widget;

/**
 * Created by EVE on 20.11.2017.
 */

import android.app.Activity;
import android.util.Log;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.util.AndroidFileCommands;
import de.k3b.io.IProgessListener;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.media.MediaDiffCopy;

/** update exif changes in asynch task mit chow dialog */
public class UpdateTask extends AsyncTaskWithProgressDialog<SelectedFiles> implements IProgessListener {
    private static final String mDebugPrefix = "UpdateTaskAsync-";
    public static final int EXIF_RESULT_ID = 522;

    private MediaDiffCopy exifChanges;
    private final AndroidFileCommands cmd;

    public UpdateTask(Activity ctx, AndroidFileCommands cmd,
               MediaDiffCopy exifChanges) {
        super(ctx, R.string.exif_menu_title);
        this.exifChanges = exifChanges;
        this.cmd = cmd;
    }

    @Override
    protected Integer doInBackground(SelectedFiles... params) {
        publishProgress("...");

        if (exifChanges != null) {
            SelectedFiles items = params[0];

            return cmd.applyExifChanges(true, exifChanges, items, null);

        }
        return 0;
    }

    @Override
    protected void onPostExecute(Integer itemCount) {
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + " onPostExecute " + itemCount);
        }
        Activity parent = this.parent;
        super.onPostExecute(itemCount);
        parent.setResult(EXIF_RESULT_ID, parent.getIntent());
        parent.finish();
    }

    @Override
    public void destroy() {
        if (exifChanges != null) exifChanges.close();
        exifChanges = null;
        super.destroy();
    }

    public boolean isEmpty() {
        return (exifChanges == null);
    }

    /**
     * called every time when command makes some little progress. Can be mapped to async progress-bar
     *
     * @param itemcount
     * @param total
     * @param message
     */
    @Override
    public boolean onProgress(int itemcount, int total, String message) {
        publishProgress(itemcount, total, message);
        return !isCancelled();
    }
}

