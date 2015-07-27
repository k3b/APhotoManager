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
 
package de.k3b.io;

/**
 * parameter for foto filter: only fotos from certain filepath, date and/or lat/lon will be visible.
 * Created by k3b on 11.07.2015.
 */
public class GalleryFilterParameter extends GeoRectangle implements IGalleryFilter {
    private String path = null;

    private long dateMin = 0;
    private long dateMax = 0;

    public GalleryFilterParameter get(IGalleryFilter src) {
        super.get(src);
        this.setDateMax(src.getDateMax());
        this.setDateMin(src.getDateMin());
        this.setPath(src.getPath());
        return this;
    }

    /******************** properties **************************/
    @Override
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public long getDateMin() {
        return dateMin;
    }

    public void setDateMin(long dateMin) {
        this.dateMin = dateMin;
    }

    @Override
    public long getDateMax() {
        return dateMax;
    }

    public void setDateMax(long dateMax) {
        this.dateMax = dateMax;
    }

}
