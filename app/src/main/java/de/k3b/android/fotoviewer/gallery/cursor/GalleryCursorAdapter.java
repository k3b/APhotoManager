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

import de.k3b.android.database.QueryParameterParcelable;
import de.k3b.android.fotoviewer.R;

/**
 * CursorAdapter that queries MediaStore.Images.Media.EXTERNAL_CONTENT_URI
 * for a GridViewItem (gridCell)
 *
 * Created by k3b on 02.06.2015.
 */
public class GalleryCursorAdapter extends CursorAdapter implements de.k3b.android.fotoviewer.Queryable {
    // for Logging-messages. Set to null to disable debug
    public static final String DEBUG_TAG = "GalleryCAdapter";

    // Identifies a particular Loader or a LoaderManager being used in this component
    private static final int MY_LOADER_ID = 0;

    // for debugging: counts how many cell elements were created
    private int itemCreateCount = 0;
    private QueryParameterParcelable parameters = null;

    public GalleryCursorAdapter(final Activity context, QueryParameterParcelable parameters) {
        super(context, null, false); // no cursor yet; no auto-requery

        if (parameters != null) {
            requery(context, parameters);
        }
    }

    /**
     * Initiates a database requery in the background
     */
    @Override
    public void requery(final Activity context, QueryParameterParcelable parameters) {
        this.parameters = parameters;
        if (null != DEBUG_TAG) Log.i(DEBUG_TAG, "requery " + parameters.toSqlString());
        requery(context, parameters.toColumns(), parameters.toFrom(), parameters.toAndroidWhere(), parameters.toOrderBy(), parameters.toAndroidParameters());
    }

    /**
     * Initiates a database requery in the background
     */
    private void requery(final Activity context, final String[] sqlProjection, final String from, final String sqlWhereStatement, final String sqlSortOrder, final String... sqlWhereParameters) {
        // if (null != DEBUG_TAG) Log.i(DEBUG_TAG, "requery");

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
                                Uri.parse(from), // Table to query
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
                if (null != DEBUG_TAG) Log.i(DEBUG_TAG, "requery rows found: " + cursor.getCount());
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
        holder.imageID = cursor.getLong(cursor.getColumnIndex(FotoSql.SQL_COL_PK));
        long count = cursor.getLong(cursor.getColumnIndex(FotoSql.SQL_COL_COUNT));
        boolean gps = !cursor.isNull(cursor.getColumnIndex(FotoSql.SQL_COL_GPS));

        String description = cursor.getString(cursor.getColumnIndex(FotoSql.SQL_COL_DESCRIPTION));

        holder.filter = FotoSql.getFilter(cursor, this.parameters, description);

        if (count > 1) description += " (" + count + ")";
        if (gps) description += "#";
        holder.description.setText(description);

        GridCellImageLoadHandler imgHandler = new GridCellImageLoadHandler(context, holder);
        imgHandler.sendEmptyMessage(0);

        if (null != DEBUG_TAG) Log.i(DEBUG_TAG, "bindView for #" + holder.imageID);
    }

    /** data belonging to gridview element */
    static class GridCellViewHolder {
        final public ImageView image;
        final public TextView description;
        public String filter;

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
