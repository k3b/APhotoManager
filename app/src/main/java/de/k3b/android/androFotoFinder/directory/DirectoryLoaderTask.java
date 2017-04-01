/*
 * Copyright (c) 2015-2017 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
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
 
package de.k3b.android.androFotoFinder.directory;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.database.QueryParameter;
import de.k3b.io.Directory;
import de.k3b.io.DirectoryBuilder;
import de.k3b.io.DirectoryFormatter;
import de.k3b.io.IDirectory;

/**
 * Load Directory in a Background Task.<br>
 * Example usage
 * <pre>
    DirectoryLoaderTask loader = new DirectoryLoaderTask(getActivity(), "MyLoader") {
        // This is called when doInBackground() is finished
        @ Override
        protected void onPostExecute(IDirectory result) {
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
public class DirectoryLoaderTask extends AsyncTask<QueryParameter, Integer, IDirectory> {
    // every 500 items the progress indicator is advanced
    private static final int PROGRESS_INCREMENT = 500;

    private final Activity context;

    // will receive debug output
    private StringBuffer mStatus = null;

    protected Exception mException = null;

    public DirectoryLoaderTask(Activity context, String debugPrefix) {
        this.context = context;
        String combinedDebugPrefix = debugPrefix + "-DirectoryLoaderTask";
        Global.debugMemory(combinedDebugPrefix, "ctor");

        if (Global.debugEnabledSql || Global.debugEnabled) {
            mStatus = new StringBuffer();
            mStatus.append(combinedDebugPrefix);
        } else {
            mStatus = null;
        }

    }

    protected IDirectory doInBackground(QueryParameter... queryParameter) {
        mException = null;
        if (queryParameter.length != 1) throw new IllegalArgumentException();

        QueryParameter queryParameters = queryParameter[0];

        if (mStatus != null) {
            mStatus.append("\n\t");
            if (queryParameters != null) {
                mStatus.append(queryParameters.toSqlString());
            }
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Uri.parse(queryParameters.toFrom()), queryParameters.toColumns(),
                    queryParameters.toAndroidWhere(), queryParameters.toAndroidParameters(), queryParameters.toOrderBy());

            int itemCount = cursor.getCount();
            final int expectedCount = itemCount + itemCount;

            publishProgress(itemCount, expectedCount);

            DirectoryBuilder builder = new DirectoryBuilder();

            int colCount = cursor.getColumnIndex(FotoSql.SQL_COL_COUNT);
            int colIconID = cursor.getColumnIndex(FotoSql.SQL_COL_PK);

            int colText = cursor.getColumnIndex(FotoSql.SQL_COL_DISPLAY_TEXT);
            int colLat = cursor.getColumnIndex(FotoSql.SQL_COL_LAT);
            int colLon = cursor.getColumnIndex(FotoSql.SQL_COL_LON);

            int markerItemCount = 1;
            int increment = PROGRESS_INCREMENT;

            if (colIconID == -1) {
                throw new IllegalArgumentException("Missing SQL Column " + FotoSql.SQL_COL_PK);
            }

            if ((colText == -1) && ((colLat == -1) || (colLon == -1))) {
                throw new IllegalArgumentException("Missing SQL Column. Need either " +
                        FotoSql.SQL_COL_DISPLAY_TEXT +
                        " or " + FotoSql.SQL_COL_LAT +
                                " + " +FotoSql.SQL_COL_LON);
            }

            while (cursor.moveToNext()) {
                String path = (colText >= 0) ? cursor.getString(colText) : getLatLonPath(cursor.getDouble(colLat), cursor.getDouble(colLon));
                if (path != null) {
                    if (colCount != -1) markerItemCount = cursor.getInt(colCount);
                    builder.add(path, markerItemCount, cursor.getInt(colIconID));
                    itemCount++;
                    if ((--increment) <= 0) {
                        publishProgress(itemCount, expectedCount);
                        increment = PROGRESS_INCREMENT;

                        // Escape early if cancel() is called
                        if (isCancelled()) break;
                    }
                }
            }
            if (mStatus != null) {
                mStatus.append("\n\tfound ").append(itemCount).append(" db rows");
            }

            IDirectory result = builder.getRoot();
            if (colText < 0) {
                compressLatLon(result);
            }
            return result;
        } catch (Exception ex) {
            mException = ex;
            if (mStatus != null) {
                mStatus.append("\n\t").append(ex.getMessage());
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (mStatus != null) {
                if (Global.debugEnabledSql) {
                    Log.w(Global.LOG_CONTEXT, mStatus.toString());
                } else if (Global.debugEnabled) {
                    Log.i(Global.LOG_CONTEXT, mStatus.toString());
                }
            }

        }
    }

    private void compressLatLon(IDirectory result) {
        List<IDirectory> children = (result != null) ? result.getChildren() : null;

        if (children != null) {
            for (IDirectory _child: children) {
                Directory child = (Directory) _child;
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
}
