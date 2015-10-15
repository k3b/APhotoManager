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
 
package de.k3b.android.androFotoFinder.gallery.cursor;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.OnGalleryInteractionListener;
import de.k3b.android.androFotoFinder.R;
import de.k3b.database.SelectedItems;

/**
 * CursorAdapter that queries MediaStore.Images.Media.EXTERNAL_CONTENT_URI
 * for a GridViewItem (gridCell)
 *
 * Created by k3b on 02.06.2015.
 */
public class GalleryCursorAdapter extends CursorAdapter {
    // Identifies a particular Loader or a LoaderManager being used in this component
    private static final boolean SYNC = false;
    private final SelectedItems mSelectedItems;
    private OnGalleryInteractionListener callback = null;

    // for debugging
    private static int id = 1;
    private final String debugPrefix;

    // for debugging: counts how many cell elements were created
    protected StringBuffer mStatus = null;

    private final Drawable imageNotLoadedYet;

    public GalleryCursorAdapter(final Activity context, SelectedItems selectedItems, String name) {
        super(context, null, false); // no cursor yet; no auto-requery
        mSelectedItems = selectedItems;

        debugPrefix = "GalleryCursorAdapter#" + (id++) + "@" + name + " ";
        Global.debugMemory(debugPrefix, "ctor");

        imageNotLoadedYet = context.getResources().getDrawable(R.drawable.image_loading);

        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + "()");
        }
        if (context instanceof OnGalleryInteractionListener) {
            this.callback = (OnGalleryInteractionListener) context;
        }
    }

    private String debugCursor(Cursor cursor, int maxRows, String delim, String... colmnNames) {
        StringBuilder result = new StringBuilder();
        if ((cursor != null) && (!cursor.isClosed())) {
            int oldPosition = cursor.getPosition();
            int last = Math.min(maxRows - 1, cursor.getCount() - 1);
            for (int position = 0; position <= last; position ++) {
                result.append("#").append(position);
                cursor.moveToPosition(position);
                for (String col : colmnNames) {
                    result.append(";").append(cursor.getString(cursor.getColumnIndex(col)));
                }
                result.append(delim);
            }
        }
        return result.toString();
    }

    /**
     * Get the type of View that will be created by {@link #getView} for the specified item.
     *
     * @param position The position of the item within the adapter's data set whose view type we
     *                 want.
     * @return An integer representing the type of View. Two views should share the same type if one
     * can be converted to the other in {@link #getView}. Note: Integers must be in the
     * range 0 to {@link #getViewTypeCount} - 1. {@link #IGNORE_ITEM_VIEW_TYPE} can
     * also be returned.
     * @see #IGNORE_ITEM_VIEW_TYPE
     */
    @Override
    public int getItemViewType(int position) {
        return R.layout.grid_item_gallery;
    }

    /** create new empty gridview cell */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View iView = View.inflate(context, R.layout.grid_item_gallery, null);
        GridCellViewHolder holder = new GridCellViewHolder(iView);
        iView.setTag(holder);

        // iView.setLayoutParams(new GridView.LayoutParams(200, 200));
        if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, debugPrefix + "newView " + holder);
        return iView;
    }

    /** load data from cursor-position into gridview cell */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final GridCellViewHolder holder = (GridCellViewHolder) view.getTag();
        long count = cursor.getLong(cursor.getColumnIndex(FotoSql.SQL_COL_COUNT));
        boolean gps = !cursor.isNull(cursor.getColumnIndex(FotoSql.SQL_COL_GPS));

        final int columnIndexWhereParam = cursor.getColumnIndex(FotoSql.SQL_COL_WHERE_PARAM);
        holder.filter =  (columnIndexWhereParam >= 0) ? cursor.getString(columnIndexWhereParam) : null;

        String description = cursor.getString(cursor.getColumnIndex(FotoSql.SQL_COL_DISPLAY_TEXT));
        if (count > 1) description += " (" + count + ")";
        if (gps) description += "#";
        holder.description.setText(description);
        long imageID = cursor.getLong(cursor.getColumnIndex(FotoSql.SQL_COL_PK));
        holder.icon.setVisibility(((mSelectedItems != null) && (mSelectedItems.contains(imageID))) ? View.VISIBLE : View.GONE);

        holder.loadImageInBackground(imageID,imageNotLoadedYet );
        if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, debugPrefix + "bindView for " + holder);
    }

    @Override
    public String toString() {
        return debugPrefix;
    }

    /** data belonging to gridview element */
    static class GridCellViewHolder {
        private static int lastInstanceNo = 0;
        private final String debugPrefix;

        final public ImageView image;
        final public ImageView icon;
        final public TextView description;
        private DownloadImageTask downloader = null;

        /** onClick add this as sql-where-filter */
        public String filter;

        /** for delay loading */
        public long imageID = 0;

        GridCellViewHolder(View parent) {
            lastInstanceNo++;
            debugPrefix = "Holder@" + lastInstanceNo + "#";

            this.description = (TextView) parent.findViewById(R.id.text);
            this.image = (ImageView) parent.findViewById(R.id.image);
            this.icon = (ImageView) parent.findViewById(R.id.icon);
        };

        @Override
        public String toString() {
            return debugPrefix + this.imageID;
        }

        public void loadImageInBackground(long imageID, Drawable imageNotLoadedYet) {
            if (imageID != this.imageID) {
                // to avoid reload the same again
                if (downloader != null) {
                    downloader.cancel(false);
                    downloader = null;
                    if (Global.debugEnabledViewItem)
                        Log.i(Global.LOG_CONTEXT, "loadImageInBackground.cancel " + this);
                }

                this.imageID = imageID;
                image.setImageDrawable(imageNotLoadedYet);
                this.downloader = new DownloadImageTask();
                if (Global.debugEnabledViewItem)
                    Log.i(Global.LOG_CONTEXT, "loadImageInBackground.execute " + this);
                downloader.execute(this);
            }
        }
    }

	// new DownloadImageTask(mContext).execute(holder);
	// from https://developer.android.com/guide/components/processes-and-threads.html#WorkerThreads
	private static class DownloadImageTask extends AsyncTask<GridCellViewHolder, Void, Bitmap> {
        private GridCellViewHolder holder;

		/** The system calls this to perform work in a worker thread and
		  * delivers it the parameters given to AsyncTask.execute() */
		protected Bitmap doInBackground(GridCellViewHolder... holders) {
            this.holder = holders[0];
            if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, "GridCellImageLoadHandler.handleMessage getThumbnail for " + holder);
            Bitmap image = getBitmap(holder.imageID);
			return image;
        }

        private Bitmap getBitmap(Long id) {
            final Bitmap thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                    holder.image.getContext().getContentResolver(),
                    id,
                    MediaStore.Images.Thumbnails.MICRO_KIND,
                    new BitmapFactory.Options());

            return thumbnail;
        }

		/** The system calls this to perform work in the UI thread and delivers
		  * the result from doInBackground() */
		protected void onPostExecute(Bitmap image) {
            holder.downloader = null;
            if (!isCancelled()) {
                if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, "loadImageInBackground.done " + holder);
                this.holder.image.setImageBitmap(image);
            }
            this.holder = null;
		}
	}
	
}
