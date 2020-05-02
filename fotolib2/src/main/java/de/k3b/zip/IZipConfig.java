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

import java.util.Date;

/** Parameters that define a backup*/
public interface IZipConfig {
    /**
     * for incremental backup: all files changes since this date. Null means full backup.
     */
    Date getDateModifiedFrom();

    /** if not empty: create subfolders in zip relative to this path.
     * Example zipRelPath='/DCIM/'
     * means that '/DCIM/subfolder/file.jpg'
     *      becomes 'subfolder/file.jpg' in zip file
     */
    String getZipRelPath();

    /** basename of output zip-file without path and witout fileextension.
     * Example if zipName='myBackup' with zipDir='/path/to/dir' the created
     * zipfile will be "/path/to/dir/myBackup-20191224-123456.zip' when the backup
     * is taken at 2019-12-24 at 12:34:56*/
    String getZipName();

    /** uri to output directory (witout filename) where to store the output zip-file. */
    String getZipDir();

    /** string representation of filter to be applied to find photos to back up to zip. */
    String getFilter        ();

    void setDateModifiedFrom(Date value);
    void setZipRelPath(String value);
    void setZipName(String value);
    void setZipDir(String value);
    void setFilter(String value);
}
