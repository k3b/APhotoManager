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

package de.k3b.geo.io;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;

/**
 * Created by k3b on 13.01.2015.
 */
public class GeoUriTests {
    /**
     * geo:{lat{,lon{,hight_ignore}}}{?q={lat{,lon{,hight_ignore}}}{(name)}{|link{|id}|}{description}}{&z={{zmin}-zmax}}
     * */
    @Test
    public void shouldFormat() throws Exception {
        GeoUri sut = new GeoUri(GeoUri.OPT_DEFAULT);
        GeoUri sutRedundant = new GeoUri(GeoUri.OPT_FORMAT_REDUNDANT_LAT_LON);

        System.out.print(sutRedundant.toUriString(createTestGeoPoint()));

        Assert.assertEquals("geo:", sut.toUriString(new GeoPointDto()));
        Assert.assertEquals("geo:123.456", sut.toUriString(new GeoPointDto().setLatitude(123.456)));
        Assert.assertEquals("geo:,-23", sut.toUriString(new GeoPointDto().setLongitude(-23.0)));
        Assert.assertEquals("geo:0,0", sut.toUriString(new GeoPointDto().setLongitude(0.0).setLatitude(0.0)));
        Assert.assertEquals("geo:123.456,-23?q=123.456,-23", sutRedundant.toUriString(new GeoPointDto().setLatitude(123.456).setLongitude(-23.0)));
    }

    @Test
    public void shouldParse() throws Exception {
        GeoUri sut = new GeoUri(GeoUri.OPT_DEFAULT);

        String original = sut.toUriString(createTestGeoPoint());

        IGeoPointInfo parsed = sut.fromUri(original);

        Assert.assertEquals(original, sut.toUriString(parsed));
    }

    @Test
    public void sholdParseMinimal() throws Exception {
        GeoUri sut = new GeoUri(GeoUri.OPT_DEFAULT);

        String uri = "geo:1,2";

        GeoPointDto parsed = (GeoPointDto)sut.fromUri(uri);

        Assert.assertEquals(1, parsed.getLatitude(), 0.002);
    }

    @Test
    public void parseShouldNotInfer() throws Exception {
        GeoUri sut = new GeoUri(GeoUri.OPT_DEFAULT);

        String uri = "geo:?d=I was in (Hamburg) located at 53,10 on 1991-03-03T04:05:06Z";

        GeoPointDto parsed = (GeoPointDto)sut.fromUri(uri);
        parsed.setDescription(null); // remove "d=description" so only the infered data remains

        Assert.assertEquals("geo:", sut.toUriString(parsed));
    }

    @Test
    public void parseShouldInfer() throws Exception {
        GeoUri sut = new GeoUri(GeoUri.OPT_PARSE_INFER_MISSING);

        String uri = "geo:?d=I was in (Hamburg) located at 53,10 on 1991-03-03T04:05:06Z";

        GeoPointDto parsed = (GeoPointDto)sut.fromUri(uri);
        parsed.setDescription(null); // remove "d=description" so only the infered data remains

        Assert.assertEquals("geo:53,10?q=(Hamburg)&t=1991-03-03T04:05:06Z", sut.toUriString(parsed));
    }

    @Test
    public void clearedDtoShouldFormatEmpty() throws Exception {
        GeoUri formatter = new GeoUri(GeoUri.OPT_DEFAULT);

        final GeoPointDto testGeoPoint = createTestGeoPoint();
        testGeoPoint.clear();
        String result = formatter.toUriString(testGeoPoint);

        Assert.assertEquals("geo:", result);
    }

    /**
     * geoarea:{lat},{lon},{lat},{lon}
     * */
    @Test
    public void shouldFormatArea() throws Exception {
        GeoPointDto ne = new GeoPointDto(12.345, -56.789, GeoPointDto.NO_ZOOM);
        GeoPointDto sw = new GeoPointDto(12.0, -53, GeoPointDto.NO_ZOOM);

        GeoUri sut = new GeoUri(GeoUri.OPT_DEFAULT);

        String result = sut.toUriString(ne, sw);

        Assert.assertEquals("geoarea:12.345,-56.789,12,-53", result);
    }


    /**
     * geoarea:{lat},{lon},{lat},{lon}
     * */
    @Test
    public void shouldParseArea() throws Exception {
        GeoUri sut = new GeoUri(GeoUri.OPT_DEFAULT);

        GeoPointDto[] resultPoints = new GeoPointDto[]{new GeoPointDto(),new GeoPointDto()};
        sut.fromUri("geoarea:12.345,-56.789,12,-53", resultPoints);

        String result = sut.toUriString(resultPoints[0], resultPoints[1]);

        Assert.assertEquals("geoarea:12.345,-56.789,12,-53", result);
    }
    
    private GeoPointDto createTestGeoPoint() {
        return new GeoPointDto(12.345, -56.78901234, "name", "link", "icon", "id", "description", 5, 7, new Date(91, 2, 3, 4, 5, 6));
        /*
        return new GeoPointDto(179.345, -86.78901234, "name, title or caption",
                "https://link/to/additional/info.htm",
                "https://link/to/symbol.png", "id", "Some description of the location",
                5, 7, new Date(91, 2, 3, 4, 5, 6));
        */
    }
}
