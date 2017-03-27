/*
 * Copyright (c) 2015-2017 by k3b.
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

package de.k3b.android.androFotoFinder.locationmap;

import android.app.Activity;
import android.util.Log;

import org.osmdroid.views.overlay.OverlayManager;

import java.util.HashMap;
import java.util.Stack;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.osmdroid.ClickableIconOverlay;
import de.k3b.database.QueryParameter;

/**
 * Created by k3b on 24.08.2015.
 */
public abstract class MarkerLoaderTaskWithRecycling<MARKER extends ClickableIconOverlay> extends MarkerLoaderTask<MARKER> {
    /** The factory LocationMapFragment.FotoMarkerLoaderTask#createMarker() tries to recycle old
     *     unused Fotomarkers before creating new */
    private final Stack<MARKER> mRecycler;

    protected final int mRecyclerSizeBefore;
    protected int mRecyclerSizeAfter;


    public MarkerLoaderTaskWithRecycling(Activity context, String debugPrefix, Stack<MARKER> recycler, HashMap<Integer, MARKER> oldItems, int markerCountLimit) {
        super(context, debugPrefix, oldItems, markerCountLimit);
        mRecycler = recycler;
        mRecyclerSizeBefore = mRecycler.size();
    }

    abstract protected MARKER createNewMarker();

    @Override
    protected MARKER createMarker() {
        if (Global.debugEnabledViewItem) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + "createMarker() " + mRecycler.size());
        }

        if (mRecycler.isEmpty()) {
            return createNewMarker();
        }

        MARKER marker = mRecycler.pop();
        if (Global.debugEnabledViewItem) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + "recycled viewitem");
        }
        return marker;
    }


    @Override
    protected OverlayManager doInBackground(QueryParameter... queryParameter) {
        OverlayManager result = super.doInBackground(queryParameter);
        mRecyclerSizeAfter = mRecycler.size();
        return result;
    }

    protected void recyleItems(boolean zoomLevelChanged, HashMap<Integer, MARKER> unusedItems) {
        if (!zoomLevelChanged) {
            if (Global.debugEnabledViewItem) {
                Log.d(Global.LOG_CONTEXT, mDebugPrefix + "recyleItems() : " + unusedItems.size());
            }

            // unused old items go into recycler
            for (Integer id : unusedItems.keySet()) {
                MARKER marker = unusedItems.get(id);
                marker.set(0, null, null, null);
                mRecycler.add(marker);
            }
        }
    }
}
