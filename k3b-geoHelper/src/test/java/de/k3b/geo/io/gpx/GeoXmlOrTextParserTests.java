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

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;

/**
 * Created by EVE on 20.04.2015.
 */
public class GeoXmlOrTextParserTests {
    @Test
    public void parseXml()  {
        String data = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<root><poi ll='52.2,9.2'/><poi ll='52.1,9.1'/></root>";

        List<IGeoPointInfo> result = new GeoXmlOrTextParser<IGeoPointInfo>().get(new GeoPointDto(), data);
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void parseXmlFragment()  {
        String data = "<poi ll='52.2,9.2'/><poi ll='52.1,9.1'/>";

        List<IGeoPointInfo> result = new GeoXmlOrTextParser<IGeoPointInfo>().get(new GeoPointDto(), data);
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void parseUris()  {
        String data = "#test uri\n" +
            "geo:52.1,9.1\n" +
            "geo:52.2,9.2\n";

        List<IGeoPointInfo> result = new GeoXmlOrTextParser<IGeoPointInfo>().get(new GeoPointDto(), data);
        Assert.assertEquals(2, result.size());
    }


}
