package de.k3b.android.osmdroid;

import java.util.AbstractList;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;

/**
 * copied from org.osmdroid.bonuspack.overlays;
 *
 * An overlay which is just a group of other overlays.
 *
 * @author M.Kergall
 */
public class FolderOverlay extends Overlay {

    protected OverlayManager mOverlayManager;

    public FolderOverlay(Context ctx) {
        super(ctx);
        mOverlayManager = new OverlayManager(null);
    }

    @SuppressLint("WrongCall")
    @Override protected void draw(Canvas canvas, MapView osm, boolean shadow) {
        if (shadow)
            return;
        mOverlayManager.onDraw(canvas, osm);
    }

    /**
     * @return the list of components of this folder.
     * Doesn't provide a copy, but the actual list.
     */
    public AbstractList<Overlay> getItems(){
        return mOverlayManager;
    }

    public boolean add(Overlay item){
        return mOverlayManager.add(item);
    }

    public boolean remove(Overlay item){
        return mOverlayManager.remove(item);
    }

    /** replaces the current overlaymanager. @returns the previous */
    public OverlayManager setOverlayManager(OverlayManager newItems) {
        OverlayManager old = mOverlayManager;
        mOverlayManager = newItems;
        return old;
    }

    @Override public boolean onSingleTapUp(MotionEvent e, MapView mapView){
        if (isEnabled())
            return mOverlayManager.onSingleTapUp(e, mapView);
        else
            return false;
    }

    @Override public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView){
        if (isEnabled())
            return mOverlayManager.onSingleTapConfirmed(e, mapView);
        else
            return false;
    }

    @Override public boolean onLongPress(MotionEvent e, MapView mapView){
        if (isEnabled())
            return mOverlayManager.onLongPress(e, mapView);
        else
            return false;
    }

    @Override public boolean onTouchEvent(MotionEvent e, MapView mapView){
        if (isEnabled())
            return mOverlayManager.onTouchEvent(e, mapView);
        else
            return false;
    }

    @Override
    public void onDetach(MapView mapView) {
        super.onDetach(mapView);
        mOverlayManager.onDetach(mapView);
    }

    //TODO: implement other events...

    /**
     * Close all opened InfoWindows of overlays it contains.
     * This only operates on overlays that inherit from OverlayWithIW.
     */
    public void closeAllInfoWindows(){
        for (Overlay overlay:mOverlayManager){
            if (overlay instanceof FolderOverlay){
                ((FolderOverlay)overlay).closeAllInfoWindows();
            }
        }
    }

}
