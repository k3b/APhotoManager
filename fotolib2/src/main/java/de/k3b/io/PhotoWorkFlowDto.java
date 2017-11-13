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

package de.k3b.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

import de.k3b.media.IMetaApi;
import de.k3b.media.MediaAsString;

/**
 * Persistable data for autoproccessing images (auto-rename, auto-add-exif)
 *
 * Created by k3b on 04.08.2017.
 */

public class PhotoWorkFlowDto {
    /** added to every serialized item if != null. Example "Generated on 2015-10-19 with myApp Version 0815." */
    public static String sFileComment = "";

    private static final String KEY_DATE_FORMAT     = "DateFormat";
    private static final String KEY_NAME            = "Name";
    private static final String KEY_NUMBER_FORMAT   = "NumberFormat";
    private static final String KEY_EXIF            = "Exif";
    private static final String KEY_OUT_DIR         = "outDir";

    private final Properties properties;
    private File outDir;

    public PhotoWorkFlowDto() {
        this(null, new Properties());
    }

    protected PhotoWorkFlowDto(File outDir, Properties properties) {
        this.outDir = outDir;
        this.properties = properties;
    }

    public PhotoWorkFlowDto load(File outDir) throws IOException {
        this.outDir = outDir;
        File apm = getApmFile();
        properties.clear();
        if (apm.exists() && apm.isFile() && apm.canRead()) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(apm);
                properties.load(inputStream);
                return this;
            } finally {
                FileUtils.close(inputStream,"PhotoWorkFlowDto.load(" + apm + ")");
            }
        }
        return null;
    }

    private File getApmFile() {
        return new File(this.outDir, RuleFileNameProcessor.APM_FILE_NAME);
    }

    public void save() throws IOException {
        File apm = getApmFile();
        FileOutputStream inputStream = null;
        try {
            inputStream = new FileOutputStream(apm);
            properties.store(inputStream, PhotoWorkFlowDto.sFileComment);
        } finally {
            FileUtils.close(inputStream,"PhotoWorkFlowDto.load(" + apm + ")");
        }
    }

    /** Android support: to persist state and to transfer activites via intent */
    public static PhotoWorkFlowDto load(Serializable content) {
        Properties properties = (Properties) content;
        if (properties != null) {
            String outDir = properties.getProperty(KEY_OUT_DIR);
            return new PhotoWorkFlowDto((outDir != null) ? new File(outDir) : null, properties);
        }
        return null;
    }

    /** Android support: to persist state and to transfer activites via intent */
    public Serializable toSerializable() {
        if (this.properties != null) {
            this.properties.put(KEY_OUT_DIR, (this.outDir == null) ? null : this.outDir.toString());
        }
        return this.properties;
    }

    /** DateFormat part for {@link RuleFileNameProcessor} */
    public String getDateFormat() {
        return properties.getProperty(KEY_DATE_FORMAT);
    }

    /** DateFormat part for {@link RuleFileNameProcessor} */
    public void setDateFormat(String dateFormat) {
        properties.setProperty(KEY_DATE_FORMAT,dateFormat);
    }

    /**  fixed-Name part for {@link RuleFileNameProcessor} */
    public String getName() {
        return properties.getProperty(KEY_NAME);
    }

    /**  fixed-Name part for {@link RuleFileNameProcessor} */
    public void setName(String Name) {
        properties.setProperty(KEY_NAME,Name);
    }

    /**  NumberFormat part for {@link RuleFileNameProcessor} */
    public String getNumberFormat() {
        return properties.getProperty(KEY_NUMBER_FORMAT);
    }

    /**  NumberFormat part for {@link RuleFileNameProcessor} */
    public void setNumberFormat(String NumberFormat) {
        properties.setProperty(KEY_NUMBER_FORMAT,NumberFormat);
    }

    public File getOutDir() {
        return outDir;
    }
    public void setOutDir(File outDir) {
        this.outDir = outDir;
    }

    public IFileNameProcessor createFileNameProcessor() {
        return new RuleFileNameProcessor(getDateFormat(), getName(), getNumberFormat(), getOutDir());
    }

    public IMetaApi getMediaDefaults() {
        String mediaDefaultString = properties.getProperty(KEY_EXIF);
        return (mediaDefaultString == null) ? null : new MediaAsString().fromString(mediaDefaultString);
    }

    public void setMediaDefaults(IMetaApi mediaDefaults) {
        String mediaDefaultString = null;
        if (mediaDefaults != null) {
            mediaDefaultString = new MediaAsString().setData(mediaDefaults).toString();
        }

        if (mediaDefaultString != null) {
            properties.setProperty(KEY_EXIF, mediaDefaultString);
        } else {
            properties.remove(KEY_EXIF);
        }
    }
}
