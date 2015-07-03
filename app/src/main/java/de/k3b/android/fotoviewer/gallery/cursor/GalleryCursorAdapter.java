package de.k3b.android.fotoviewer.gallery.cursor;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.k3b.android.fotoviewer.queries.QueryParameterParcelable;
import de.k3b.android.fotoviewer.queries.FotoSql;
import de.k3b.android.fotoviewer.Global;
import de.k3b.android.fotoviewer.OnGalleryInteractionListener;
import de.k3b.android.fotoviewer.R;
import de.k3b.android.fotoviewer.queries.Queryable;
import de.k3b.database.QueryParameter;

/**
 * CursorAdapter that queries MediaStore.Images.Media.EXTERNAL_CONTENT_URI
 * for a GridViewItem (gridCell)
 *
 * Created by k3b on 02.06.2015.
 */
public class GalleryCursorAdapter extends CursorAdapter implements Queryable {
    // Identifies a particular Loader or a LoaderManager being used in this component
    private static int MY_LOADER_ID = 0;
    private static final boolean SYNC = false;
    private OnGalleryInteractionListener callback = null;

    // for debugging
    private static int id = 1;
    private final String debugPrefix;

    // for debugging: counts how many cell elements were created
    private int itemCreateCount = 0;
    private QueryParameterParcelable parameters = null;
    private final Drawable imageNotLoadedYet;

    public GalleryCursorAdapter(final Activity context, QueryParameterParcelable parameters, String name) {
        super(context, null, false); // no cursor yet; no auto-requery
        debugPrefix = "GalleryCursorAdapter#" + (id++) + "@" + name + " ";

        imageNotLoadedYet = context.getResources().getDrawable(R.drawable.image_loading);

        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + "()");
        }
        if (context instanceof OnGalleryInteractionListener) {
            this.callback = (OnGalleryInteractionListener) context;
        }

        if (parameters != null) {
            requery(context, parameters);
        }
    }

    /**
     * Interface Queryable: Initiates a database requery in the background
     */
    @Override
    public void requery(final Activity context, QueryParameterParcelable parameters) {
        this.parameters = parameters;
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + "requery " + ((parameters != null) ? parameters.toSqlString() : null));
        }

        requery(context, parameters.toColumns(), parameters.toFrom(), parameters.toAndroidWhere(), parameters.toOrderBy(), parameters.toAndroidParameters());
    }

    /**
     * Initiates a database requery in the background
     */
    private void requery(final Activity context, final String[] sqlProjection, final String from, final String sqlWhereStatement, final String sqlSortOrder, final String... sqlWhereParameters) {

        /*
         * Initializes the CursorLoader. The MY_LOADER_ID value is eventually passed
         * to onCreateLoader().
         */

        if (SYNC) {
            // for debugging
            Cursor result = context.getContentResolver().query(Uri.parse(from), // Table to query
                    sqlProjection,             // Projection to return
                    sqlWhereStatement,        // No selection clause
                    sqlWhereParameters,       // No selection arguments
                    sqlSortOrder              // Default sort order
            );
            onLoadFinished(result);
        } else {
            final int currentLoaderId = ++MY_LOADER_ID;
            context.getLoaderManager().initLoader(currentLoaderId, null, new LoaderManager.LoaderCallbacks<Cursor>() {

                @Override
                public Loader<Cursor> onCreateLoader(int loaderID, Bundle args) {
                    if (loaderID == currentLoaderId) {
                        // Returns a new CursorLoader
                        return new CursorLoader(
                                context,   // Parent activity context
                                Uri.parse(from), // Table to query
                                sqlProjection,             // Projection to return
                                sqlWhereStatement,        // No selection clause
                                sqlWhereParameters,       // No selection arguments
                                sqlSortOrder              // Default sort order
                        );
                    }
                    return null;
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
                    GalleryCursorAdapter.this.onLoadFinished(cursor);
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader) {
                    GalleryCursorAdapter.this.onLoadFinished(null);
                }
            });
        }
    }

    private void onLoadFinished(Cursor cursor) {
        int resultCount = (cursor == null) ? 0 : cursor.getCount();
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + "onLoadFinished() requery rows found: " + resultCount + " in " + cursor + " " +
                    debugCursor(cursor, 10, " + ", FotoSql.SQL_COL_DISPLAY_TEXT));
        }

        if (callback != null) {
            callback.setResultCount(resultCount);
        }
        GalleryCursorAdapter.this.changeCursor(cursor);
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
        return R.layout.gallery_grid_item;
    }

    /** create new empty gridview cell */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View iView = View.inflate(context, R.layout.gallery_grid_item, null);
        iView.setTag(new GridCellViewHolder(iView));

        // iView.setLayoutParams(new GridView.LayoutParams(200, 200));
        itemCreateCount++;
        if (Global.debugEnabled) Log.i(Global.LOG_CONTEXT, debugPrefix + "newView #" + itemCreateCount);
        return iView;
    }

    /** load data from cursor-position into gridview cell */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final GridCellViewHolder holder = (GridCellViewHolder) view.getTag();
        holder.imageID = cursor.getLong(cursor.getColumnIndex(FotoSql.SQL_COL_PK));
        long count = cursor.getLong(cursor.getColumnIndex(FotoSql.SQL_COL_COUNT));
        boolean gps = !cursor.isNull(cursor.getColumnIndex(FotoSql.SQL_COL_GPS));

        String description = cursor.getString(cursor.getColumnIndex(FotoSql.SQL_COL_DISPLAY_TEXT));

        holder.filter = FotoSql.getFilter(cursor, this.parameters, description);

        if (count > 1) description += " (" + count + ")";
        if (gps) description += "#";
        holder.description.setText(description);
        holder.icon.setVisibility((count > 1) ? View.VISIBLE : View.GONE);
        holder.image.setImageDrawable(imageNotLoadedYet);

        GridCellImageLoadHandler imgHandler = new GridCellImageLoadHandler(context, holder);
        imgHandler.sendEmptyMessage(0);

        if (Global.debugEnabled) Log.i(Global.LOG_CONTEXT, debugPrefix + "bindView for #" + holder.imageID);
    }

    @Override
    public String toString() {
        return debugPrefix + this.parameters;
    }

    /** data belonging to gridview element */
    static class GridCellViewHolder {
        final public ImageView image;
        final public ImageView icon;
        final public TextView description;

        /** onClick add this as sql-where-filter */
        public String filter;

        /** for delay loading */
        public long imageID;

        GridCellViewHolder(View parent) {
            this.description = (TextView) parent.findViewById(R.id.text);
            this.image = (ImageView) parent.findViewById(R.id.image);
            this.icon = (ImageView) parent.findViewById(R.id.icon);
        };
    }

    /** Handler to load image in Background */
    static class GridCellImageLoadHandler extends Handler {
        private GridCellViewHolder holder;
        private Context mContext;

        public GridCellImageLoadHandler(Context c, GridCellViewHolder v) {
            holder = v;
            mContext = c;
        }

        @Override
        public void handleMessage(Message msg) {
            Long id = holder.imageID;
            if (Global.debugEnabled) Log.i(Global.LOG_CONTEXT, "GridCellImageLoadHandler.handleMessage getThumbnail for #" + id);
            Bitmap image = getBitmap(id);
            holder.image.setImageBitmap(image);
        }

        public Bitmap getBitmap(Long id) {
            final Bitmap thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                    mContext.getContentResolver(),
                    id,
                    MediaStore.Images.Thumbnails.MICRO_KIND,
                    new BitmapFactory.Options());

            return thumbnail;
        }
    }
}
