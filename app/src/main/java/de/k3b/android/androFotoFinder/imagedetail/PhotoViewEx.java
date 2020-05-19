/*
 * Copyright (c) 2016-2020 by k3b.
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
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

import de.k3b.io.filefacade.IFile;
import de.k3b.media.PhotoPropertiesBulkUpdateService;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.log.LogManager;

/**
 * Enhanvced PhotoView with support for
 * - huge images
 * - fast load reduced image that will be replaced by full-size image on first zoom
 *
 * Simplified version of code used in de.k3b.android.androFotoFinder.imagedetail.ImagePagerAdapterFromCursor
 * from https://github.com/k3b/APhotoManager/
 *
 * Created by k3b on 14.07.2016.
 */
public class PhotoViewEx extends PhotoView {
    private static final String LOG_CONTEXT = PhotoViewAttacher.LOG_TAG; // ;Global.LOG_CONTEXT;
    private PhotoViewAttacherEx mAttacher;

    public PhotoViewEx(Context context) {
        this(context, null);
    }

    public PhotoViewEx(Context context, AttributeSet attr) {
        this(context, attr, 0);
    }

    public PhotoViewEx(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
    }

    /** factory method to be overwritten if you need different attacher functionality */
    @Override
    protected PhotoViewAttacher createPhotoViewAttacher() {
        mAttacher = new PhotoViewAttacherEx(this);
        return mAttacher;
    }


    /** k3b 20150913 #10: Faster initial loading: initially the view is loaded with low res image.
     * on first zoom it is reloaded with this uri */
    public void setImageReloadFile(IFile file) {
        mAttacher.setImageReloadFile(file);
        if (file != null) {
            setDebugPrefix(file.getName());
        }
    }

    @Override
    public ScaleType getScaleType() {
        if (mAttacher != null) return super.getScaleType();
        return ScaleType.CENTER;
    }

    static class PhotoViewAttacherEx extends PhotoViewAttacher {
        /** k3b 20150913 #10: Faster initial loading: initially the view is loaded with low res image.
         * on first zoom it is reloaded with this uri */
        private IFile mImageReloadFile = null;

        /** my android 4.4 cannot process images bigger than 4096*4096. -1 means must be calculated from openGL  */
        private static int MAX_IMAGE_DIMENSION = -1; // will be set to 4096

        public PhotoViewAttacherEx(PhotoView photoView) {
            super(photoView);
        }
        /** k3b 20150913 #10: Faster initial loading: initially the view is loaded with low res image.
         * on first zoom it is reloaded with this uri */
        public void setImageReloadFile(IFile imageReloadURI) {
            this.mImageReloadFile = imageReloadURI;
            if (imageReloadURI != null) {
                setDebugPrefix(imageReloadURI.getName());
            }
        }

        /** invoked by the guesture detector */
        @Override
        public void onScale(float scaleFactor, float focusX, float focusY) {
            /** k3b 20150913 #10: Faster initial loading: initially the view is loaded with low res image.
             * On first zoom it is reloaded with mImageReloadFile */
            if (mImageReloadFile != null) {
                ImageView imageView = getImageView();
                if (imageView != null) {
                    if (DEBUG) {
                        // !!!
                        LogManager.getLogger().d(
                                LOG_TAG,
                                "onScale: Reloading image from " + mImageReloadFile);
                    }
                    try {
                        if (MAX_IMAGE_DIMENSION < 0) {
                            MAX_IMAGE_DIMENSION = (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) ? 4096 : HugeImageLoader.getMaxTextureSize();
                        }
                        imageView.setImageBitmap(HugeImageLoader.loadImage(mImageReloadFile, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION));
                        this.setRotationTo(PhotoPropertiesBulkUpdateService.getRotationFromExifOrientation(mImageReloadFile, null));

                    } catch (OutOfMemoryError e) {
                        LogManager.getLogger().e(
                                LOG_TAG,
                                "onScale: Not enought memory to reloading image from " + mImageReloadFile + " failed: " + e.getMessage());
                    }

                    mImageReloadFile = null; // either success or error: do not try it again
                }
            }

            super.onScale(scaleFactor,focusX,focusY);
        }
    }
}
