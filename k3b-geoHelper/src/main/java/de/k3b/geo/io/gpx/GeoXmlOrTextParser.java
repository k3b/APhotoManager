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

package de.k3b.geo.io.gpx;

import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoFileRepository;
import de.k3b.geo.io.GeoUri;

/**
 * gets geoItems from text.
 * Created by k3b on 20.04.2015.
 */
public class GeoXmlOrTextParser<T extends IGeoPointInfo> {
    public List<T> get(GeoPointDto baseItem, String src) {
        if (src == null) return null;

        if (src.startsWith(GeoFileRepository.COMMENT) || src.startsWith(GeoUri.GEO_SCHEME)) {
            // lines of comments "#.." or geo-uris seperated by cr/lf
            GeoFileRepository<T> parser = new GeoFileRepository<T>(null, baseItem) {
                @Override
                protected boolean isValid(IGeoPointInfo geo) {
                    return true;
                }
            };
            StringReader rd = new StringReader(src);
            ArrayList<T> result = new ArrayList<T>();

            try {
                parser.load(result, rd);
                rd.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        } else {
            if (!src.startsWith("<?xml")) {
                // to allow xml-fragments without xml-root element
                src = "<xml>" + src + "</xml>";
            }
            GpxReader<T> parser = new GpxReader<T>(baseItem);
            StringReader rd = new StringReader(src);
            List<T> result = null;

            try {
                result = parser.getTracks(new InputSource(rd));
            } catch (IOException e) {
                e.printStackTrace();
            }
            rd.close();
            return result;
        }
    }

}
