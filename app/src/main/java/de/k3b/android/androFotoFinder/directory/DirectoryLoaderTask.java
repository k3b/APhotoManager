package de.k3b.android.androFotoFinder.directory;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;

import java.util.List;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.database.QueryParameter;
import de.k3b.io.Directory;
import de.k3b.io.DirectoryBuilder;
import de.k3b.io.DirectoryFormatter;

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
        Global.debugMemory(debugPrefix, "ctor");
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

            DirectoryBuilder builder = new DirectoryBuilder();

            long startTime = SystemClock.currentThreadTimeMillis();
            int colText = cursor.getColumnIndex(FotoSql.SQL_COL_DISPLAY_TEXT);
            int colCount = cursor.getColumnIndex(FotoSql.SQL_COL_COUNT);
            int colIconID = cursor.getColumnIndex(FotoSql.SQL_COL_PK);

            int colLat = cursor.getColumnIndex(FotoSql.SQL_COL_LAT);
            int colLon = cursor.getColumnIndex(FotoSql.SQL_COL_LON);

            int increment = PROGRESS_INCREMENT;
            while (cursor.moveToNext()) {
                String path = (colText >= 0) ? cursor.getString(colText) : getLatLonPath(cursor.getDouble(colLat), cursor.getDouble(colLon));
                if (path != null) {
                    builder.add(path, cursor.getInt(colCount), cursor.getInt(colIconID));
                    itemCount++;
                    if ((--increment) <= 0) {
                        publishProgress(itemCount, expectedCount);
                        increment = PROGRESS_INCREMENT;

                        // Escape early if cancel() is called
                        if (isCancelled()) break;
                    }
                }
            }

            Directory result = builder.getRoot();
            if (colText < 0) {
                compressLatLon(result);
            }
            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void compressLatLon(Directory result) {
        List<Directory> children = (result != null) ? result.getChildren() : null;

        if (children != null) {
            for (Directory child : children) {
                if (child.getRelPath().indexOf("/") > 0) {
                    child.setRelPath(DirectoryFormatter.getLastPath(child.getRelPath()));
                }
                compressLatLon(child);
            }
        }
    }

    private String getLatLonPath(double latitude, double longitude) {
        String result = DirectoryFormatter.getLatLonPath(latitude, longitude);
        // if (result == null) return this.context.getString(R.string.unknown);
        return result;
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
