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

package de.k3b.geo.io;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.geo.api.GeoPointDto;

/**
 * Remembers last picked IDs, lat, long
 *
 * Created by k3b on 19.11.2015.
 */
public class GeoPickHistory extends GeoFileRepository<GeoPointDto> {
    private final int mMaxSize;

    public GeoPickHistory(File file, int maxSize) {
        super(file, new GeoPointDto());
        mMaxSize = maxSize;
    }

    /** remove all items where id or lat+lon are the same.
     * Returns id of deleted item if exist else null */
    public String remove(Long id, double latitude, double longitude) {
        String resultKey = null;
        List<GeoPointDto> data = this.load();
        if (data != null) {
            final String key = (id == null) ? null : id.toString();
            for (int i = data.size() -1; i >= 0; i-- ) {
                GeoPointDto item = data.get(i);
                if ((item == null)
                    || ((latitude == item.getLatitude()) && (longitude == item.getLongitude()))
                    || ((key != null) && key.compareTo(item.getId()) == 0)) {
                    data.remove(i);

                    if ((item != null) && (item.getId() != null)) {
                        resultKey = item.getId();
                    }
                }
            }
        }
        return resultKey;
    }

    /** remember a pick history */
    public GeoPickHistory add(Long id, double latitude, double longitude) {
        List<GeoPointDto> data = this.load();

        if (data == null) data = new ArrayList<GeoPointDto>();

        // inherit id from deleted item if not overwritten.
        String idAsString = remove(id, latitude, longitude);

        if (id != null) idAsString = id.toString();

        data.add(new GeoPointDto().setId(idAsString).setLatitude(latitude).setLongitude(longitude));
        while(data.size() > mMaxSize) data.remove(0);
        return this;
    }

    /** add keys to SelectedItems or SelectedKeys */
    public void addKeysTo(Collection dest) {
        List<GeoPointDto> data = this.load();

        if (data != null) {
            for (int i = data.size() -1; i >= 0; i-- ) {
                GeoPointDto item = data.get(i);
                String id = (item == null) ? null : item.getId();
                if ((id != null) && (id.length() > 0)) {
                    try {
                        Long idLong = Long.parseLong(id); // getLong returns null
                        if ((idLong != null) && (!dest.contains(idLong))) {
                            dest.add(idLong);
                        }
                    } catch (Exception ex) {
                        Log.w(Global.LOG_CONTEXT, "GeoPickHistory.addKeysTo('" + id +
                                "'): removing invalid imageid " + ex.getMessage());
                        data.remove(i);
                    }
                }
            }
        }
    }
}
