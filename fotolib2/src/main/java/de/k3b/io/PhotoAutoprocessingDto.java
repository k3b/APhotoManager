/*
 * Copyright (c) 2017-2020 by k3b.
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import de.k3b.LibGlobal;
import de.k3b.media.IPhotoProperties;
import de.k3b.media.PhotoPropertiesAsString;

/**
 * #93: Persistable data for autoproccessing images (auto-rename, auto-add-exif).
 *
 * Implemented as Properties file.
 *
 * Created by k3b on 04.08.2017.
 */

public class PhotoAutoprocessingDto implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(LibGlobal.LOG_TAG);

    /** added to every serialized item if != null. Example "Generated on 2015-10-19 with myApp Version 0815." */
    public static String sFileComment = "";

    // KEY_xxx for properties file
    private static final String KEY_DATE_FORMAT     = "DateFormat";
    private static final String KEY_NAME            = "Name";
    private static final String KEY_NUMBER_FORMAT   = "NumberFormat";
    private static final String KEY_EXIF            = "Exif";
    private static final String KEY_OUT_DIR         = "outDir";

    private final Properties properties;
    private IFile outDir;

    public PhotoAutoprocessingDto() {
        this(null, new Properties());
    }

    public PhotoAutoprocessingDto(IFile outDir, Properties properties) {
        this.outDir = outDir;
        this.properties = properties;
    }

    public static IFile getApmFile(IFile outDir) {
        return outDir.create(RuleFileNameProcessor.APM_FILE_NAME);
    }

    public void paste(PhotoAutoprocessingDto newData) {
        if (newData != null) {
            this.setDateFormat(newData.getDateFormat());
            this.setNumberFormat(newData.getNumberFormat());
            this.setMediaDefaults(newData.getMediaDefaults());

            // Fix Autoprocessing/PhotoAutoprocessingDto renaming rules that contain source file direcory names.
            String name = getTranslateName(newData);
            this.setName(name);
        }

    }

    /**
     * Get the fixed rename-name part in case Autoprocessing/PhotoAutoprocessingDto was moved/copied to a
     * different dir and renaming rules contain source file direcory names.
     */
    public String getTranslateName(PhotoAutoprocessingDto newData) {
        if (newData != null) {
            final RuleFileNameProcessor srcData = (RuleFileNameProcessor) newData.createFileNameProcessor();
            if (srcData != null) {
                return RuleFileNameProcessor.translateName(srcData, this.getOutDir());
            }
        }
        return null;
    }

    /**
     * Android support: to persist state and to transfer activites via intent.
     */
    public static PhotoAutoprocessingDto load(Serializable content) {
        PhotoAutoprocessingDto photoAutoprocessingDto = null;
        if (content instanceof Properties) {
            Properties properties = (Properties) content;
            String outDir = properties.getProperty(KEY_OUT_DIR);
            photoAutoprocessingDto = new PhotoAutoprocessingDto((outDir != null)
                    ? FileFacade.convert("PhotoAutoprocessingDto load", outDir) : null, properties);
        }
        if (LibGlobal.debugEnabled) {
            logger.debug(PhotoAutoprocessingDto.class.getSimpleName() + ": load De-Serialize:" + photoAutoprocessingDto);
        }
        return photoAutoprocessingDto;
    }

    public PhotoAutoprocessingDto load(IFile outDir) throws IOException {
        this.outDir = outDir;
        IFile apm = getApmFile();
        properties.clear();
        if (apm.exists() && apm.isFile() && apm.canRead()) {
            properties.load(apm.openInputStream());
            if (LibGlobal.debugEnabled) {
                logger.debug(this.getClass().getSimpleName() + ": loaded from " + apm + ":" + this);
            }
            return this;
        }
        return null;
    }

    private IFile getApmFile() {
        return getApmFile(this.outDir);
    }

    /** if has no data the file is deleted */
    public void save() throws IOException {
        IFile apm = getApmFile();
        OutputStream stream = null;
        if (isEmpty()) {
            if (LibGlobal.debugEnabled) {
                logger.debug(this.getClass().getSimpleName() + ": save delete empty " + apm + ":" + this);
            }
            apm.delete();
        } else {
            try {
                if (LibGlobal.debugEnabled) {
                    logger.debug(this.getClass().getSimpleName() + ": save to " + apm + ":" + this);
                }
                stream = apm.openOutputStream();
                properties.store(stream, PhotoAutoprocessingDto.sFileComment);
            } finally {
                FileUtils.close(stream, "PhotoAutoprocessingDto.load(" + apm + ")");
            }
        }
    }

    /** DateFormat part for {@link RuleFileNameProcessor} */
    public String getDateFormat() {
        return getProperty(KEY_DATE_FORMAT);
    }

    /** DateFormat part for {@link RuleFileNameProcessor} */
    public PhotoAutoprocessingDto setDateFormat(String dateFormat) {
        setProperty(KEY_DATE_FORMAT,dateFormat);
        return this;
    }

    /**  fixed-Name part for {@link RuleFileNameProcessor} */
    public String getName() {
        return getProperty(KEY_NAME);
    }

    /**  fixed-Name part for {@link RuleFileNameProcessor} */
    public PhotoAutoprocessingDto setName(String Name) {
        setProperty(KEY_NAME,Name);
        return this;
    }

    /**  NumberFormat part for {@link RuleFileNameProcessor} */
    public String getNumberFormat() {
        return getProperty(KEY_NUMBER_FORMAT);
    }

    /**  NumberFormat part for {@link RuleFileNameProcessor} */
    public PhotoAutoprocessingDto setNumberFormat(String NumberFormat) {
        setProperty(KEY_NUMBER_FORMAT,NumberFormat);
        return this;
    }

    public IFile getOutDir() {
        return outDir;
    }

    public PhotoAutoprocessingDto setOutDir(IFile outDir) {
        this.outDir = outDir;
        return this;
    }

    public IFileNameProcessor createFileNameProcessor() {
        return new RuleFileNameProcessor(getDateFormat(), getName(), getNumberFormat(), getOutDir());
    }

    /** exif data that should be applied to every jpg file */
    public IPhotoProperties getMediaDefaults() {
        String mediaDefaultString = getProperty(KEY_EXIF);
        return (mediaDefaultString == null) ? null : new PhotoPropertiesAsString().fromString(mediaDefaultString);
    }

    /** exif data that should be applied to every jpg file */
    public PhotoAutoprocessingDto setMediaDefaults(IPhotoProperties mediaDefaults) {
        String mediaDefaultString = toString(mediaDefaults);
        setProperty(KEY_EXIF, mediaDefaultString);
        return this;
    }

    public static String toString(IPhotoProperties mediaDefaults) {
        if (mediaDefaults != null) {
            return (mediaDefaults instanceof PhotoPropertiesAsString)
               ? mediaDefaults.toString()
               : new PhotoPropertiesAsString().setData(mediaDefaults).toString();
        }
        return null;
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
