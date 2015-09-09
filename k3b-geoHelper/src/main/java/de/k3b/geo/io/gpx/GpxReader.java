/*
 * Copyright (C) 2015 k3b
 *
 * This file is part of de.k3b.android.LocationMapViewer (https://github.com/k3b/LocationMapViewer/) .
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
package de.k3b.geo.io.gpx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xml.sax.InputSource;

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoInfoHandler;
import de.k3b.geo.api.IGeoPointInfo;

/**
 * Reads {@link de.k3b.geo.api.GeoPointDto} from gpx file or stream.<br/>
 *
 * inspired by http://stackoverflow.com/questions/672454/how-to-parse-gpx-files-with-saxreader
 */
public class GpxReader<T extends IGeoPointInfo> extends GpxReaderBase implements IGeoInfoHandler {
    private List<T> track;

    /**
     * Creates a new GpxReader
     * @param reuse if not null this instance is cleared and then reused for every new gpx found
     */
    public GpxReader(final GeoPointDto reuse) {
        super(null,reuse);
        this.onGotNewWaypoint = this; // cannot do this in constructor
    }

    public List<T> getTracks(InputSource in) throws IOException {
        track = new ArrayList<T>();
        parse(in);
        return track;
    }

    /** is called for every completed gpx-trackpoint */
    @Override
    public boolean onGeoInfo(IGeoPointInfo geoInfo) {
        if (mReuse != null) {
            track.add((T) mReuse.clone());
        } else {
            track.add((T) this.current);
        }
        return true;
    }

}
