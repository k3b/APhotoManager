/*
 * Copyright (c) 2018-2019 by k3b.
 *
 * This file is part of #toGoZip (https://github.com/k3b/ToGoZip/).
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
package de.k3b.zip;

import java.io.Serializable;
import java.util.Date;

public class ZipConfigDto implements IZipConfig, Serializable {
    private Date   dateModifiedFrom     ;
    private String zipRelPath           ;
    private String zipName              ;
    private String zipDir               ;
    private String filter               ;

    public ZipConfigDto(IZipConfig source) {
        loadFrom(source);
    }

    public void loadFrom(IZipConfig source) {
        copy(this,source);
    }

    public static void copy(IZipConfig dest, IZipConfig source) {
        if ((null != dest) && (null != source)) {
            dest.setDateModifiedFrom     (source.getDateModifiedFrom());
            dest.setZipRelPath           (source.getZipRelPath      ());
            dest.setZipName              (source.getZipName         ());
            dest.setZipDir               (source.getZipDir          ());
            dest.setFilter               (source.getFilter          ());
        }
    }

    @Override
    public Date getDateModifiedFrom() {
        return dateModifiedFrom;
    }

    @Override
    public void setDateModifiedFrom(Date dateModifiedFrom) {
        this.dateModifiedFrom = dateModifiedFrom;
    }

    @Override
    public String getZipRelPath() {
        return zipRelPath;
    }

    @Override
    public void setZipRelPath(String zipRelPath) {
        this.zipRelPath = zipRelPath;
    }

    @Override
    public String getZipName() {
        return zipName;
    }

    @Override
    public void setZipName(String zipName) {
        this.zipName = zipName;
    }

    @Override
    public String getZipDir() {
        return zipDir;
    }

    @Override
    public void setZipDir(String zipDir) {
        this.zipDir = zipDir;
    }

    @Override
    public String getFilter() {
        return filter;
    }

    @Override
    public void setFilter(String filter) {
        this.filter = filter;
    }
}
