/*
 * Copyright (c) 2015 by k3b.
 *
 * This file is part of AndroFotoFinder.
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
 
package de.k3b.android.osmdroid;

import java.util.List;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.DefaultOverlayManager;
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
        mOverlayManager = new DefaultOverlayManager(null);
    }

    @SuppressLint("WrongCall")
    @Override
    protected void draw(Canvas canvas, MapView osm, boolean shadow) {
        if (shadow)
            return;
        mOverlayManager.onDraw(canvas, osm);
    }

    /**
     * @return the list of components of this folder.
     * Doesn't provide a copy, but the actual list.
     */
    public List<Overlay> getItems(){
        return mOverlayManager ;
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
