/*
 * Copyright (c) 2015 by k3b.
 *
 * This file is part of LocationMapViewer.
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

package de.k3b.geo.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.api.IGeoRepository;

/**
 * Repository to load/save List< {@link de.k3b.geo.api.GeoPointDto} > in a file.
 *
 * Created by k3b on 17.03.2015.
 */
public class GeoFileRepository<T extends IGeoPointInfo> implements IGeoRepository<T> {
    private static final Logger logger = LoggerFactory.getLogger(GeoFileRepository.class);

    /** used to translate between {@link de.k3b.geo.api.IGeoPointInfo} and geo-uri string */
    private static final GeoUri converter = new GeoUri(GeoUri.OPT_DEFAULT);
    public static final java.lang.String COMMENT = "#";

    /** where data is loaded from/saved to */
    private final File mFile;
    private final GeoPointDto mFactory;

    /** the content of the repository */
    protected List<T> mData = null;

    /** connect repository to file */
    public GeoFileRepository(File file, GeoPointDto factory) {
        this.mFile = file;
        this.mFactory = factory;
    }

    /** load from repository
     *
     * @return data loaded
     */
    public List<T> load() {
        if (mData == null) {
            mData = new ArrayList<>();
            if (this.mFile.exists()) {
                try {
                    load(mData, new FileReader(this.mFile));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("load(): " + mData.size() + " items from " + this.mFile);
            }
        } else if (logger.isDebugEnabled()) {
            logger.debug("load() cached value : " + mData.size() + " items from " + this.mFile);
        }
        return mData;
    }

    /**
     * uncached, fresh load from repository
     *
     * @return data loaded
     */
    @Override
    public List<T> reload() {
        this.mData = null;
        return load();
    }

    /** generate a new id */
    public String createId() {
        return UUID.randomUUID().toString();
    }

    /**
     * removes item from repository.
     *
     * @param item that should be removed
     * @return true if successful
     */
    @Override
    public boolean delete(T item) {
        if ((item != null) && load().remove(item)) {
            save();
            return true;
        }

        return false;
    }

    /** save to repository
     *
     * @return false: error.
     */
    public boolean save() {
        try {
            if ((mData != null) && (mData.size() > 0)) {
                if (!this.mFile.exists()) {
                    this.mFile.getParentFile().mkdirs();
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("save(): " + mData.size() + " items to " + this.mFile);
                }
                save(mData, new FileWriter(this.mFile, false));
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("save(): no items for " + this.mFile);
        }
        return false;
    }

    // load(new InputStreamReader(inputStream, "UTF-8"))
    /** load points from reader */
    public void load(List<T> result, Reader reader) throws IOException {
        String line;
        BufferedReader br = new BufferedReader(reader);
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if ((line.length() > 0) && (!line.startsWith(COMMENT))) {
                GeoPointDto geo = loadItem(line);
                final boolean valid = isValid(geo);
                if (logger.isDebugEnabled()) {
                    logger.debug("load(" + line + "): " + ((valid) ? "loaded" : "ignored"));
                }

                if (valid) result.add((T) geo);
            }
        }
        br.close();
    }

    protected GeoPointDto loadItem(String line) {
        return converter.fromUri(line, create());
    }

    protected GeoPointDto create() {
        return (GeoPointDto) mFactory.clone().clear();
    }

    /** save points to writer */
    void save(List<T> source, Writer writer) throws IOException {
        for (T geo : source) {
            saveItem(writer, geo);
        }
        writer.close();
    }

    protected boolean saveItem(Writer writer, T geo) throws IOException {
        final boolean valid = isValid(geo);

        final String line = converter.toUriString(geo);
        if (valid) {
            writer.write(line);
            writer.write("\n");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("save(" + line + "): " + ((valid) ? "saved" : "ignored" ));
        }
        return valid;
    }

    /** returns true if geo should be loaded from / saved to repository */
    protected boolean isValid(IGeoPointInfo geo) {
        return ((geo != null) && (isValidId(geo.getId())));
    }

    /** returns true if geo.id should be loaded from / saved to repository */
    private boolean isValidId(String id) {
        return ((id != null) && (!id.startsWith("#")));
    }
}
