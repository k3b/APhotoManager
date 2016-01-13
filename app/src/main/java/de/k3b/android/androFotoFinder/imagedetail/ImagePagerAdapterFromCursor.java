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
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.util.GarbageCollector;
import de.k3b.database.QueryParameter;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import uk.co.senab.photoview.PhotoView;

/**
 * Adapter for android.support.v4.view.ViewPager that allows swiping next/previous image.<br>
 *
 * Translates between position in ViewPager and content page content with image
 * Created by k3b on 04.07.2015.
 */
public class ImagePagerAdapterFromCursor extends PagerAdapter  {
    // debug support
    private static int id = 0;
    private final String mDebugPrefix;
    private static final boolean SYNC = false; // true: sync loading is much easier to debug.

    private final Activity mActivity;
    // workaround because setEllipsize(TextUtils.TruncateAt.MIDDLE) is not possible for title
    private final int mMaxTitleLength;

    private QueryParameter mParameters; // defining sql to get data
    private Cursor mCursor = null; // the content of the page
    private boolean mDataValid = true;

    public ImagePagerAdapterFromCursor(final Activity context, String name) {
        mActivity = context;
        mDebugPrefix = "ImagePagerAdapterFromCursor#" + (id++) + "@" + name + " ";
        Global.debugMemory(mDebugPrefix, "ctor");
        mMaxTitleLength = context.getResources().getInteger(R.integer.title_length_in_chars);

        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + "()");
        }
    }

    /**
     * Swap in a new Cursor, returning the old Cursor.  Unlike the returned old Cursor is <em>not</em>
     * closed.
     *
     * @param newCursor The new cursor to be used.
     * @return Returns the previously set Cursor, or null if there wasa not one.
     * If the given new Cursor is the same instance is the previously set
     * Cursor, null is also returned.
     */
    public Cursor swapCursor(Cursor newCursor) {
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
        return mActivity.getString(R.string.image_loading_at_position_format, position);
    }

    public String getFullFilePath(int position) {
        Cursor cursor = getCursorAt(position);
        if (cursor != null) {
            return cursor.getString(cursor.getColumnIndex(FotoSql.SQL_COL_DISPLAY_TEXT));
        }
        return null;
    }

    public long getImageId(int position) {
        Cursor cursor = getCursorAt(position);
        if (cursor != null) {
            return cursor.getLong(cursor.getColumnIndex(FotoSql.SQL_COL_PK));
        }
        return 0;
    }

    public IGeoPointInfo getGeoPoint(int position) {
        Cursor cursor = getCursorAt(position);
        if (cursor != null) {
            int colLat = cursor.getColumnIndex(FotoSql.SQL_COL_LAT);
            int colLon = cursor.getColumnIndex(FotoSql.SQL_COL_LON);

            return new GeoPointDto(cursor.getDouble(colLat), cursor.getDouble(colLon), IGeoPointInfo.NO_ZOOM);
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

            if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, mDebugPrefix + "instantiateItem(#" + position +") => " + uri + " => " + photoView);

            setImage(position, imageID, uri, photoView);

            // Now just add PhotoView to ViewPager and return it
            container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            return photoView;
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

    /** internal helper. return -1 if position is not available */
    public int getCursorFromPath(String path) {
        if (this.mDataValid && (this.mCursor != null) && (path != null)) {
            int index = mCursor.getColumnIndex(FotoSql.SQL_COL_DISPLAY_TEXT);
            if (index >= 0) {
                if (mCursor.moveToFirst()) {
                    do {
                        if (path.equals(mCursor.getString(index))) {
                            return mCursor.getPosition();
                        }
                    } while (mCursor.moveToNext());
                }
            }
        }
        return -1;
    }

    private void setImage(int position, long imageID, Uri uri, PhotoView photoView) {
        /** k3b 20150913 #10: Faster initial loading: initially the view is loaded with low res image. on first zoom it is reloaded with this uri */
        photoView.setImageReloadFile(new File(getFullFilePath(position)));

        // #26 option slow-hiqh-quality-detail vs fast-lowRes
        // #26 android 5.1: does not support Thumbnails.getThumbnail(...,MediaStore.Images.Thumbnails.FULL_SCREEN_KIND,...) :-(
        // int resolutionKind = Global.initialImageDetailResolutionHigh ? MediaStore.Images.Thumbnails.FULL_SCREEN_KIND : MediaStore.Images.Thumbnails.MINI_KIND;
        int resolutionKind = MediaStore.Images.Thumbnails.MINI_KIND;

        Bitmap thumbnail = null;
        final BitmapFactory.Options options = new BitmapFactory.Options();
        final ContentResolver contentResolver = photoView.getContext().getContentResolver();
        try {
            thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                    contentResolver,
                    imageID,
                    resolutionKind,
                    options);
        } catch (IllegalArgumentException ex) {
            // #26 android 5.1: does not support Thumbnails.getThumbnail(...,MediaStore.Images.Thumbnails.FULL_SCREEN_KIND,...) :-(
            Log.w(Global.LOG_CONTEXT, mDebugPrefix +" getThumbnail(FULL_SCREEN) not supported - resetting to getThumbnail(MINI).");

            Global.initialImageDetailResolutionHigh = false;
            resolutionKind = MediaStore.Images.Thumbnails.MINI_KIND;

            thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                    contentResolver,
                    imageID,
                    resolutionKind,
                    options);
        }
        photoView.setImageBitmap(thumbnail);
        photoView.setMaximumScale(20);
        photoView.setMediumScale(5);
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
        if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, mDebugPrefix + "destroyItem(#" + position +") " + object);
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
