package de.k3b.android.fotoviewer.directory;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import de.k3b.android.fotoviewer.Global;
import de.k3b.android.fotoviewer.queries.FotoSql;
import de.k3b.database.QueryParameter;
import de.k3b.io.Directory;
import de.k3b.io.DirectoryBuilder;

/**
 * Load Directory in a Background Task.<br>
 * Example usage
 * <pre>
    DirectoryLoaderTask loader = new DirectoryLoaderTask(getActivity(), "MyLoader") {
        // This is called when doInBackground() is finished
        protected void onPostExecute(Directory result) {
            updateGui(result);
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress) {
            setStatus("Loaded " + progress[0] + "/" + progress[1]);
        }
    };
    loader.execute(parameters);
 </pre>
 *
 * Created by k3b on 02.07.2015.
 */
public class DirectoryLoaderTask extends AsyncTask<QueryParameter, Integer, Directory> {
    // every 500 items the progress indicator is advanced
    private static final int PROGRESS_INCREMENT = 500;

    private final Activity context;
    private final String debugPrefix;

    public DirectoryLoaderTask(Activity context, String debugPrefix) {
        this.context = context;
        this.debugPrefix = debugPrefix;
    }

    protected Directory doInBackground(QueryParameter... queryParameter) {
        if (queryParameter.length != 1) throw new IllegalArgumentException();

        QueryParameter queryParameters = queryParameter[0];

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Uri.parse(queryParameters.toFrom()), queryParameters.toColumns(),
                    queryParameters.toAndroidWhere(), queryParameters.toAndroidParameters(), queryParameters.toOrderBy());

            int itemCount = cursor.getCount();
            final int expectedCount = itemCount + itemCount;

            publishProgress(itemCount, expectedCount);
            if (Global.debugEnabled)
                Log.i(Global.LOG_CONTEXT, debugPrefix + itemCount + " rows found for query " + queryParameters.toSqlString());

            DirectoryBuilder builder = new DirectoryBuilder();

            long startTime = SystemClock.currentThreadTimeMillis();
            int colText = cursor.getColumnIndex(FotoSql.SQL_COL_DISPLAY_TEXT);
            int colCount = cursor.getColumnIndex(FotoSql.SQL_COL_COUNT);
            int increment = PROGRESS_INCREMENT;
            while (cursor.moveToNext()) {
                builder.add(cursor.getString(colText), cursor.getInt(colCount));
                itemCount++;
                if ((--increment) <= 0) {
                    publishProgress(itemCount, expectedCount);
                    increment = PROGRESS_INCREMENT;

                    // Escape early if cancel() is called
                    if (isCancelled()) break;
                }
            }

            return builder.getRoot();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /*
    // This is called each time you call publishProgress()
    protected void onProgressUpdate(Integer... progress) {
        setProgressPercent(progress[0]);
    }

    // This is called when doInBackground() is finished
    protected void onPostExecute(Directory result) {
        showNotification("Downloaded " + result + " bytes");
    }
    */
    private static void usageExample(Activity context, QueryParameter parameters, String debugPrefix) {
        DirectoryLoaderTask loader = new DirectoryLoaderTask(context, debugPrefix) {
            // This is called when doInBackground() is finished
            protected void onPostExecute(Directory result) {
                // updateGui(result);
            }
            // This is called each time you call publishProgress()
            protected void onProgressUpdate(Integer... progress) {
                // setStatus("Loaded " + progress[0] + "/" + progress[1]);
            }

        };
        loader.execute(parameters);
    }
}
