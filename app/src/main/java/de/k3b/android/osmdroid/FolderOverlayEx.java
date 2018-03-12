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

import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.OverlayManager;

public class FolderOverlayEx extends FolderOverlay {

    /** replaces the current overlaymanager. @returns the previous */
    public OverlayManager setOverlayManager(OverlayManager newFolderOverlayContent) {
        OverlayManager old = mOverlayManager;
        mOverlayManager = newFolderOverlayContent;
        return old;
    }
}
