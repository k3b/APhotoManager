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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import de.k3b.LibGlobal;
import de.k3b.io.DateUtil;
import de.k3b.io.FileUtils;
import de.k3b.io.Properties;
import de.k3b.io.StringUtils;

/** Parameters that define a backup that can be persisted through a properties-file */
public class ZipConfigRepository implements IZipConfig {
    private static final Logger logger = LoggerFactory.getLogger(LibZipGlobal.LOG_TAG);

    public static final String FILE_SUFFIX = ".zip.apm.config";
    private Properties data = new Properties();

    /** added to every serialized item if != null. Example "Generated on 2015-10-19 with myApp Version 0815." */
    public static String sFileComment = null;

    public static boolean isZipConfig(String uri) {
        if (uri == null) return false;
        return uri.endsWith(FILE_SUFFIX);
    }

    public ZipConfigRepository(IZipConfig src) {
        ZipConfigDto.copy(this,src);
    }

    public ZipConfigRepository load(InputStream inputsteam, Object uri) throws IOException {
        data.clear();
        data.load(inputsteam);
        FileUtils.close(inputsteam, uri);
        return this;
    }

    public boolean save() {
        File configFile = getZipConfigFile();

        if (configFile != null) {
            OutputStream out = null;
            try {
                out = new FileOutputStream(configFile);
                data.store(out, ZipConfigRepository.sFileComment);
                return true;
            } catch (IOException ex) {
                // file not found or no permission
                if (LibZipGlobal.debugEnabled) {
                    logger.warn(ex.getClass().getSimpleName() + ".saveTo(" + configFile + ") failed ", ex);
                }
            } finally {
                FileUtils.close(out, configFile);
            }
        }
        return false;
    }

    public File getZipConfigFile() {
        final String zipName = this.getZipName();
        return getZipConfigFile(zipName);
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

    public static IZipConfig getZipConfigOrNull(String zipName) {
        if (zipName != null) {
            File repositoryFile = getZipConfigFile(zipName);
            if ((repositoryFile != null) && (repositoryFile.exists())) {
                try {
                    IZipConfig repo = new ZipConfigRepository(null).load(new FileInputStream(repositoryFile), repositoryFile);
                    if (repo != null) {
                        return new ZipConfigDto(repo);
                    }
                } catch (IOException ignore) {
                }
            }
        }
        return null;
    }

    public static File getZipConfigFile(String zipName) {
        if (zipName == null) return null;

        File zipFileDir = LibGlobal.zipFileDir;
        String fileName = FileUtils.replaceExtension(zipName, ZipConfigRepository.FILE_SUFFIX);
        if ((zipFileDir == null) || (fileName == null)) return null;

        File configFile = new File(zipFileDir, fileName);
        return configFile;
    }
}
