/*
 * Copyright (c) 2016-2017 by k3b.
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

package de.k3b.media;

import java.util.Date;
import java.util.List;

import de.k3b.io.VISIBILITY;

/**
 * DTO=DataTransferObject: In Memory implementation of {@link IMetaApi}.
 *
 * Created by k3b on 10.10.2016.
 */

public class MediaDTO implements IMetaApi {
    public String path;
    public Date dateTimeTaken;
    public Double latitude;
    private Double longitude;
    private String title;
    private String description;
    private List<String> tags;
    private Integer rating;
    private VISIBILITY visibility;

    public MediaDTO() {}

    public MediaDTO(IMetaApi src) {
        MediaUtil.copy(this, src, true, true);
    }

    public MediaDTO clear() {
        path  = null;
        dateTimeTaken  = null;
        latitude  = null;
        longitude  = null;
        title  = null;
        description  = null;
        tags  = null;
        rating  = null;
        return this;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public IMetaApi setPath(String filePath) {
        path = filePath;
        return this;
    }

    @Override
    public Date getDateTimeTaken() {
        return dateTimeTaken;
    }

    @Override
    public IMetaApi setDateTimeTaken(Date value) {
        dateTimeTaken = value;
        return this;
    }

    /** latitude, in degrees north. (-90 .. +90); longitude, in degrees east.  (-180 .. + 180)    */
    @Override public IMetaApi setLatitudeLongitude(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        return this;
    }

    @Override
    public Double getLatitude() {
        return latitude;
    }

    @Override
    public Double getLongitude() {
        return longitude;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public IMetaApi setTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public IMetaApi setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    @Override
    public IMetaApi setTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    /**
     * 5=best .. 1=worst or 0/null unknown
     */
    @Override
    public Integer getRating() {
        return this.rating;
    }

    @Override
    public IMetaApi setRating(Integer value) {
        this.rating = value;
        return this;
    }

    public VISIBILITY getVisibility() {
        return visibility;
    }

    public IMetaApi setVisibility(VISIBILITY visibility) {
        this.visibility = visibility;
        return this;
    }

    @Override
    public String toString() {
        return MediaUtil.toString(this);
    }
}
