package de.k3b.android.widget;

/**
 * Created by EVE on 20.11.2017.
 */

import android.app.Activity;
import android.util.Log;

import java.io.File;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.util.AndroidFileCommands;
import de.k3b.io.IProgessListener;
import de.k3b.io.PhotoAutoprocessingDto;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.media.PhotoPropertiesDiffCopy;

/** update exif changes in asynch task mit chow dialog */
public class UpdateTask extends AsyncTaskWithProgressDialog<SelectedFiles> implements IProgessListener {
    private static final String mDebugPrefix = "UpdateTaskAsync-";
    public static final int EXIF_RESULT_ID = 522;

    private PhotoPropertiesDiffCopy exifChanges;
    private final AndroidFileCommands cmd;

    private boolean move;
    private File destDirFolder;
    private PhotoAutoprocessingDto autoProccessData;

    public UpdateTask(int resIdDlgTitle, Activity ctx, AndroidFileCommands cmd,
                      PhotoPropertiesDiffCopy exifChanges) {
        this(resIdDlgTitle, ctx, cmd, exifChanges, true, null, null);
    }

    public UpdateTask(int resIdDlgTitle, Activity ctx, AndroidFileCommands cmd,
                       boolean move, File destDirFolder,
                       PhotoAutoprocessingDto autoProccessData) {
        this(resIdDlgTitle, ctx, cmd, null, move, destDirFolder, autoProccessData);
    }

    private UpdateTask(int resIdDlgTitle, Activity ctx, AndroidFileCommands cmd,
                      PhotoPropertiesDiffCopy exifChanges,
                      boolean move, File destDirFolder,
                      PhotoAutoprocessingDto autoProccessData) {
        super(ctx, resIdDlgTitle);
        this.exifChanges = exifChanges;
        this.cmd = cmd;
        this.move = move;
        this.autoProccessData = autoProccessData;
        this.destDirFolder = destDirFolder;
    }

    @Override
    protected Integer doInBackground(SelectedFiles... params) {
        publishProgress("...");

        if (exifChanges != null) {
            SelectedFiles items = params[0];

            return cmd.applyExifChanges(move, exifChanges, items, this);

        } else {
            SelectedFiles items = params[0];

            return cmd.moveOrCopyFilesTo(move, items, destDirFolder, autoProccessData, this);

        }
    }

    @Override
    protected void onPostExecute(Integer itemCount) {
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + " onPostExecute " + itemCount);
        }
        Activity parent = this.getActivity();
        super.onPostExecute(itemCount);
        if (parent != null) {
            parent.setResult(EXIF_RESULT_ID, parent.getIntent());
            parent.finish();
        }
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

