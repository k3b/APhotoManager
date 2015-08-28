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

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.gpx.GpxReader;

/**
 * Regressionstests: load *.kml/*.gpx/*.poi file from resources
 * and make shure that for every parsed GeoPointDto in the result is identical to id of poi.<br/>
 *
 * Created by k3b on 19.04.2015.
 */
public class GeoPointDtoRegressionTests {
    private static final String REGRESSION_ROOT = "/de/k3b/geo/io/regressionTests/";
    private static final GeoUri formatter = new GeoUri(GeoUri.OPT_DEFAULT);

    private String currentResourceName = null;
    private StringBuffer checkResultMessage = null;

    @Test
    public void regressionTest() {
        check("0|empty.xml", "1|gpx11.gpx", "1|gpx10.gpx" , "2|kml22.kml", "2|gpx-similar.gpx", "6|poi.xml");
        Assert.assertEquals(null, this.checkResultMessage);
    }

    private void check(String... streamNames) {
        checkResultMessage = null;
        for (String streamName : streamNames) {
            final String[] testCase = streamName.split("\\|");
            checkStream(testCase[0], REGRESSION_ROOT + testCase[1]);
        }
    }

    private void checkStream(String expectedNumberOfPois, String resourceName) {
        checkStream(Integer.valueOf(expectedNumberOfPois), getStream(resourceName), resourceName);
    }

    private InputStream getStream(String _resourceName) {
        this.currentResourceName = _resourceName;

        // this does not work with test-resources :-(
        // or i donot know how to do it with AndroidStudio-1.02/gradle-2.2
        InputStream result = this.getClass().getResourceAsStream(this.currentResourceName);

        if (result == null) {
            File prjRoot = new File(".").getAbsoluteFile();
            while (prjRoot.getName().compareToIgnoreCase("LocationMapViewer") != 0) {
                prjRoot = prjRoot.getParentFile();
                if (prjRoot == null) return null;
            }

            // assuming this src folder structure:
            // .../LocationMapViewer/k3b-geoHelper/src/test/resources/....
            File resourceFile = new File(prjRoot, "k3b-geoHelper/src/test/resources" + _resourceName);

            this.currentResourceName = resourceFile.getAbsolutePath(); // . new Path(resourceName).get;
            try {
                result = new FileInputStream(this.currentResourceName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
        return result;
    }

    private void checkStream(Integer expectedNumberOfPois, InputStream xmlStream, String resourceName) {
        try {
            GpxReader<IGeoPointInfo> parser = new GpxReader<IGeoPointInfo>(null);
            List<IGeoPointInfo> pois = parser.getTracks(new InputSource(xmlStream));
            if (expectedNumberOfPois != pois.size()) {
                addError("Expected " + expectedNumberOfPois +" but got " + pois.size());
            }
            checkPois(pois);
            xmlStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            addError("cannot load  " +
                    resourceName + ": " + e.getMessage() + "\n" + e.getStackTrace());
        }
    }

    private void checkPois(List<IGeoPointInfo> pois) {
        int index = 0;
        for (IGeoPointInfo poi : pois) {
            checkPoi(index++, poi, poi.getId());
        }
    }

    private void checkPoi(int index, IGeoPointInfo poi, String expected) {
        String actual= formatter.toUriString(new GeoPointDto(poi).setId(null));

        if ((expected != null) && (0 != expected.compareTo(actual))) {
            addError("#" + index + " failed: expected '" + expected + "' but got '" + actual + "'");
        }
    }

    private void addError(String message) {
        if (this.checkResultMessage == null) {
            this.checkResultMessage = new StringBuffer();
        }

        this.checkResultMessage.append(this.currentResourceName).append(": ").append(message).append("\n");
    }
}
