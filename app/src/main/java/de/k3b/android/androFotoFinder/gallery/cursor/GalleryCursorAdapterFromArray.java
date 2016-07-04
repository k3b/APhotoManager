package de.k3b.android.androFotoFinder.gallery.cursor;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

import de.k3b.android.androFotoFinder.AdapterArrayHelper;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.ThumbNailUtils;
import de.k3b.android.util.MediaScanner;
import de.k3b.database.SelectedItems;
import uk.co.senab.photoview.HugeImageLoader;

/**
 * Created by k3b on 30.05.2016.
 * Purpose: allow viewing images from ".nomedia" folders where no data is available in mediadb/cursor.
 * Same as GalleryCursorAdapter but while underlaying cursor has
 * no data photos are taken from array instead.
 *
 */
public class GalleryCursorAdapterFromArray extends GalleryCursorAdapter {

    /** not null data comes from array instead from base implementation */
    private AdapterArrayHelper mArrayImpl = null;

    public GalleryCursorAdapterFromArray(final Activity context, SelectedItems selectedItems, String name, String fullPhotoPath) {
        super(context, selectedItems, name);

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

    /**
     * @see android.widget.ListAdapter#getView(int, View, ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final String fullPhotoPathFromArray = (mArrayImpl != null) ? mArrayImpl.getFullFilePathfromArray(position) : null;

        if (fullPhotoPathFromArray != null) {
            View v;
            if (convertView == null) {
                v = newView(mContext, null, parent);
            } else {
                v = convertView;
            }
            final GridCellViewHolder holder = (GridCellViewHolder) v.getTag();
            holder.url =  fullPhotoPathFromArray;

            final File file = new File(fullPhotoPathFromArray);
            ThumbNailUtils.getThumb(fullPhotoPathFromArray, holder.image);
            holder.image.setImageBitmap(HugeImageLoader.loadImage(file, 32,32));

            holder.image.setImageURI(Uri.parse(holder.url));
            holder.imageID = this.getImageId(position);
            holder.icon.setVisibility(((mSelectedItems != null) && (mSelectedItems.contains(holder.imageID))) ? View.VISIBLE : View.GONE);

            if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, mDebugPrefix + "bindView for " + holder);
            return v;

        } else {
            return super.getView(position,convertView, parent);
        }
    }

    /** converts imageID to uri */
    @Override
    public Uri getUri(long imageID) {
        if (mArrayImpl != null) return Uri.parse("file:"+mArrayImpl.getFullFilePathfromArray(mArrayImpl.convertBetweenPositionAndId((int) imageID)));
        return super.getUri(imageID);
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
