/*
 * Copyright (c) 2017-2018 by k3b.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

import de.k3b.FotoLibGlobal;
import de.k3b.media.IMetaApi;
import de.k3b.media.MediaAsString;

/**
 * #93: Persistable data for autoproccessing images (auto-rename, auto-add-exif)
 *
 * Created by k3b on 04.08.2017.
 */

public class PhotoWorkFlowDto {
    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);

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

    public PhotoWorkFlowDto(File outDir, Properties properties) {
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
                if (FotoLibGlobal.debugEnabled) {
                    logger.debug(this.getClass().getSimpleName() + ": loaded from " + apm + ":" + this);
                }
                return this;
            } finally {
                FileUtils.close(inputStream,"PhotoWorkFlowDto.load(" + apm + ")");
            }
        }
        return null;
    }

    public void paste(PhotoWorkFlowDto newData) {
        if (newData != null) {
            this.setDateFormat(newData.getDateFormat());
            this.setNumberFormat(newData.getNumberFormat());
            this.setMediaDefaults(newData.getMediaDefaults());
            String name = getTranslateName(newData);
            this.setName(name);
        }

    }

    public String getTranslateName(PhotoWorkFlowDto newData) {
        if (newData != null) {
            final RuleFileNameProcessor srcData = (RuleFileNameProcessor) newData.createFileNameProcessor();
            if (srcData != null) {
                return RuleFileNameProcessor.translateName(srcData, this.getOutDir());
            }
        }
        return null;
    }

    private File getApmFile() {
        return getApmFile(this.outDir);
    }

    public static File getApmFile(File outDir) {
        return new File(outDir, RuleFileNameProcessor.APM_FILE_NAME);
    }

    /** if has no data the file is deleted */
    public void save() throws IOException {
        File apm = getApmFile();
        FileOutputStream stream = null;
        if (isEmpty()) {
            if (FotoLibGlobal.debugEnabled) {
                logger.debug(this.getClass().getSimpleName() + ": save delete empty " + apm + ":" + this);
            }
            apm.delete();
        } else {
            try {
                if (FotoLibGlobal.debugEnabled) {
                    logger.debug(this.getClass().getSimpleName() + ": save to " + apm + ":" + this);
                }
                stream = new FileOutputStream(apm);
                properties.store(stream, PhotoWorkFlowDto.sFileComment);
            } finally {
                FileUtils.close(stream, "PhotoWorkFlowDto.load(" + apm + ")");
            }
        }
    }

    /** Android support: to persist state and to transfer activites via intent.  */
    public static PhotoWorkFlowDto load(Serializable content) {
        PhotoWorkFlowDto photoWorkFlowDto = null;
        if (content instanceof Properties ) {
            Properties properties = (Properties) content;
            String outDir = properties.getProperty(KEY_OUT_DIR);
            photoWorkFlowDto = new PhotoWorkFlowDto((outDir != null) ? new File(outDir) : null, properties);
        }
        if (FotoLibGlobal.debugEnabled) {
            logger.debug(PhotoWorkFlowDto.class.getSimpleName() + ": load De-Serialize:" + photoWorkFlowDto);
        }
        return photoWorkFlowDto;
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
        return getProperty(KEY_DATE_FORMAT);
    }

    /** DateFormat part for {@link RuleFileNameProcessor} */
    public PhotoWorkFlowDto setDateFormat(String dateFormat) {
        setProperty(KEY_DATE_FORMAT,dateFormat);
        return this;
    }

    /**  fixed-Name part for {@link RuleFileNameProcessor} */
    public String getName() {
        return getProperty(KEY_NAME);
    }

    /**  fixed-Name part for {@link RuleFileNameProcessor} */
    public PhotoWorkFlowDto setName(String Name) {
        setProperty(KEY_NAME,Name);
        return this;
    }

    /**  NumberFormat part for {@link RuleFileNameProcessor} */
    public String getNumberFormat() {
        return getProperty(KEY_NUMBER_FORMAT);
    }

    /**  NumberFormat part for {@link RuleFileNameProcessor} */
    public PhotoWorkFlowDto setNumberFormat(String NumberFormat) {
        setProperty(KEY_NUMBER_FORMAT,NumberFormat);
        return this;
    }

    public File getOutDir() {
        return outDir;
    }
    public PhotoWorkFlowDto setOutDir(File outDir) {
        this.outDir = outDir;
        return this;
    }

    public IFileNameProcessor createFileNameProcessor() {
        return new RuleFileNameProcessor(getDateFormat(), getName(), getNumberFormat(), getOutDir());
    }

    public IMetaApi getMediaDefaults() {
        String mediaDefaultString = getProperty(KEY_EXIF);
        return (mediaDefaultString == null) ? null : new MediaAsString().fromString(mediaDefaultString);
    }

    public PhotoWorkFlowDto setMediaDefaults(IMetaApi mediaDefaults) {
        String mediaDefaultString = null;
        if (mediaDefaults != null) {
            mediaDefaultString = (mediaDefaults instanceof MediaAsString)
               ? mediaDefaults.toString()
               : new MediaAsString().setData(mediaDefaults).toString();
        }

        setProperty(KEY_EXIF, mediaDefaultString);
        return this;
    }

    public boolean isEmpty() {
        return ((getMediaDefaults() == null) && isRenameEmpty());
    }

    public boolean isRenameEmpty() {
        return (getNumberFormat() == null)
                && (getDateFormat() == null) && (getName() == null);
    }

    private String getProperty(String key) {
        String value = properties.getProperty(key);
        if (StringUtils.isNullOrEmpty(value)) return null;
        return value;
    }

    private Object setProperty(String key, String value) {
        toString();
        if (StringUtils.isNullOrEmpty(value))
            return properties.remove(key);
        else
            return properties.put(key, value);
    }

    @Override
    public String toString() {
        return ListUtils.toString(" ", this.getClass().getSimpleName(),
                getDateFormat(), getName(), getNumberFormat(), getMediaDefaults());
    }

    public void clear() {
        properties.clear();
    }
}
