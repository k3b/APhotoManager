package de.k3b.android.fotoviewer.gallery.array;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.ArrayList;

import de.k3b.android.fotoviewer.ImageItem;
import de.k3b.android.fotoviewer.R;

/**
 * Created by EVE on 02.06.2015.
 */
public class StaticDemoData {
    /**
     * Prepare some dummy data for gridview
     */
    public static ArrayList<ImageItem> getData(Context context) {
        final ArrayList<ImageItem> imageItems = new ArrayList<>();
        TypedArray imgs = context.getResources().obtainTypedArray(R.array.image_ids);
        for (int i = 0; i < imgs.length(); i++) {
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), imgs.getResourceId(i, -1));
            imageItems.add(new ImageItem(bitmap, "Image#" + i));
        }
        return imageItems;
    }

}
