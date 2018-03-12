/*
 * Copyright (c) 2018 by k3b.
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.io.collections.SelectedItems;

/**
 * App specific helper to query and (De)Serialize between Intent|Bundle and SelectedFiles|SelectedItems.
 *
 * Created by k3b on 22.02.2018.
 */

public class AffUtils {
    /** For SelecedItems or SelectedFotos format: id,id,id,..... */
    public static final String EXTRA_SELECTED_ITEM_IDS = "de.k3b.extra.SELECTED_ITEMS";
    /** For SelectedFotos format: path,path,path,..... */
    public static final String EXTRA_SELECTED_ITEM_PATHS = "de.k3b.extra.SELECTED_ITEMS_PATH";
    /** For SelectedFotos format: date.ticks,date.ticks,date.ticks,..... */
    public static final String EXTRA_SELECTED_ITEM_DATES = "de.k3b.extra.SELECTED_ITEMS_DATE";

    public static SelectedFiles getSelectedFiles(Intent data) {
        if (data != null) {
            String selectedIDs = data.getStringExtra(EXTRA_SELECTED_ITEM_IDS);
            String selectedFiles = data.getStringExtra(EXTRA_SELECTED_ITEM_PATHS);
            String selectedDates = data.getStringExtra(EXTRA_SELECTED_ITEM_DATES);

            if ((selectedIDs != null) && (selectedFiles != null)) {
                return SelectedFiles.create(selectedFiles, selectedIDs, selectedDates);
            }
        }
        return null;
    }
    public static SelectedFiles getSelectedFiles(Bundle data) {
        if (data != null) {
            String selectedIDs = (String) data.getSerializable(EXTRA_SELECTED_ITEM_IDS);
            String selectedFiles = (String) data.getSerializable(EXTRA_SELECTED_ITEM_PATHS);
            String selectedDates = (String) data.getSerializable(EXTRA_SELECTED_ITEM_DATES);

            if ((selectedIDs != null) && (selectedFiles != null)) {
                return SelectedFiles.create(selectedFiles, selectedIDs, selectedDates);
            }
        }
        return null;
    }

    public static boolean putSelectedFiles(Intent destination, SelectedFiles selectedFiles) {
        if ((destination != null) && (selectedFiles != null) && (selectedFiles.size() > 0)) {
            destination.putExtra(EXTRA_SELECTED_ITEM_IDS, selectedFiles.toIdString());
            destination.putExtra(EXTRA_SELECTED_ITEM_PATHS, selectedFiles.toString());
            final String dateString = selectedFiles.toDateString();
            if (dateString != null) destination.putExtra(EXTRA_SELECTED_ITEM_DATES, dateString);
            return true;
        }
        return false;
    }

    public static boolean putSelectedFiles(Bundle destination, SelectedFiles selectedFiles) {
        if ((destination != null) && (selectedFiles != null) && (selectedFiles.size() > 0)) {
            destination.putSerializable(EXTRA_SELECTED_ITEM_IDS, selectedFiles.toIdString());
            destination.putSerializable(EXTRA_SELECTED_ITEM_PATHS, selectedFiles.toString());
            final String dateString = selectedFiles.toDateString();
            if (dateString != null) destination.putSerializable(EXTRA_SELECTED_ITEM_DATES, dateString);
            return true;
        }
        return false;
    }

    /** converts internal ID-list to string array of filenNames via media database. */
    public static SelectedFiles querySelectedFiles(Context context, SelectedItems items) {
        if ((items != null) && (items.size() > 0)) {
            List<Long> ids = new ArrayList<Long>();
            List<String> paths = new ArrayList<String>();
            List<Date> datesPhotoTaken = new ArrayList<Date>();

            if (FotoSql.getFileNames(context, items, ids, paths, datesPhotoTaken) != null) {
                return new SelectedFiles(paths.toArray(new String[paths.size()]), ids.toArray(new Long[ids.size()]), datesPhotoTaken.toArray(new Date[datesPhotoTaken.size()]));
            }
        }
        return null;
    }

    public static SelectedItems getSelectedItems(Intent intent) {
        String selectedIDsString = intent.getStringExtra(EXTRA_SELECTED_ITEM_IDS);
        return (selectedIDsString != null) ? new SelectedItems().parse(selectedIDsString) : null;
    }
}
