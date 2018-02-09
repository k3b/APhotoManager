/*
 * Copyright (c) 2015-2017 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
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
 
package de.k3b.io;

import java.util.List;

/**
 * Definition of pssible filter parameters
 * Created by k3b on 13.07.2015.
 */
public interface IGalleryFilter extends IGeoRectangle {
    int SORT_BY_NONE_OLD = 0;
    int SORT_BY_NONE = ' ';
    String SORT_DIRECTION_ASCENDING = "^";
    String SORT_DIRECTION_DESCENDING = "V";

    /******************** properties **************************/
    String getPath();

    long getDateMin();

    long getDateMax();

    /** true: only photos whith no geo info (lat==lon==null) */
    boolean isNonGeoOnly();

    /** number defining current sorting */
    int getSortID();

    /** false: sort descending */
    boolean isSortAscending();

    boolean isWithNoTags();

    int getRatingMin();

    /** All Tags/Keywords/Categories/VirtualAlbum that the image must contain. ("AND") */
    List<String> getTagsAllIncluded();

    /** None of the Tags/Keywords/Categories/VirtualAlbum that the image must NOT contain. ("AND NOT") */
    List<String> getTagsAllExcluded();

    /** match if the text is in path, filename, title, description, tags */
    String getInAnyField();

    /** one of the VISIBILITY_XXXX values for public/private images */
    VISIBILITY getVisibility();

    /** load content of other IGalleryFilter implementation into this */
    IGalleryFilter get(IGalleryFilter src);
}
