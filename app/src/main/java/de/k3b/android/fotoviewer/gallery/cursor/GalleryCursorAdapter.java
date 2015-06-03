package de.k3b.android.fotoviewer.gallery.cursor;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import de.k3b.android.fotoviewer.R;

/**
 * CursorAdapter that queries MediaStore.Images.Media.EXTERNAL_CONTENT_URI
 * for a GridViewItem (gridCell)
 *
 * Created by k3b on 02.06.2015.
 */
public class GalleryCursorAdapter extends CursorAdapter {
    // for Logging-messages. Set to null to disable debug
    public static final String DEBUG_TAG = "GalleryCAdapter";

    // columns that must be avaulable in the Cursor
    public static final String SQL_COL_PK = MediaStore.Images.Media._ID;
    public static final String SQL_COL_DESCRIPTION = MediaStore.Images.Media.DATA;

    // Identifies a particular Loader or a LoaderManager being used in this component
    private static final int MY_LOADER_ID = 0;
    public static final Uri SQL_TABLE_EXTERNAL_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    public static final String[] SQL_PROJECTION = new String[]{SQL_COL_PK, SQL_COL_DESCRIPTION};
    public static final String SQL_WHERE_STATEMENT = null;      // No selection clause
    public static final String[] SQL_WHERE_PARAMETERS = null;   // No selection arguments
    public static final String SQL_SORT_ORDER = null;           // Default sort order

    // for debugging: counts how many cell elements were created
    private int itemCreateCount = 0;

    public GalleryCursorAdapter(final Activity context) {
        super(context, null, false); // no cursor yet; no auto-requery

        requery(context, SQL_PROJECTION, SQL_WHERE_STATEMENT, SQL_SORT_ORDER, SQL_WHERE_PARAMETERS);
    }

    /**
     * Initiates a database requery in the background
     */
    public void requery(final Activity context, final String[] sqlProjection, final String sqlWhereStatement, final String sqlSortOrder, final String... sqlWhereParameters) {
        if (null != DEBUG_TAG) Log.i(DEBUG_TAG, "requery");

        /*
         * Initializes the CursorLoader. The MY_LOADER_ID value is eventually passed
         * to onCreateLoader().
         */
        context.getLoaderManager().initLoader(MY_LOADER_ID, null, new LoaderManager.LoaderCallbacks<Cursor>() {

            @Override
            public Loader<Cursor> onCreateLoader(int loaderID, Bundle args) {
                switch (loaderID) {
                    case MY_LOADER_ID:
                        // Returns a new CursorLoader
                        return new CursorLoader(
                                context,   // Parent activity context
                                SQL_TABLE_EXTERNAL_CONTENT_URI, // Table to query
                                sqlProjection,             // Projection to return
                                sqlWhereStatement,        // No selection clause
                                sqlWhereParameters,       // No selection arguments
                                sqlSortOrder              // Default sort order
                        );
                    default:
                        // An invalid id was passed in
                        return null;
                }
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
                GalleryCursorAdapter.this.changeCursor(cursor);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                GalleryCursorAdapter.this.changeCursor(null);
            }
        });
    }

    /** create new empty gridview cell */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View iView = View.inflate(context, R.layout.gallery_grid_item, null);
        iView.setTag(new GridCellViewHolder(iView));

        // iView.setLayoutParams(new GridView.LayoutParams(200, 200));
        itemCreateCount++;
        if (null != DEBUG_TAG) Log.i(DEBUG_TAG, "newView #" + itemCreateCount);
        return iView;
    }

    /** load data from cursor-position into gridview cell */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final GridCellViewHolder holder = (GridCellViewHolder) view.getTag();
        holder.imageID = cursor.getLong(cursor.getColumnIndex(SQL_COL_PK));
        holder.description.setText(cursor.getString(cursor.getColumnIndex(SQL_COL_DESCRIPTION)));

        GridCellImageLoadHandler imgHandler = new GridCellImageLoadHandler(context, holder);
        imgHandler.sendEmptyMessage(0);

        if (null != DEBUG_TAG) Log.i(DEBUG_TAG, "bindView for #" + holder.imageID);
    }

    /** data belonging to gridview element */
    static class GridCellViewHolder {
        final public ImageView image;
        final public TextView description;

        GridCellViewHolder(View parent) {
            this.description = (TextView) parent.findViewById(R.id.text);
            this.image = (ImageView) parent.findViewById(R.id.image);
        };

        public long imageID;
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
            if (null != DEBUG_TAG) Log.i(DEBUG_TAG, "handleMessage getThumbnail for #" + id);
            Bitmap image = MediaStore.Images.Thumbnails.getThumbnail(
                    mContext.getContentResolver(),
                    id,
                    MediaStore.Images.Thumbnails.MICRO_KIND,
                    new BitmapFactory.Options());

            holder.image.setImageBitmap(image);
        }
    }
}
