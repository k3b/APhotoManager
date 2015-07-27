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
 
package de.k3b.android.androFotoFinder.imagedetail;

import android.app.Activity;
import android.app.LoaderManager;
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

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.QueryParameterParcelable;
import de.k3b.android.androFotoFinder.queries.Queryable;
import de.k3b.android.util.GarbageCollector;
import uk.co.senab.photoview.PhotoView;

/**
 * Adapter for android.support.v4.view.ViewPager that allows swiping next/previous image.<br>
 *
 * Translates between position in ViewPager and content page content with image
 * Created by k3b on 04.07.2015.
 */
public class ImagePagerAdapterFromCursor extends PagerAdapter  implements Queryable {

    // Identifies a particular Loader or a LoaderManager being used in this component
    private static int MY_LOADER_ID = 0;

    // debug support
    private static int id = 0;
    private final String debugPrefix;
    private static final boolean SYNC = false; // true: sync loading is much easier to debug.

    private final Activity mActivity;
    // workaround because setEllipsize(TextUtils.TruncateAt.MIDDLE) is not possible for title
    private final int mMaxTitleLength;

    private QueryParameterParcelable parameters; // defining sql to get data
    private Cursor mCursor = null; // the content of the page
    private boolean mDataValid = true;

    public ImagePagerAdapterFromCursor(final Activity context, QueryParameterParcelable parameters, String name) {
        mActivity = context;
        debugPrefix = "ImagePagerAdapterFromCursor#" + (id++) + "@" + name + " ";
        Global.debugMemory(debugPrefix, "ctor");
        mMaxTitleLength = context.getResources().getInteger(R.integer.title_length_in_chars);

        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + "()");
        }

        if (parameters != null) {
            requery(context, parameters);
        }
    }

    /**
     * Interface Queryable:
     * Initiates a database requery in a background thread. onLoadFinished() is called when done.
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
     * Initiates a database requery in a background thread. onLoadFinished() is called when done.
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

    /** is called in gui thread when backgroud-threat loading has finished */
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

    /** debug support for logging current cursor content */
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
     * Implementation for PagerAdapter:
     * Return the number of views available.
     */
    @Override
    public int getCount() {
        return (this.mDataValid && (this.mCursor != null)) ? this.mCursor.getCount() : 0;
    }

    /**
     * Implementation for PagerAdapter:
     * This method may be called by the ViewPager to obtain a title string
     * to describe the specified page. This method may return null
     * indicating no title for this page. The default implementation returns
     * null.
     *
     * @param position The position of the title requested
     * @return A title for the requested page
     */
    public CharSequence getPageTitle(int position) {
        Cursor cursor = getCursorAt(position);
        if (cursor != null) {
            String name = cursor.getString(cursor.getColumnIndex(FotoSql.SQL_COL_DISPLAY_TEXT));
            if (name != null) {
                StringBuilder result = new StringBuilder();

                if (Global.debugEnabled) {
                    long imageID = cursor.getLong(cursor.getColumnIndex(FotoSql.SQL_COL_PK));
                    result.append("#").append(imageID).append(":");
                }

                // workaround because setEllipsize(TextUtils.TruncateAt.MIDDLE) is not possible for title
                if (name.length() > mMaxTitleLength) {
                    result.append("..").append(name.substring(name.length() - (mMaxTitleLength - 2)));
                } else {
                    result.append(name);
                }

                return result.toString();
            }
        }
        return mActivity.getString(R.string.loading_image_at_position, position);
    }

    public String getFullFilePath(int position) {
        Cursor cursor = getCursorAt(position);
        if (cursor != null) {
            return cursor.getString(cursor.getColumnIndex(FotoSql.SQL_COL_DISPLAY_TEXT));
        }
        return null;
    }

    /**
     * Implementation for PagerAdapter:
     * Create the page for the given position.  The adapter is responsible
     * for adding the view to the container given here, although it only
     * must ensure this is done by the time it returns from
     * {@link #finishUpdate(ViewGroup)}.
     *
     * @param container The containing View in which the page will be shown.
     * @param position The page position to be instantiated.
     * @return Returns an Object representing the new page.  This does not
     * need to be a View, but can be some other container of the page.
     */
    @Override
    public View instantiateItem(ViewGroup container, int position) {
        Cursor cursor = getCursorAt(position);
        if (cursor != null) {
            long imageID = cursor.getLong(cursor.getColumnIndex(FotoSql.SQL_COL_PK));

            Uri uri = getUri(imageID);

            PhotoView photoView = new PhotoView(container.getContext());

            if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, debugPrefix + "instantiateItem(#" + position +") => " + uri + " => " + photoView);

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

    /** internal helper. return null if position is not available */
    private Cursor getCursorAt(int position) {
        if (this.mDataValid && (this.mCursor != null)) {
            this.mCursor.moveToPosition(position);
            return this.mCursor;
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

    /**
     * Implementation for PagerAdapter:
     * Remove a page for the given position.  The adapter is responsible
     * for removing the view from its container, although it only must ensure
     * this is done by the time it returns from {@link #finishUpdate(ViewGroup)}.
     *
     * @param container The containing View from which the page will be removed.
     * @param position The page position to be removed.
     * @param object The same object that was returned by
     * {@link #instantiateItem(View, int)}.
     */
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, debugPrefix + "destroyItem(#" + position +") " + object);
        container.removeView((View) object);
        GarbageCollector.freeMemory((View) object); // to reduce memory leaks
    }

    /**
     * Implementation for PagerAdapter:
     * Determines whether a page View is associated with a specific key object
     * as returned by {@link #instantiateItem(ViewGroup, int)}. This method is
     * required for a PagerAdapter to function properly.
     *
     * @param view Page View to check for association with <code>object</code>
     * @param object Object to check for association with <code>view</code>
     * @return true if <code>view</code> is associated with the key object <code>object</code>
     */
    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    /**
     * Implementation for PagerAdapter:
     * Called to inform the adapter of which item is currently considered to
     * be the "primary", that is the one show to the user as the current page.
     *
     * @param container The containing View from which the page will be removed.
     * @param position The page position that is now the primary.
     * @param object The same object that was returned by
     * {@link #instantiateItem(View, int)}.
     */
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
        this.mActivity.setTitle(this.getPageTitle(position));
    }

}
