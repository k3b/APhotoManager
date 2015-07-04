package de.k3b.android.fotoviewer.imageviewer;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import de.k3b.android.fotoviewer.R;
import uk.co.senab.photoview.PhotoView;

/**
 * Created by k3b on 04.07.2015.
 */
public class ImagePagerAdapterFromCursor extends PagerAdapter {

    private static final int[] sDrawables = {R.drawable.image_1, R.drawable.image_2, R.drawable.image_3,
            R.drawable.image_4, R.drawable.image_5, R.drawable.image_6};

    public ImagePagerAdapterFromCursor() {}

    @Override
    public int getCount() {
        return sDrawables.length;
    }

    @Override
    public View instantiateItem(ViewGroup container, int position) {
        PhotoView photoView = new PhotoView(container.getContext());
        photoView.setImageResource(sDrawables[position]);

        // Now just add PhotoView to ViewPager and return it
        container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        return photoView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

}
