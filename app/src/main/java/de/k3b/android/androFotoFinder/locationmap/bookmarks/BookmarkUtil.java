/*
 * Copyright (c) 2015 by k3b.
 *
 * This file is part of LocationMapViewer.
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

package de.k3b.android.androFotoFinder.locationmap.bookmarks;

import de.k3b.geo.api.IGeoPointInfo;

/**
 * utils to handle bookmark items.
 *
 * Created by k3b on 27.03.2015.
 */
public class BookmarkUtil {
    private static int id =1;

    /** clones item (i.e. currentPositon) and mark it as new */
    public static GeoBmpDto createBookmark(GeoBmpDto template) {
        GeoBmpDto result = (GeoBmpDto) template.clone();
        result
                .setBitmap(template.getBitmap())
                .setName("")                    // to be set in rename dialog
                .setDescription(null)           // not a template
                .setId(null);                   // new item, not inserted yet
        return result;
    }

    public static boolean isBookmark(GeoBmpDto item) {
        return ((item != null) && (item.getDescription() == null));
    }

    public static boolean isNew(GeoBmpDto item) {
        return ((item != null) && (item.getId() == null));
    }

    public static boolean isValid(final IGeoPointInfo geoPointInfo) {
        return (geoPointInfo != null) && (geoPointInfo instanceof GeoBmpDto) && (isNotEmpty(geoPointInfo.getName()));
    }

    /**
     * sets data for NewItemPlaceholder
     */
    public static GeoBmpDto markAsTemplate(final GeoBmpDto template) {
        if (template != null) {
            final String newId = "#" + (id++);
            if (!isNotEmpty(template.getName())) {
                template.setName(newId);
            }
            if (!isNotEmpty(template.getId())) {
                template.setId(newId);
            }
            template.setDescription(template.getName());
        }
        return template;
    }

    public static boolean isNotEmpty(String name) {
        return (name != null) && (name.length() > 0);
    }
}
