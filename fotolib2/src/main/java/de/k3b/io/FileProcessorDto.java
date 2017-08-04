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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by k3b on 04.08.2017.
 */

public class FileProcessorDto {
    private static final String KEY_DATE_FORMAT = "DateFormat";
    private static final String KEY_NAME = "Name";
    private static final String KEY_NUMBER_FORMAT = "NumberFormat";

    private final Properties properties;
    private File outDir;

    public FileProcessorDto() {
        this(null, new Properties());
    }

    public void load(File outDir) throws IOException {
        this.outDir = outDir;
        File apm = new File(outDir, FileNameProcessor.APM_FILE_NAME);
        properties.clear();
        if (apm.exists() && apm.isFile() && apm.canRead()) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(apm);
                properties.load(inputStream);
            } finally {
                FileUtils.close(inputStream,"FileProcessorDto.load(" + apm + ")");
            }
        }
    }

    public void save() throws IOException {
        File apm = new File(outDir, FileNameProcessor.APM_FILE_NAME);
        if (apm.exists() && apm.isFile() && apm.canRead()) {
            FileOutputStream inputStream = null;
            try {
                inputStream = new FileOutputStream(apm);
                properties.store(inputStream, "");
            } finally {
                FileUtils.close(inputStream,"FileProcessorDto.load(" + apm + ")");
            }
        }
    }

    protected FileProcessorDto(File outDir, Properties properties) {
        this.outDir = outDir;
        this.properties = properties;
    }

    public String getDateFormat() {
        return properties.getProperty(KEY_DATE_FORMAT);
    }

    public void setDateFormat(String dateFormat) {
        properties.setProperty(KEY_DATE_FORMAT,dateFormat);
    }

    public String getName() {
        return properties.getProperty(KEY_NAME);
    }

    public void setName(String Name) {
        properties.setProperty(KEY_NAME,Name);
    }

    public String getNumberFormat() {
        return properties.getProperty(KEY_NUMBER_FORMAT);
    }

    public void setNumberFormat(String NumberFormat) {
        properties.setProperty(KEY_NUMBER_FORMAT,NumberFormat);
    }

    public File getOutDir() {
        return outDir;
    }
}
