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

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import de.k3b.io.DateUtil;
import de.k3b.io.StringUtils;

public class ZipConfigRepository implements IZipConfig {
    private static final String FILE_SUFFIX = ".zip.apm.config";
    private Properties data = new Properties();
    public static boolean isZipConfig(String uri) {
        if (uri == null) return false;
        return uri.endsWith(FILE_SUFFIX);
    }

    public ZipConfigRepository load(InputStream inputsteam, Object uri) throws IOException {
        data.clear();
        data.load(inputsteam);
        return this;
    }

    @Override
    public Date getDateModifiedFrom() {
        return DateUtil.parseIsoDate(get("DateModifiedFrom"));
    }

    @Override public String getZipRelPath() {return get("ZipRelPath");}
    @Override public String getZipName   () {return get("ZipName");}
    @Override public String getZipDir    () {return get("ZipDir");}
    @Override public String getFilter    () {return get("Filter");}

    @Override
    public void setDateModifiedFrom(Date value) {
        set("DateModifiedFrom",DateUtil.toIsoDateTimeString(value));
    }

    @Override public void setZipRelPath(String value) {set("ZipRelPath",value);}
    @Override public void setZipName   (String value) {set("ZipName",value);}
    @Override public void setZipDir    (String value) {set("ZipDir",value);}
    @Override public void setFilter    (String value) {set("Filter",value);}


    private void set(String key, String value) {
        if (StringUtils.isNullOrEmpty(value))
            data.remove(key);
        else
            data.put(key, value);
    }

    private String get(String key) {
        return data.getProperty(key);
    }
}
