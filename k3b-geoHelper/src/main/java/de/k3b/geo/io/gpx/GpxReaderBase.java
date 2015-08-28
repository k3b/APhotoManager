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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoInfoHandler;
import de.k3b.geo.io.GeoFormatter;
import de.k3b.geo.io.GeoUri;
import de.k3b.geo.io.GeoUriDef;
import de.k3b.util.IsoDateTimeParser;

/**
 * Parser for xml-geo formats implemented for
 *  - gpx-1.1 http://www.topografix.com/GPX/1/1/ and
 *  - gpx-1.0 http://www.topografix.com/GPX/1/0/ and
 *  - poi (de.k3b.geo internal format compatible with geo-uri format)
 *  - kml-2.2 (a little bit of http://www.opengis.net/kml/2.2).
 *
 * This parser is not acurate: it might pick elements from wrong namespaces.
 *
 * Note: if you change/add features to this xml parser
 * ...\LocationMapViewer\k3b-geoHelper\src\main\java\de.k3b.geo.io.gpx.GpxReaderBase.java
 * please also update regression test-data and prog at
 * ...\LocationMapViewer\k3b-geoHelper\src\test\resources\de\k3b\geo\io\regressionTests\*.*
 * ...\LocationMapViewer\k3b-geoHelper\src\test\java\de.k3b.geo.io.GeoPointDtoRegressionTests.java
 *
 * Created by k3b on 20.01.2015.
 */
public class GpxReaderBase extends DefaultHandler {
    private static final Logger logger = LoggerFactory.getLogger(GpxReaderBase.class);

    protected IGeoInfoHandler onGotNewWaypoint;

    /** if not null this instance is cleared and then reused for every new gpx found */
    protected final GeoPointDto mReuse;

    /** if not null gpx-v11: "trkpt" parsing is active */
    protected GeoPointDto current;
	
    private StringBuffer buf = new StringBuffer();

    // used by <poi geoUri='geo:...' />. created on demand.
    private GeoUri geoUriParser = null;

    public GpxReaderBase(final IGeoInfoHandler onGotNewWaypoint, final GeoPointDto reuse) {
        this.onGotNewWaypoint = onGotNewWaypoint;
        this.mReuse = reuse;
    }

