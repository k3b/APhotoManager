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

import java.util.Date;

/** Parameters that define a backup*/
public interface IZipConfig {
    Date getDateModifiedFrom();
    String getZipRelPath    ();
    String getZipName       ();
    String getZipDir        ();
    String getFilter        ();

    void setDateModifiedFrom(Date value);
    void setZipRelPath(String value);
    void setZipName(String value);
    void setZipDir(String value);
    void setFilter(String value);
}
