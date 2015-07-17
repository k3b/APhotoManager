package de.k3b.android.osmdroid;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

import java.util.List;

/**
 * A marker containing IGeoPoint, unique id and data. In a geografic map it will shows an icon.
 *
 * Created by k3b on 17.07.2015.
 */
public abstract class MarkerBase<DataType> extends IconOverlay {
    protected int mId = 0;
    private DataType mData = null;

    /**
     * save to be called in non-gui-thread
     */
    protected MarkerBase(ResourceProxy pResourceProxy) {
        super(pResourceProxy);
    }

    /**
     * @return true if click was handeled.
     */
    abstract protected boolean onMarkerClicked(MapView mapView, int markerId, IGeoPoint makerPosition, DataType markerData);

    /** used to recycle this */
    public MarkerBase set(int id, IGeoPoint position, Drawable icon, DataType data) {
        set(position, icon);
        mId = id;
        mData = data;
        return this;
    }

    /**
     * From org.osmdroid.bonuspack.overlays.Marker#hitTest
     * @return true, if this marker was taped.
     */
    protected boolean hitTest(final MotionEvent event, final MapView mapView){
        final Projection pj = mapView.getProjection();
        pj.toPixels(mPosition, mPositionPixels);
        final Rect screenRect = pj.getIntrinsicScreenRect();
        int x = -mPositionPixels.x + screenRect.left + (int) event.getX();
        int y = -mPositionPixels.y + screenRect.top + (int) event.getY();
        boolean hit = mIcon.getBounds().contains(x, y);
        return hit;
    }

    /**
     * @return true: tap handeled. No following overlay/map should handle the event.
     * false: tap not handeled. A following overlay/map should handle the event.
     */
    @Override public boolean onSingleTapConfirmed(final MotionEvent event, final MapView mapView) {
        boolean touched = hitTest(event, mapView);
        if (touched) {
            return onMarkerClicked(mapView, mId, mPosition, mData);
        } else {
            return super.onSingleTapConfirmed(event, mapView);
        }
    }

    public static MarkerBase find(List<MarkerBase> list, int id) {
        for (MarkerBase item : list) {
            if ((item != null) && (item.mId == id)) return item;
        }
        return null;
    }

    public int getID() {
        return mId;
    }
}