    /** processes in and calls onGotNewWaypoint for every waypoint found */
    public void parse(InputSource in) throws IOException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            // factory.setValidating(true);
            SAXParser parser = factory.newSAXParser();
            parser.parse(in, this);
        } catch (ParserConfigurationException e) {
            final String message = "Error parsing xml from " + in;
            logger.error(message, e);
            throw new IOException(message,e);
        } catch (SAXException e) {
            final String message = "Error parsing xml from " + in;
            logger.error(message, e);
            throw new IOException(message,e);
        }
    }

    /** returns an instance of an empty {@link de.k3b.geo.api.GeoPointDto} */
    protected GeoPointDto newInstance() {
        if (mReuse != null) return mReuse.clear();
        return new GeoPointDto();
    }

    /** returns an instance of an empty {@link de.k3b.geo.api.GeoPointDto}
     * and tries to find non standard attributes */
    protected GeoPointDto newInstance(Attributes attributes) {
        GeoPointDto result = newInstance();

        String geoUri=attributes.getValue(GeoUriDef.XML_ATTR_GEO_URI);
        if (geoUri != null) {
            String mode=attributes.getValue(GeoUriDef.XML_ATTR_GEO_URI_INFER_MISSING);
            if ((mode != null) || (this.geoUriParser == null)) {
                int modes = GeoUri.OPT_DEFAULT;

                if (mode != null) {
                    mode = mode.trim().toLowerCase();

                    if ((!mode.startsWith("0")) && (!mode.startsWith("f"))) {
                        // everything except "0" or "false" is infer
                        modes |= GeoUri.OPT_PARSE_INFER_MISSING;
                    }
                }
                this.geoUriParser = new GeoUri(modes);
            }
            geoUriParser.fromUri(geoUri, result);
        }

        // explicit attributes can overwrite values from geoUri=...
        String value = attributes.getValue(GeoUriDef.LAT_LON);
        if (value != null) GeoUri.parseLatOrLon(result, value);

        value = attributes.getValue(GeoUriDef.NAME);
        if (value != null) result.setName(value);

        value = attributes.getValue(GeoUriDef.DESCRIPTION);
        if (value != null) result.setDescription(value);

        value = attributes.getValue(GeoUriDef.ID);
        if (value != null) result.setId(value);

        value = attributes.getValue(GeoUriDef.LINK);
        if (value != null) result.setLink(value);

        value = attributes.getValue(GeoUriDef.SYMBOL);
        if (value != null) result.setSymbol(value);

        value = attributes.getValue(GeoUriDef.ZOOM);
        if (value != null) result.setZoomMin(GeoFormatter.parseZoom(value));

        value = attributes.getValue(GeoUriDef.ZOOM_MAX);
        if (value != null) result.setZoomMax(GeoFormatter.parseZoom(value));

        value = attributes.getValue(GeoUriDef.TIME);
        if (value != null) result.setTimeOfMeasurement(IsoDateTimeParser.parse(value));

        return result;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        String name = getElementName(localName, qName);
        
        logger.debug("startElement {}-{}", localName, qName);
        if (name.equals(GpxDef_11.TRKPT) || name.equals(GpxDef_10.WPT)) {
            this.current = this.newInstance(attributes);
            final String lat = attributes.getValue(GpxDef_11.ATTR_LAT);
            if (lat != null) this.current.setLatitude(Double.parseDouble(lat));
            final String lon = attributes.getValue(GpxDef_11.ATTR_LON);
            if (lon != null) this.current.setLongitude(Double.parseDouble(lon));
        } else if ((name.equals(KmlDef_22.PLACEMARK)) || (name.equals(GeoUriDef.XML_ELEMENT_POI))) {
            this.current = this.newInstance(attributes);
        } else if ((this.current != null) && (name.equals(GpxDef_11.LINK) || name.equals(GpxDef_10.URL))) {
            this.current.setLink(attributes.getValue(GpxDef_11.ATTR_LINK));
        }
		if (this.current != null) {
			buf.setLength(0);
		}
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        String name = getElementName(localName, qName);
        logger.debug("endElement {} {}", localName, qName);
        if (name.equals(GpxDef_11.TRKPT) || name.equals(GpxDef_10.WPT) || name.equals(KmlDef_22.PLACEMARK) || name.equals(GeoUriDef.XML_ELEMENT_POI)) {
            this.onGotNewWaypoint.onGeoInfo(this.current);
            this.current = null;
        } else if (this.current != null) {
            if (name.equals(GpxDef_11.NAME)) {
                this.current.setName(buf.toString());
            } else if (name.equals(GpxDef_11.DESC) || name.equals(KmlDef_22.DESCRIPTION)) {
                this.current.setDescription(buf.toString());
            } else if ((null == this.current.getLink()) && (name.equals(GpxDef_11.LINK) || name.equals(GpxDef_10.URL))) {
                this.current.setLink(buf.toString());
            } else if (name.equals(GeoUriDef.ID)) {
                this.current.setId(buf.toString());
            } else if (name.equals(GpxDef_11.TIME) || name.equals(KmlDef_22.TIMESTAMP_WHEN) || name.equals(KmlDef_22.TIMESPAN_BEGIN)) {
                final Date dateTime = IsoDateTimeParser.parse(buf.toString());
                if (dateTime != null) {
                    this.current.setTimeOfMeasurement(dateTime);
                } else {
                    saxError("/gpx//time or /kml//when or /kml//begin: invalid time "
                            + name +"=" + buf.toString());
                }

            } else if (name.equals(KmlDef_22.COORDINATES) || name.equals(KmlDef_22.COORDINATES2)) {
                // <coordinates>lon,lat,height blank lon,lat,height ...</coordinates>
                try {
                    String parts[] = buf.toString().split("[,\\s]");
                    this.current.setLatitude(Double.parseDouble(parts[1]));
                    this.current.setLongitude(Double.parseDouble(parts[0]));
                } catch (NumberFormatException e) {
                    saxError("/kml//Placemark/Point/coordinates>Expected: 'lon,lat,...' but got "
                            + name +"=" + buf.toString());
                }
            }
        }
    }

    private void saxError(String message) throws SAXException {
        throw new SAXException(message);
    }
    private String getElementName(String localName, String qName) {
        if ((localName != null) && (localName.length() > 0))
            return localName;
        if (qName == null) return "";

        int delim = qName.indexOf(":");
        if (delim < 0) return qName;

        return qName.substring(delim+1);
    }

    @Override
    public void characters(char[] chars, int start, int length)
            throws SAXException {
		if (this.current != null) {
			buf.append(chars, start, length);
		}
    }
}
