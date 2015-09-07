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

package de.k3b.android.androFotoFinder;

/**
 * Created by k3b on 01.09.2015.
 */
public interface Common {
    /**
     * Format:GalleryFilterParameter.toString/parse.
     * See https://github.com/k3b/AndroFotoFinder/wiki/intentapi#filter
     */
    static final String EXTRA_FILTER = "de.k3b.extra.FILTER";

    /**
     * Format:SelectedItems.toString/parse.
     * See https://github.com/k3b/AndroFotoFinder/wiki/intentapi#SelectedItems
     */
    static final String EXTRA_SELECTED_ITEMS = "de.k3b.extra.SELECTED_ITEMS";
}
