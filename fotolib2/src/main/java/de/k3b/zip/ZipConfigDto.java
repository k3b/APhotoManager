/*
 * Copyright (c) 2018-2020 by k3b.
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
import java.text.SimpleDateFormat;
import java.util.Date;

import de.k3b.io.FileNameUtil;

public class ZipConfigDto implements IZipConfig, Serializable {
    private static final String ZIP_FILE_NAME_SUFFIX = ".yyyyMMdd-HHmmss";

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

    public static String fixZipBaseName(String zipName) {
        return FileNameUtil.createFileNameWitoutExtension(zipName);
    }

    @Override
    public void setZipName(String zipName) {
        this.zipName = fixZipBaseName(zipName);
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

    /**
     * get zip file name with basename, datetime suffix, fileextension but witout directory path..
     * Example if zipName='myBackup' with zipDir='/path/to/dir' the created
     * zipfile will be "/path/to/dir/myBackup-20191224-123456.zip' when the backup
     * is taken at 2019-12-24 at 12:34:56
     */
    public static String getZipFileName(IZipConfig config, Date modificationDateTime) {
        StringBuilder zipFileName = new StringBuilder()
                .append(config.getZipName())
                .append(getDateString(config.getDateModifiedFrom()))
                .append(getDateString(modificationDateTime))
                .append(".zip");
        return zipFileName.toString();

    }

    private static String getDateString(Date modificationDateTime) {
        if (modificationDateTime == null) return ".-";
        return new SimpleDateFormat(ZIP_FILE_NAME_SUFFIX).format(modificationDateTime);
    }
}
