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
 
package de.k3b.android.androFotoFinder.queries;

import de.k3b.database.QueryParameter;

/**
 * Data to be shared between activities and fragments
 *
 * Created by k3b on 16.06.2015.
 */
public class FotoViewerParameter {
    /** getFrom orderBy for directory chooser */
    public static QueryParameter currentDirOrderByQuery = null;

    /** true: load images from folder and its subfolders. false: do not load images from subfolders */
    public static boolean includeSubItems = true;

    /** getFrom data for gallery */
    public static QueryParameter currentGalleryContentQuery = FotoSql.queryDetail;

    /** true: gallery contains inline dir-picker */
    public static boolean galleryHasEmbeddedDirPicker = false;
}
