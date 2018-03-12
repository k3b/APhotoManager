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
 
package de.k3b.android.androFotoFinder.imagedetail;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;

import java.io.File;
import java.util.Date;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.util.DBUtils;
import de.k3b.android.util.GarbageCollector;
import de.k3b.media.JpgMetaWorkflow;
import de.k3b.android.util.MenuUtils;
import de.k3b.android.util.ResourceUtils;

/**
 * Adapter for android.support.v4.view.ViewPager that allows swiping next/previous image.<br>
 *
 * Translates between position in ViewPager and content page content with image
 * Created by k3b on 04.07.2015.
 */
public class ImagePagerAdapterFromCursor extends PagerAdapter {
    private static final int MAX_IMAGE_DIMENSION = HugeImageLoader.getMaxTextureSize();

    /** colum alias for optinal sql expression to show ContextDetails */
    public static final String CONTEXT_COLUMN_FIELD = "ContextDetails";

    // debug support
    private static int id = 0;
    protected final String mDebugPrefix;

    private final Activity mActivity;
    // workaround because setEllipsize(TextUtils.TruncateAt.MIDDLE) is not possible for title
    private final int mMaxTitleLength;

    private Cursor mCursor = null; // the content of the page

    protected DisplayImageOptions mDisplayImageOptions;
    private Menu mMenu = null;

    private ImageButtonControllerImpl mImageButtonController = null;

