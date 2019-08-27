/*
 * Copyright (c) 2019 by k3b.
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
package de.k3b.android.androFotoFinder.directory;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import de.k3b.android.androFotoFinder.FotoGalleryActivity;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailActivityViewPager;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.database.QueryParameter;
import de.k3b.io.IDirectory;

/**
 * Handles aspect "showInNewXxxx for DirectoryPicker, TagPicker, GeoAreaPicker
 */
public class ShowInMenuHandler {
    private final Activity mContext;
    private final PickerContext pickerContext;
    private final QueryParameter baseQuery;
    private final int mDirTypId;

    public ShowInMenuHandler(Activity mContext, PickerContext pickerContext, QueryParameter baseQuery, int mDirTypId) {
        this.mContext = mContext;

        this.pickerContext = pickerContext;
        this.baseQuery = baseQuery;
        this.mDirTypId = mDirTypId;
    }

    public void fixMenuOpenIn(ShowInMenuHandler showInMenuHandler, IDirectory selection, Menu menu) {
        if ((baseQuery != null) &&
                (menu.findItem(R.id.cmd_gallery) != null)) {
            long baseCount = getPickCount("(with baseQuery)", showInMenuHandler, selection, baseQuery);
            if (baseCount > 0) {
                long count = getPickCount(selection.toString(), showInMenuHandler, selection, null);
                setMenuCount(menu, R.id.cmd_gallery, count);
                setMenuCount(menu, R.id.cmd_photo, count);
                if (baseCount != count) {
                    setMenuCount(menu, R.id.cmd_gallery_base, baseCount);
                    setMenuCount(menu, R.id.cmd_photo_base, baseCount);
                }
            }
        }
    }

    public boolean onPopUpClick(MenuItem menuItem, IDirectory popUpSelection) {
        switch (menuItem.getItemId()) {
            case R.id.cmd_show_in_new:
                return pickerContext.onShowPopUp(null, null, popUpSelection,
                        R.menu.menu_context_pick_show_in_new,
                        R.menu.menu_context_pick_show_in_new);

            case R.id.cmd_photo:
                return showPhoto(popUpSelection, mDirTypId, null);
            case R.id.cmd_photo_base:
                return showPhoto(popUpSelection, mDirTypId, baseQuery);
            case R.id.cmd_gallery:
                return showGallery(popUpSelection, mDirTypId, null);
            case R.id.cmd_gallery_base:
                return showGallery(popUpSelection, mDirTypId, baseQuery);
            default:
                break;
        }
        return false;
    }

    private void setMenuCount(Menu menu, int menuId, long count) {
        MenuItem menuItem = menu.findItem(menuId);
        if (menuItem != null) {
            final boolean visible = count > 0;
            menuItem.setVisible(visible);
            if (visible) {
                menuItem.setTitle(menuItem.getTitle() + "(" +
                        count + ")");
            }
        }
    }

    private long getPickCount(String dbgContext, ShowInMenuHandler showInMenuHandler, IDirectory selection, QueryParameter baseQuery) {
        QueryParameter query = pickerContext.getSelectionQuery("getPickCount" + dbgContext, selection,
                mDirTypId, baseQuery);
        if (query == null) return 0;
        return FotoSql.getCount(mContext, query);
    }

    private boolean showPhoto(IDirectory selectedDir, int dirTypId, QueryParameter baseQuery) {
        QueryParameter query = pickerContext.getSelectionQuery("showPhoto", selectedDir, dirTypId, baseQuery);
        if (query != null) {
            String dbgContext = "[6]" + FotoSql.getName(mContext, dirTypId) + ":" + selectedDir;

            FotoSql.setSort(query, FotoSql.SORT_BY_DATE, false);
            ImageDetailActivityViewPager.showActivity(dbgContext, mContext, null, 0, query, 0);
            return true;
        }
        return false;
    }

    private boolean showGallery(IDirectory selectedDir, int dirTypId, QueryParameter baseQuery) {
        String dbgContext = "[7]" + FotoSql.getName(mContext, dirTypId) + ":" + selectedDir;
        QueryParameter query = pickerContext.getSelectionQuery(dbgContext, selectedDir, dirTypId, baseQuery);
        if (query != null) {
            FotoGalleryActivity.showActivity(dbgContext, mContext, query, 0);
            return true;
        }
        return false;
    }

    public interface PickerContext {
        QueryParameter getSelectionQuery(String dbgContext, IDirectory selectedDir,
                                         int dirTypId, QueryParameter baseQuery);

        boolean onShowPopUp(View anchor, View owner, IDirectory selection, int... idContextMenue);
    }
}
