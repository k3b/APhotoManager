package de.k3b.android.fotoviewer.imageviewer;

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
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.IOException;

import de.k3b.android.fotoviewer.Global;
import de.k3b.android.fotoviewer.R;
import de.k3b.android.fotoviewer.queries.FotoSql;
import de.k3b.android.fotoviewer.queries.QueryParameterParcelable;
import de.k3b.android.fotoviewer.queries.Queryable;
import uk.co.senab.photoview.PhotoView;

/**
 * Created by k3b on 04.07.2015.
 */
public class ImagePagerAdapterFromCursor extends PagerAdapter  implements Queryable {
    // Identifies a particular Loader or a LoaderManager being used in this component
    private static int MY_LOADER_ID = 0;
    private static final boolean SYNC = false;

    private static int id = 0;
    private final String debugPrefix;
    private QueryParameterParcelable parameters;
    private Cursor mCursor = null;
    private boolean mDataValid = true;

    public ImagePagerAdapterFromCursor(final Activity context, QueryParameterParcelable parameters, String name) {
        debugPrefix = "ImagePagerAdapterFromCursor#" + (id++) + "@" + name + " ";
        Global.debugMemory(debugPrefix, "ctor");

        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + "()");
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
                    ImagePagerAdapterFromCursor.this.onLoadFinished(cursor);
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader) {
                    ImagePagerAdapterFromCursor.this.onLoadFinished(null);
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

        changeCursor(cursor);
        notifyDataSetChanged();

    }

    /**
     * Change the underlying cursor to a new cursor. If there is an existing cursor it will be
     * closed.
     *
     * @param cursor The new cursor to be used
     */
    private void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    /**
     * Swap in a new Cursor, returning the old Cursor.  Unlike
     * {@link #changeCursor(Cursor)}, the returned old Cursor is <em>not</em>
     * closed.
     *
     * @param newCursor The new cursor to be used.
     * @return Returns the previously set Cursor, or null if there wasa not one.
     * If the given new Cursor is the same instance is the previously set
     * Cursor, null is also returned.
     */
    private Cursor swapCursor(Cursor newCursor) {
        if (newCursor == mCursor) {
            return null;
        }
        Cursor oldCursor = mCursor;
        mCursor = newCursor;
        return oldCursor;
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


    @Override
    public int getCount() {
        return (mDataValid && (mCursor != null)) ? mCursor.getCount() : 0;
    }

    @Override
    public View instantiateItem(ViewGroup container, int position) {
        if (mDataValid && (mCursor != null)) {
            mCursor.moveToPosition(position);

            long imageID = mCursor.getLong(mCursor.getColumnIndex(FotoSql.SQL_COL_PK));

            Uri uri = getUri(imageID);

            if (Global.debugEnabled) Log.i(Global.LOG_CONTEXT, debugPrefix + "instantiateItem(#" + position +") => " + uri);

            PhotoView photoView = new PhotoView(container.getContext());

            setImage(position, imageID, uri, photoView);

            // Now just add PhotoView to ViewPager and return it
            container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            return photoView;

            /*
            LinearLayout llImage = (LinearLayout) getLayoutInflater().inflate(R.layout.view_pager_item, null);

            SubsamplingScaleImageView draweeView = (SubsamplingScaleImageView) llImage.getChildAt(0);
            draweeView.setImage(ImageSource.uri(images.get(position)));

            container.addView(llImage, 0);

            return llImage;
        */
        }
        return null;
    }

    private boolean mHasLowMemory = false;
    private void setImage(int position, long imageID, Uri uri, PhotoView photoView) {
        if (!mHasLowMemory) {
            try {
                photoView.setImageURI(uri);
                return;
            } catch (OutOfMemoryError e) {
                String message = photoView.getContext().getString(R.string.err_low_memory);
                Log.e(Global.LOG_CONTEXT, debugPrefix + "instantiateItem(#" + position + ") => " + message);
                        Toast.makeText(photoView.getContext(), message, Toast.LENGTH_LONG).show();
                mHasLowMemory = true;
                // show only once
            }
        }

        // mHasLowMemory
        Bitmap thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                photoView.getContext().getContentResolver(),
                imageID,
                MediaStore.Images.Thumbnails.MINI_KIND, // FULL_SCREEN_KIND,
                new BitmapFactory.Options());
        photoView.setImageBitmap(thumbnail);
    }

    /** converts imageID to content-uri */
    private Uri getUri(long imageID) {
        return Uri.parse(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString() + "/" + imageID);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (Global.debugEnabled) Log.i(Global.LOG_CONTEXT, debugPrefix + "destroyItem(#" + position +")");
        container.removeView((View) object);
        // super.destroyItem(container, position, object); // else exception method was not overwritten
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

}
