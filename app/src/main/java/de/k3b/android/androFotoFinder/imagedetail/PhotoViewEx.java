package de.k3b.android.androFotoFinder.imagedetail;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.media.JpgMetaWorkflow;
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
    private PhotoViewAttacherEx mAttacher;

    /** if != null must rotate to this */
    private float mustRotationToInDegrees = 0f;
    private String name = PhotoViewEx.class.getSimpleName();

    public PhotoViewEx(Context context) {
        this(context, null);
    }

    public PhotoViewEx(Context context, AttributeSet attr) {
        this(context, attr, 0);
    }

    public PhotoViewEx(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
    }

    /** Required to have my own enhanced attacher that contains the additional functionality */
    protected IPhotoViewAttacher onCreatePhotoViewAttacher(PhotoView photoView) {
        mAttacher = new PhotoViewAttacherEx(photoView);
        return mAttacher;
    }

    /** k3b 20150913 #10: Faster initial loading: initially the view is loaded with low res image.
     * on first zoom it is reloaded with this uri */
    public void setImageReloadFile(File file) {
        mAttacher.setImageReloadFile(file);
        if (file != null) {
            setDebugInfo(file.getName());
        }
    }

    public PhotoViewEx setDebugInfo(String name) {
        this.name = getClass().getSimpleName() + "#" + name;
        mAttacher.setDebugInfo(name);
        return this;
    }

    @Override
    public ScaleType getScaleType() {
        if (mAttacher != null) return super.getScaleType();
        return ScaleType.CENTER;
    }

    @Override
    public void setRotationTo(float rotationDegree) {
        rotationDegree = 180;
        if (rotationDegree != 0f) {
            // this.mustRotationToInDegrees = rotationDegree;
            if (true || PhotoViewAttacher.DEBUG) { // Global.debugEnabledViewItem
                Log.i(Global.LOG_CONTEXT,
                        name + "-setRotationTo: defered setRotationTo " + rotationDegree);
            }
        }
        super.setRotationTo(rotationDegree);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if ((this.mustRotationToInDegrees != 0f) && (this.getWidth() != 0)) {
            float rotateTo = this.mustRotationToInDegrees;
            this.mustRotationToInDegrees = 0;
            LogManager.getLogger().d(
                    PhotoViewAttacher.LOG_TAG,
                    name + "-defered setRotationTo: " + rotateTo);

            super.setRotationTo(rotateTo);
        }
    }

    static class PhotoViewAttacherEx extends PhotoViewAttacher {
        /** k3b 20150913 #10: Faster initial loading: initially the view is loaded with low res image.
         * on first zoom it is reloaded with this uri */
        private File mImageReloadFile = null;

        /** my android 4.4 cannot process images bigger than 4096*4096. -1 means must be calculated from openGL  */
        private static int MAX_IMAGE_DIMENSION = -1; // will be set to 4096

        public PhotoViewAttacherEx(PhotoView photoView) {
            super(photoView);
        }
        /** k3b 20150913 #10: Faster initial loading: initially the view is loaded with low res image.
         * on first zoom it is reloaded with this uri */
        public void setImageReloadFile(File imageReloadURI) {
            this.mImageReloadFile = imageReloadURI;
        }

        @Override
        public void setPhotoViewRotation(float degrees) {
            super.setPhotoViewRotation(degrees);
            dbg("setPhotoViewRotation", degrees);
        }

        @Override
        public void setRotationTo(float degrees) {
            super.setRotationTo(degrees);
            dbg("setRotationTo", degrees);
        }
        @Override
        public void setRotationBy(float degrees) {
            super.setRotationBy(degrees);
            dbg("setRotationBy", degrees);
        }

        private String name = PhotoViewEx.class.getSimpleName();
        public void setDebugInfo(String name) {
            this.name = getClass().getSimpleName() + "#" + name;
        }

        private void dbg(String ctx, float degrees) {
            if (true || PhotoViewAttacher.DEBUG) { // Global.debugEnabledViewItem
                Log.i(Global.LOG_CONTEXT,
                        name + "-" +
                                ctx +
                                " " + degrees);
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
                        imageView.setImageBitmap(HugeImageLoader.loadImage(mImageReloadFile.getAbsoluteFile(), MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION));
                        this.setRotationTo(JpgMetaWorkflow.getRotationFromExifOrientation(mImageReloadFile.getAbsolutePath()));

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
