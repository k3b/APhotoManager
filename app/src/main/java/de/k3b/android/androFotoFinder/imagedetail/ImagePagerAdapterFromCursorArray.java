package de.k3b.android.androFotoFinder.imagedetail;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import uk.co.senab.photoview.PhotoView;

/**
 * Purpose: allow viewing images from ".nomedia" folders where no data is available in mediadb/cursor.
 * Same as ImagePagerAdapterFromCursor but while underlaying cursor has
 * no data photos are taken from array instead.
 *
 * Created by k3b on 12.04.2016.
 */
public class ImagePagerAdapterFromCursorArray extends ImagePagerAdapterFromCursor {
    /** not null data comes from array instead from base implementation */
    private String[] mFullPhotoPaths = null;

    public ImagePagerAdapterFromCursorArray(final Activity context, String name, String... fullPhotoPaths) {
        super(context, name);
        mFullPhotoPaths = fullPhotoPaths;
        if ((mFullPhotoPaths != null) && (mFullPhotoPaths.length == 0)) {
            mFullPhotoPaths = null;
        }
    }

    /** get informed that cursordata may be available so array can be disabled */
    @Override
    public Cursor swapCursor(Cursor newCursor) {
        Cursor oldCursor = super.swapCursor(newCursor);
        if (super.getCount() > 0) {
            // cursor has data
            this.mFullPhotoPaths = null;
        }
        return oldCursor;
    }

    @Override
    public int getCount() {
        if (mFullPhotoPaths != null) {
            return mFullPhotoPaths.length;
        }

        return super.getCount();
    }

    @Override
    public String getFullFilePath(int position) {
        if ((mFullPhotoPaths != null) && (position >= 0) && (position < mFullPhotoPaths.length)) {
            return mFullPhotoPaths[position];
        }
        return super.getFullFilePath(position);
    }

    @Override
    public View instantiateItem(ViewGroup container, int position) {
        if ((mFullPhotoPaths != null) && (position >= 0) && (position < mFullPhotoPaths.length)) {
            // special case image from ".nomedia" folder via absolute path not via content: uri
            PhotoView photoView = new PhotoView(container.getContext());
            photoView.setImageURI(Uri.parse(mFullPhotoPaths[position]));

            container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            return photoView;

        }
        return  super.instantiateItem(container,position);
    }

}
