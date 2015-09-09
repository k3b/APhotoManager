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
 
package de.k3b.android.androFotoFinder.locationmap;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.OverlayManager;

import java.util.HashMap;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.queries.FotoSql;
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
    public static final int NO_MARKER_COUNT_LIMIT = 0;
    // every 500 items the progress indicator is advanced
    private static final int PROGRESS_INCREMENT = 500;

    private final Activity mContext;
    protected final String mDebugPrefix;
    private final IconFactory mIconFactory;
    private final DefaultResourceProxyImplEx mResourceProxy;
    private final int mMarkerCountLimit;
    protected HashMap<Integer, MARKER> mOldItems;
    protected StringBuffer mStatus = null;
    private int mStatisticsRecycled = 0;

    public MarkerLoaderTask(Activity context, String debugPrefix, HashMap<Integer, MARKER> oldItems, int markerCountLimit) {
        mMarkerCountLimit = markerCountLimit;
        if (Global.debugEnabledSql || Global.debugEnabled) {
            mStatus = new StringBuffer();
        }

        Global.debugMemory(debugPrefix, "ctor");
        this.mContext = context;
        this.mDebugPrefix = debugPrefix;
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
            cursor = mContext.getContentResolver().query(Uri.parse(queryParameters.toFrom()), queryParameters.toColumns(),
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
                    // recycle existing with same content
                    mOldItems.remove(id);
                    mStatisticsRecycled ++;
                } else {
                    marker = createMarker();
                    GeoPoint point = new GeoPoint(cursor.getDouble(colLat),cursor.getDouble(colLon));
                    BitmapDrawable icon = createIcon(cursor.getString(colCount));
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
                
                if ((mMarkerCountLimit != NO_MARKER_COUNT_LIMIT) && (itemCount >=mMarkerCountLimit))
                {
                    break;
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

    protected BitmapDrawable createIcon(String iconText) {
        return mIconFactory.createIcon(iconText);
    }

}
