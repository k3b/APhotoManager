/*
 * Copyright (c) 2015-2016 by k3b.
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

package de.k3b.android.androFotoFinder.locationmap.bookmarks;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

import de.k3b.android.androFotoFinder.R;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.api.IGeoRepository;

/**
 * Gui-type independendant handling to display/update a BookmarkList.
 * Should work with activity, dialog and fragment
 * Created by k3b on 13.04.2015.
 */
public class BookmarkListController {
    private static final String BOOKMARKS_FILE_NAME = "favorites.txt";

    private GeoBmpDto currentItem;

    public BookmarkListController setAdditionalPoints(GeoBmpDto[] additionalPoints) {
        this.additionalPoints = additionalPoints;
        for (GeoBmpDto template : this.additionalPoints) {
            BookmarkUtil.markAsTemplate(template);
        }
        return this;
    }

    interface OnSelChangedListener
    {
        void onSelChanged(GeoBmpDto newSelection);
    }

    private final Context context;
    private final ListView listView;
    private final IGeoRepository<GeoBmpDto> repository;
    private GeoBmpDto[] additionalPoints = null;
    private OnSelChangedListener selChangedListener = null;

    public BookmarkListController(Context context, final ListView listView) {
        this.context = context;
        this.listView = listView;
        this.repository = new GeoBmpFileRepository(this.context.getDatabasePath(BOOKMARKS_FILE_NAME));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                final GeoBmpDto currenSelection = (GeoBmpDto) listView.getItemAtPosition(position);
                setCurrentItem(currenSelection);
            }
        });
        /* does not work: onItemSelected is never called
        listView.setItemsCanFocus(true);

        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View childView,
                                       int position, long id) {
                onSelChanged((GeoBmpDto) listView.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                onSelChanged(null);
            }
        });
*/
        // this.registerForContextMenu(listView);

    }

    public void setSelChangedListener(OnSelChangedListener selChangedListener) {
        this.selChangedListener = selChangedListener;
    }

    public BookmarkListController reloadGuiFromRepository() {
        final ArrayAdapter<GeoBmpDto> adapter = GeoBmpListAdapter.createAdapter(this.context,
                R.layout.geobmp_list_view_row, repository, additionalPoints);
        this.listView.setAdapter(adapter);
        this.setCurrentItem(adapter.isEmpty() ? null : adapter.getItem(0));
        return this;
    }


    public void setCurrentItem(GeoBmpDto newSelection) {
        final GeoBmpListAdapter listAdapter = (GeoBmpListAdapter) this.listView.getAdapter();
        listAdapter.setCurrentSelecion(newSelection);

        this.currentItem = newSelection;

        if (selChangedListener != null) {
            selChangedListener.onSelChanged(newSelection);
        }
    }

    public GeoBmpDto getCurrentItem() {
        return currentItem;
    }

    public void update(IGeoPointInfo geoPointInfo) {
        if (BookmarkUtil.isValid(geoPointInfo)) {
            GeoBmpDto item = (GeoBmpDto) geoPointInfo;
            if (BookmarkUtil.isNew(item)) {
                item.setId(repository.createId());
                List<GeoBmpDto> items = this.repository.load();
                items.add(0, item);
            }
            this.repository.save();
            this.reloadGuiFromRepository();
        }
    }

    public void deleteCurrent() {
        if (repository.delete(currentItem)) {
            reloadGuiFromRepository();
        }
    }
}
