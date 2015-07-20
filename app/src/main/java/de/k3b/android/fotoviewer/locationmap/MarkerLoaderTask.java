package de.k3b.android.fotoviewer.locationmap;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.OverlayManager;

import java.util.HashMap;

import de.k3b.android.fotoviewer.Global;
import de.k3b.android.fotoviewer.R;
import de.k3b.android.fotoviewer.queries.FotoSql;
import de.k3b.android.osmdroid.DefaultResourceProxyImplEx;
import de.k3b.android.osmdroid.IconFactory;
import de.k3b.android.osmdroid.MarkerBase;
import de.k3b.database.QueryParameter;

/**
 * Load Marker Overlays in a Background Task.<br>
 * Example usage
 * <pre>
 MarkerLoaderTask loader = new MarkerLoaderTask(getActivity(), "MyLoader") {
 // This is called when doInBackground() is finished
 protected void onPostExecute(OverlayManager result) {
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
 * Created by k3b on 16.07.2015.
 */
public abstract class MarkerLoaderTask<MARKER extends MarkerBase> extends AsyncTask<QueryParameter, Integer, OverlayManager> {
    // every 500 items the progress indicator is advanced
    private static final int PROGRESS_INCREMENT = 500;

    private final Activity context;
    private final String debugPrefix;
    private final IconFactory mIconFactory;
    private final DefaultResourceProxyImplEx mResourceProxy;
    protected HashMap<Integer, MARKER> mOldItems;
    protected StringBuffer mStatus = null;
    private int mStatisticsRecycled = 0;

    public MarkerLoaderTask(Activity context, String debugPrefix, HashMap<Integer, MARKER> oldItems) {
        if (Global.debugEnabled) {
            mStatus = new StringBuffer();
        }

        Global.debugMemory(debugPrefix, "ctor");
        this.context = context;
        this.debugPrefix = debugPrefix;
        mOldItems = oldItems;
        mResourceProxy = new DefaultResourceProxyImplEx(context);
        mIconFactory = new IconFactory(mResourceProxy, context.getResources().getDrawable(R.drawable.marker_green));
    }

    protected abstract MARKER createMarker();

    @Override
    protected OverlayManager doInBackground(QueryParameter... queryParameter) {
        if (queryParameter.length != 1) throw new IllegalArgumentException();

        QueryParameter queryParameters = queryParameter[0];

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Uri.parse(queryParameters.toFrom()), queryParameters.toColumns(),
                    queryParameters.toAndroidWhere(), queryParameters.toAndroidParameters(), queryParameters.toOrderBy());

            int itemCount = cursor.getCount();
            final int expectedCount = itemCount + itemCount;

            publishProgress(itemCount, expectedCount);
            if (this.mStatus != null) {
                this.mStatus.append("'").append(itemCount).append("' rows found for query \n\t").append(queryParameters.toSqlString());
            }
            OverlayManager result = new OverlayManager(null);

            long startTime = SystemClock.currentThreadTimeMillis();
            int colCount = cursor.getColumnIndex(FotoSql.SQL_COL_COUNT);
            int colIconID = cursor.getColumnIndex(FotoSql.SQL_COL_PK);

            int colLat = cursor.getColumnIndex(FotoSql.SQL_COL_LAT);
            int colLon = cursor.getColumnIndex(FotoSql.SQL_COL_LON);

            int increment = PROGRESS_INCREMENT;
            while (cursor.moveToNext()) {
                int id = cursor.getInt(colIconID);
                MARKER marker = mOldItems.get(id);
                if (marker != null) {
                    // recycle existing
                    mOldItems.remove(id);
                    mStatisticsRecycled ++;
                } else {
                    marker = createMarker();
                    GeoPoint point = new GeoPoint(cursor.getDouble(colLat),cursor.getDouble(colLon));
                    BitmapDrawable icon = mIconFactory.createIcon(id, cursor.getString(colCount));
                    marker.set(id, point, icon,null );
                }

                result.add(marker);

                itemCount++;
                if ((--increment) <= 0) {
                    publishProgress(itemCount, expectedCount);
                    increment = PROGRESS_INCREMENT;

                    // Escape early if cancel() is called
                    if (isCancelled()) break;
                }
            }
            if (this.mStatus != null) {
                this.mStatus.append("\n\tRecycled : ").append(mStatisticsRecycled);
                // Log.i(Global.LOG_CONTEXT, debugPrefix + itemCount + this.mStatus);
            }

            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}
