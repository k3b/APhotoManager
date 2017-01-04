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

/**
 * (Default) Implementation of {@link IMetaApi} to forward all methods to an inner {@link IMetaApi}.
 *
 * Created by k3b on 09.10.2016.
 */

public class MetaApiWrapper implements IMetaApi {
    private final IMetaApi child;

    public MetaApiWrapper(IMetaApi child) {

        this.child = child;
    }
    @Override
    public Date getDateTimeTaken() {
        return child.getDateTimeTaken();
    }

    @Override
    public MetaApiWrapper setDateTimeTaken(Date value) {
        child.setDateTimeTaken(value);
        return this;
    }

    @Override
    public MetaApiWrapper setLatitude(Double latitude) {
        child.setLatitude(latitude);
        return this;
    }

    @Override
    public MetaApiWrapper setLongitude(Double longitude) {
        child.setLongitude(longitude);
        return this;
    }

    @Override
    public Double getLatitude() {
        return child.getLatitude();
    }

    @Override
    public Double getLongitude() {
        return child.getLongitude();
    }

    public String getTitle() {
        return child.getTitle();
    }

    public MetaApiWrapper setTitle(String title) {
        child.setTitle(title);
        return this;
    }

    public String getDescription() {
        return child.getDescription();
    }

    public MetaApiWrapper setDescription(String description) {
        child.setDescription(description);
        return this;
    }

    public List<String> getTags() {
        return child.getTags();
    }

    public MetaApiWrapper setTags(List<String> tags) {
        child.setTags(tags);
        return this;
    }

    /**
     * 5=best .. 1=worst or 0/null unknown
     */
    @Override
    public Integer getRating() {
        return child.getRating();
    }

    @Override
    public IMetaApi setRating(Integer value) {
        child.setRating(value);
        return this;
    }

    public String getPath() {
        return child.getPath();
    }

    public MetaApiWrapper setPath(String filePath) {
        child.setPath(filePath);
        return this;
    }
}
