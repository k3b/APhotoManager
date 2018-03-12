/*
 * Copyright (c) 2015-2018 by k3b.
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
 
package de.k3b.android.androFotoFinder.gallery.cursor;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import de.k3b.android.androFotoFinder.AffUtils;
import de.k3b.android.androFotoFinder.ThumbNailUtils;
import de.k3b.android.androFotoFinder.imagedetail.HugeImageLoader;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.util.DBUtils;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.io.collections.SelectedItems;

/**
 * CursorAdapter that queries MediaStore.Images.Media.EXTERNAL_CONTENT_URI
 * for a GridViewItem (gridCell).
 *
* In android displaying an item with an image in a list or grid works like this (example):
* 
*     The grid needs to display item that is at position 35
*     The grid asks its adapter for a filled griditemview for the item that is at position 35
*     griditemview is either recycled from an old griditemview that is not visible any more or a new griditemview is created
*     each griditemview has a corresponding viewHolder with imageID and a bitmap-gui element
*     the image in the griditemview is initally loaded with a placeholder-image that will be visible until the imagload is complete.
*     the adapter initiates loading the image in the background (async-task) that gets the viewHolder with imageID.
*     when loading of the image in the background is finished the viewholder-gui element gets the loaded image.
* 
 * Created by k3b on 02.06.2015.
 */
public class GalleryCursorAdapter extends CursorAdapter  {
    private static final int MAX_IMAGE_DIMENSION = HugeImageLoader.getMaxTextureSize();

    // Identifies a particular Loader or a LoaderManager being used in this component
    protected final Activity mContext;
    protected final SelectedItems mSelectedItems;

    // for debugging
    private static int id = 1;
    protected final String mDebugPrefix;

    // for debugging: counts how many cell elements were created
    protected StringBuffer mStatus = null;

    public GalleryCursorAdapter(final Activity context, SelectedItems selectedItems, String name) {
        super(context, null, false); // no cursor yet; no auto-requery
        mContext = context;
        mSelectedItems = selectedItems;

        mDebugPrefix = "GalleryCursorAdapter#" + (id++) + "@" + name + " ";
        Global.debugMemory(mDebugPrefix, "ctor");

        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + "()");
        }
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
        if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, mDebugPrefix + "newView " + holder);
        return iView;
    }

    /** load data from cursor-position into gridview cell */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final GridCellViewHolder holder = (GridCellViewHolder) view.getTag();

        long count = DBUtils.getLong(cursor, FotoSql.SQL_COL_COUNT, 0);
        boolean gps = !DBUtils.isNull(cursor,FotoSql.SQL_COL_GPS,true);

        // new col id for with since ver 0.6.3
        long imageSize = DBUtils.getLong(cursor, FotoSql.SQL_COL_WIDTH, 0);
        if (imageSize == 0) {
            // backward compatibility old col id for with before ver 0.6.3
            imageSize = DBUtils.getLong(cursor, FotoSql.SQL_COL_SIZE, 0);
        }

        holder.filter = DBUtils.getString(cursor, FotoSql.SQL_COL_WHERE_PARAM, null);

        String description = DBUtils.getString(cursor, FotoSql.SQL_COL_DISPLAY_TEXT, "");
        String uri = DBUtils.getString(cursor, FotoSql.SQL_COL_PATH, null);
        long imageID = DBUtils.getLong(cursor, FotoSql.SQL_COL_PK, 0);
        if (count > 1) description += " (" + count + ")";
        if (gps) description += "#";
        holder.description.setText(description);
        holder.icon.setVisibility(((mSelectedItems != null) && (mSelectedItems.contains(imageID))) ? View.VISIBLE : View.GONE);
        holder.imageID = imageID;

        if (uri != null) {
            if ((imageSize > 0) && (imageSize <= Global.imageDetailThumbnailIfBiggerThan)) {
                try {
                    // #53, #83 Optimisation: no need for thumbnail - saves cache memory but may throw OutOfMemoryError
                    holder.image.setImageBitmap(HugeImageLoader.loadImage(new File(uri), MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION));
                } catch (OutOfMemoryError err) {
                    ThumbNailUtils.getThumb(uri, holder.image);
                }

            } else {
                ThumbNailUtils.getThumb(uri, holder.image);
            }
            if (Global.debugEnabledViewItem)
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + "bindView for " + holder);
        } else {
            Log.w(Global.LOG_CONTEXT, mDebugPrefix + "bindView for " + holder + ": no uri found in col " + FotoSql.SQL_COL_PATH);
        }
    }

    @Override
    public String toString() {
        return mDebugPrefix;
    }

    /** converts imageID to uri */
    public Uri getUri(long imageID) {
        return FotoSql.getUri(imageID);
    }

    /** data belonging to gridview element */
    static class GridCellViewHolder {
        /** used to create a debug-instance name. +=1 for every new instance */
        private static int lastInstanceNo = 0;
        private final String debugPrefix;

        final public ImageView image;
        final public ImageView icon;
        final public TextView description;

        /** onClick add this as sql-where-filter */
        public String filter;

        /** for delay loading */
        public long imageID = 0;

        /** for delay loading */
        public String url = null;

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
    }

    public SelectedFiles createSelectedFiles(Context context, SelectedItems items) {
        return AffUtils.querySelectedFiles(context, items);
    }

    public String getFullFilePath(int position) {
        Cursor cursor = getCursorAt(position);
        return DBUtils.getString(cursor,FotoSql.SQL_COL_DISPLAY_TEXT, null);
    }

    /** internal helper. return null if position is not available */
    private Cursor getCursorAt(int position) {
        return (Cursor) getItem(position);
    }

    /** translates offset in adapter to id of image */
    public long getImageId(int position) {
        Cursor cursor = getCursorAt(position);
        return DBUtils.getLong(cursor, FotoSql.SQL_COL_PK, 0);
    }

}
