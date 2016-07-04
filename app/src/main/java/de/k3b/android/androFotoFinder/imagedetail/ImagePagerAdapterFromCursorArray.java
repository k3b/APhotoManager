package de.k3b.android.androFotoFinder.imagedetail;

import android.app.Activity;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import de.k3b.android.androFotoFinder.AdapterArrayHelper;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.util.MediaScanner;
import de.k3b.database.SelectedItems;

/**
 * Purpose: allow viewing images from ".nomedia" folders where no data is available in mediadb/cursor.
 * Same as ImagePagerAdapterFromCursor but while underlaying cursor has
 * no data photos are taken from array instead.
 *
 * Created by k3b on 12.04.2016.
 */
public class ImagePagerAdapterFromCursorArray extends ImagePagerAdapterFromCursor {

    /** not null data comes from array instead from base implementation */
    private AdapterArrayHelper mArrayImpl = null;

    public ImagePagerAdapterFromCursorArray(final Activity context, String name, String fullPhotoPath) {
        super(context, name);

        if (MediaScanner.isNoMedia(fullPhotoPath,MediaScanner.DEFAULT_SCAN_DEPTH)) {
            mArrayImpl = new AdapterArrayHelper(context, fullPhotoPath, "debugContext");
        }
    }

    /** get informed that cursordata may be available so array can be disabled */
    @Override
    public Cursor swapCursor(Cursor newCursor) {
        Cursor oldCursor = super.swapCursor(newCursor);
        if (super.getCount() > 0) {
            // cursor has data => disable aray
            this.mArrayImpl = null;
        }
        return oldCursor;
    }

    @Override
    public int getCount() {
        if (mArrayImpl != null) {
            return mArrayImpl.getCount();
        }

        return super.getCount();
    }

    @Override
    public String getFullFilePath(int position) {
        if (mArrayImpl != null) return mArrayImpl.getFullFilePathfromArray(position);
        return super.getFullFilePath(position);
    }

    /** translates offset in adapter to id of image */
    @Override
    public long getImageId(int position) {
        if (mArrayImpl != null) return mArrayImpl.getImageId(position);
        return super.getImageId(position);
    }

    @Override
    public View instantiateItem(ViewGroup container, int position) {
        final String fullPhotoPath = (mArrayImpl != null) ? mArrayImpl.getFullFilePathfromArray(position) : null;
        if (fullPhotoPath != null) {
            // special case image from ".nomedia" folder via absolute path not via content: uri

            return createViewWithContent(position, container, fullPhotoPath, "instantiateItemFromArray(#", 32767);
        }

        // no array avaliable. Use original cursor baed implementation
        return  super.instantiateItem(container,position);
    }
    /** internal helper. return -1 if position is not available */
    @Override
    public int getPositionFromPath(String path) {
        if (mArrayImpl != null) {
            int result = mArrayImpl.getPositionFromPath(path);

            if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, mDebugPrefix + "getPositionFromPath-Array(" + path +") => " + result);
            return result;
        }
        return super.getPositionFromPath(path);
    }


    /** SelectedItems.Id2FileNameConverter: converts items.id-s to string array of filenNames via media database. */
    @Override
    public String[] getFileNames(SelectedItems items) {
        if (mArrayImpl != null) return mArrayImpl.getFileNames(items);
        return super.getFileNames(items);
    }

    public void refreshLocal() {
        if (mArrayImpl != null) mArrayImpl.reload(" after move delete rename ");
    }

    public boolean isInArrayMode() {
        return (mArrayImpl != null);
    }
}
