/*
 * Copyright (c) 2017 by k3b.
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
 * Read data from 1st child and if empty from 2nd.
 * Used to implemt setting "prefer xmp over exif" strategy
 *
 * Created by k3b on 22.04.2017.
 */
public class MetaApiChainReader extends MetaApiWrapper {
    private final IMetaApi readChild2;

    public MetaApiChainReader(IMetaApi readChild1, IMetaApi readChild2) {
        super(readChild1, null);
        this.readChild2 = readChild2;
    }

    /**
     * Normalized absolute path to file (jpg or xmp)
     */
    @Override
    public String getPath() {
        String result = super.getPath();
        if ((readChild2 != null) && ((result == null) || (result.length() == 0))) result = readChild2.getPath();
        return result;
    }

    /**
     * When the photo was taken (not file create/modify date) in local time or utc
     */
    @Override
    public Date getDateTimeTaken() {
        Date result = super.getDateTimeTaken();
        if ((readChild2 != null) && (result == null)) result = readChild2.getDateTimeTaken();
        return result;
    }

    @Override
    public Double getLatitude() {
        Double result = super.getLatitude();
        if ((readChild2 != null) && (result == null)) result = readChild2.getLatitude();
        return result;
    }

    @Override
    public Double getLongitude() {
        Double result = super.getLongitude();
        if ((readChild2 != null) && (result == null)) result = readChild2.getLongitude();
        return result;
    }

    /**
     * Title = Short Descrioption used as caption
     */
    @Override
    public String getTitle() {
        String result = super.getTitle();
        if ((readChild2 != null) && ((result == null) || (result.length() == 0))) result = readChild2.getTitle();
        return result;
    }

    /**
     * Longer description = comment. may have more than one line
     */
    @Override
    public String getDescription() {
        String result = super.getDescription();
        if ((readChild2 != null) && ((result == null) || (result.length() == 0))) result = readChild2.getDescription();
        return result;
    }

    /**
     * Tags/Keywords/Categories/VirtualAlbum used to find images
     */
    @Override
    public List<String> getTags() {
        List<String> result = super.getTags();
        if ((readChild2 != null) && ((result == null) || (result.size() == 0))) result = readChild2.getTags();
        return result;
    }

    /**
     * 5=best .. 1=worst or 0/super.get unknown
     */
    @Override
    public Integer getRating() {
        Integer result = super.getRating();
        if ((readChild2 != null) && (result == null)) result = readChild2.getRating();
        return result;
    }

    @Override
    public VISIBILITY getVisibility() {
        VISIBILITY result = super.getVisibility();
        if ((readChild2 != null) && (result == null)) result = readChild2.getVisibility();
        return result;
    }
}
