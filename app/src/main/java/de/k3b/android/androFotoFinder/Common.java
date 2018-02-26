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

import android.app.Activity;
import android.content.Intent;

/**
 * Created by k3b on 01.09.2015.
 */
public interface Common {
    /**
     * gallery,filter:
     * Format:GalleryFilterParameter.toString/parseMultiple.
     * See https://github.com/k3b/AndroFotoFinder/wiki/intentapi#filter
     */
    static final String EXTRA_FILTER = "de.k3b.extra.FILTER";

    /** detail,gallery:  sql where ... order by ... group by ... */
    public static final String EXTRA_QUERY = "de.k3b.extra.SQL";

    /** detail: offset in in the resultset to be shown */
    public static final String EXTRA_POSITION = "de.k3b.extra.OFFSET";

    /** gallery,geoEdit,picker: app title for picker */
    public static final String EXTRA_TITLE = Intent.EXTRA_TITLE;

    /**  detail:  getData/EXTRA_STREAM - file/content  */
    public static final String EXTRA_STREAM = Intent.EXTRA_STREAM;

    /** detail:  Activity.onActivityResult() - resultCode: no photo-files were modified */
    public static final int RESULT_NOCHANGE = Activity.RESULT_CANCELED;

    /** detail,geoEdit:  Activity.onActivityResult() - resultCode:  one or more photo-files were modified. caller must invalidate cached files/directories. */
    public static final int RESULT_CHANGE = Activity.RESULT_OK;
}
