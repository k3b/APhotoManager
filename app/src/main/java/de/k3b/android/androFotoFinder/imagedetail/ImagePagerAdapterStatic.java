/*
 * Copyright (c) 2019 by k3b.
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

import android.content.Context;
import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.io.InputStream;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.io.FileUtils;
import de.k3b.media.PhotoPropertiesBulkUpdateService;

/**
 * Adapter for android.support.v4.view.ViewPager for exactly one fixed image uri
 */
public class ImagePagerAdapterStatic extends PagerAdapter {
    private final Uri imageUri;

    public ImagePagerAdapterStatic(Uri imageUri) {

        this.imageUri = imageUri;
    }

    @Override
    public int getCount() {
        return 1;
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
        final Context context = container.getContext();
        PhotoViewEx photoView =  new PhotoViewEx(context);

        photoView.setMaximumScale(20);
        photoView.setMediumScale(5);

        InputStream inputStream = null;
        try {
            photoView.setImageURI(this.imageUri);
            inputStream = context.getContentResolver().openInputStream(this.imageUri);
            final int rotationInDegrees = PhotoPropertiesBulkUpdateService.getRotationFromExifOrientation(null, inputStream);

            photoView.setRotationTo(rotationInDegrees);
        } catch (Exception e) {
                Log.e(Global.LOG_CONTEXT, this.imageUri + " : cannot show", e);
        } finally {
            FileUtils.close(inputStream, this.imageUri);
        }
        container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        return photoView;
    }

}