    public ImagePagerAdapterFromCursor(final Activity context, String name) {
        mActivity = context;
        mDebugPrefix = "ImagePagerAdapterFromCursor#" + (id++) + "@" + name + " ";
        Global.debugMemory(mDebugPrefix, "ctor");
        mMaxTitleLength = context.getResources().getInteger(R.integer.title_length_in_chars);

        mImageButtonController = new ImageButtonControllerImpl();
        mDisplayImageOptions = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.image_loading)
                .showImageForEmptyUri(R.drawable.image_loading)
                .showImageOnFail(R.drawable.image_loading)
                .cacheInMemory(false)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .displayer(new SimpleBitmapDisplayer())
                .build();
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + "()");
        }
    }

    public void setMenu(Menu menu) {
        mMenu = menu;
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

    /**
     * Implementation for PagerAdapter:
     * Return the number of views available.
     */
    @Override
    public int getCount() {
        int result = 0;
        if (this.mCursor != null) {
            result = this.mCursor.getCount();
        }

        return result;
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
            String name = DBUtils.getString(cursor,FotoSql.SQL_COL_DISPLAY_TEXT, null);
            if (name != null) {
                StringBuilder result = new StringBuilder();

                if (Global.debugEnabled) {
                    long imageID = DBUtils.getLong(cursor, FotoSql.SQL_COL_PK,0);
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
        return DBUtils.getString(cursor,FotoSql.SQL_COL_DISPLAY_TEXT, null);
    }

    /** translates offset in adapter to id of image */
    public long getImageId(int position) {
        Cursor cursor = getCursorAt(position);
        return DBUtils.getLong(cursor, FotoSql.SQL_COL_PK,0);
    }

    public Date getDatePhotoTaken(int position) {
        Cursor cursor = getCursorAt(position);
        return FotoSql.getDate(cursor, cursor.getColumnIndex(FotoSql.SQL_COL_DATE_TAKEN));
    }


    public boolean hasGeo(int position) {
        Cursor cursor = getCursorAt(position);
        return !DBUtils.isNull(cursor,FotoSql.SQL_COL_GPS,true);
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
            String fullPhotoPath = getFullFilePath(position);

            // determine max(with,height) from db
            // new col id for with since ver 0.6.3
            int colSize = cursor.getColumnIndex(FotoSql.SQL_COL_WIDTH);

            if (colSize < 0) {
                // backward compatibility old col id for with before ver 0.6.3
                colSize = cursor.getColumnIndex(FotoSql.SQL_COL_SIZE);
            }

            int size = (colSize >= 0) ? cursor.getInt(colSize) : 32767;

            if (fullPhotoPath != null) {
                return createViewWithContent(position, container, fullPhotoPath, "instantiateItemFromCursor(#", size);
            }

        }
        return null;
    }

    /** internal helper. return null if position is not available */
    private Cursor getCursorAt(int position) {
        if ((this.mCursor != null) && (position >= 0) && (position < this.mCursor.getCount())) {
            this.mCursor.moveToPosition(position);
            return this.mCursor;
        }
        return null;
    }

    /** internal helper. return -1 if position is not available */
    public int getPositionFromPath(String path) {
        int result = -1;
        if ((this.mCursor != null) && (path != null)) {
            int index = mCursor.getColumnIndex(FotoSql.SQL_COL_DISPLAY_TEXT);
            if ((index >= 0) && (mCursor.moveToFirst())) {
                do {
                    if (path.equals(mCursor.getString(index))) {
                        result = mCursor.getPosition();
                        break;
                    }
                } while (mCursor.moveToNext());
            }
        }
        if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, mDebugPrefix + "getPositionFromPath(" + path +") => " + result);
        return result;
    }

    @NonNull
    protected View createViewWithContent(int position, ViewGroup container, String fullPhotoPath, String debugContext, int size) {
        final Context context = container.getContext();

        final boolean useLayout=true;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);

        PhotoViewEx photoView;
        View root;
        TextView contextTextView = null;

        if (useLayout) {
            root = inflater.inflate(R.layout.pager_item_image, container, false);

            photoView = (PhotoViewEx) root.findViewById(R.id.image);
            mImageButtonController.create((ImageButton) root.findViewById(R.id.cmd_any));
            contextTextView = (TextView) root.findViewById(R.id.text);

        } else {
            photoView = new PhotoViewEx(context);
            root = photoView;
        }

        if (contextTextView != null) {
            String contextText = DBUtils.getString(getCursorAt(position), CONTEXT_COLUMN_FIELD, null);

            if ((contextText != null) && (contextText.length() > 0)) {
                contextTextView.setVisibility(View.VISIBLE);
                contextTextView.setText(ImageContextController.sqlFormat(contextText));
            } else {
                contextTextView.setVisibility(View.INVISIBLE);
            }
        }

        photoView.setMaximumScale(20);
        photoView.setMediumScale(5);

        final File imageFile = new File(fullPhotoPath);

        String loadType;

        // if image is big use memoryefficient, fast, low-quality thumbnail (old code)
        if (size > Global.imageDetailThumbnailIfBiggerThan) {
            loadType = "image too big using thumb ";
            setImageFromThumbnail(photoView, imageFile);
        } else {
            try {
                // #53 Optimisation: no need for thumbnail - saves cache memory but may throw OutOfMemoryError
                loadType = "image small enough ";
                photoView.setImageBitmap(HugeImageLoader.loadImage(imageFile, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION));
                photoView.setImageReloadFile(null);
            } catch (OutOfMemoryError err) {
                loadType = "small image out of memory using thumb ";
                setImageFromThumbnail(photoView, imageFile);
            }
        }
        photoView.setRotationTo(JpgMetaWorkflow.getRotationFromExifOrientation(fullPhotoPath));
        if (Global.debugEnabledViewItem) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + debugContext + position +", "
                    + loadType + ") => " + fullPhotoPath + " => " + photoView);
        }

        container.addView(root, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        return root;
    }

    private void setImageFromThumbnail(PhotoViewEx photoView, File imageFile) {
        /** k3b 20150913 #10: Faster initial loading: initially the view is loaded with low res image.
         * on first zoom it is reloaded with this uri */
        photoView.setImageReloadFile(imageFile);

        ImageLoader.getInstance().displayImage("file://" + imageFile, photoView, mDisplayImageOptions);
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
        return (view != null) && view.equals(object);
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
        if (object != null) {
            mImageButtonController.create((ImageButton) ((View) object).findViewById(R.id.cmd_any));
        }
    }

    public void setIconResourceName(String name) {
        mImageButtonController.setContext(name);
    }

    private class ImageButtonControllerImpl {
        private MenuItem mMenuItem = null ;
        private View.OnClickListener mClickListener;
        private final View.OnLongClickListener mLongClickListener;

        public ImageButtonControllerImpl() {
            mClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ((mMenuItem != null) && (mMenu != null)) {
                        mMenu.performIdentifierAction(mMenuItem.getItemId(), Menu.FLAG_ALWAYS_PERFORM_CLOSE);
                    }
                }
            };
            mLongClickListener = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if ((mActivity != null) && (mMenuItem != null)) {
                        Toast.makeText(mActivity, mMenuItem.getTitle(), Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            };
        }

        public void create(ImageButton btn) {
            if (btn != null) {
                Drawable icon = null;

                // only even orderIDs will become a button
                // to allow hiding items
                if ((mMenuItem != null) && (mMenuItem.getOrder() % 2 == 0)) {
                    icon = mMenuItem.getIcon();
                    if ((icon == null) && (mMenuItem.getItemId() == R.id.action_details)) {
                        icon = ResourceUtils.getDrawable(mActivity, android.R.drawable.ic_dialog_info);
                    }
                }

                if (icon != null) {
                    btn.setImageDrawable(icon);
                    btn.setVisibility(View.VISIBLE);
                } else {
                    btn.setVisibility(View.INVISIBLE);
                }
                btn.setOnClickListener(mClickListener);
                btn.setOnLongClickListener(mLongClickListener);
            }
        }

        public void setContext(MenuItem context) {
            this.mMenuItem = context;
        }
        public void setContext(String name) {
            setContext(MenuUtils.findByTitle(mMenu, name, 2));
        }
        public String getContext() {
            CharSequence icon = (mMenuItem == null) ? null : mMenuItem.getTitle();
            if (icon == null) return null;
            return icon.toString();
        }
        public int getMenuId() {
            return (mMenuItem == null) ? 0 : mMenuItem.getItemId();
        }
    }
    public int getMenuId() {
        return (mImageButtonController == null) ? 0 : mImageButtonController.getMenuId();
    }
    public void setContext(MenuItem context) {
        if (mImageButtonController != null) mImageButtonController.setContext(context);
    }
}
