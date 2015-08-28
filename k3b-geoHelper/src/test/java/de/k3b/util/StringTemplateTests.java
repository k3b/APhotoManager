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

package de.k3b.util;

import org.junit.*;

/**
 * Created by k3b on 01.04.2015.
 */
public class StringTemplateTests {
    StringTemplateEngine sut = null;

    @Before
    public void setup() {
        sut = new StringTemplateEngine(new StringTemplateEngine.IValueResolver() {
            @Override
            public String get(String className, String propertyName, String templateParameter) {
                if ((0==className.compareTo("first")) && (0==propertyName.compareTo("name"))) {
                    return "world";
                }
                return "???";
            }
        });
    }

    @Test
    public void shouldFormat() {
        String result = sut.format("hello ${first.name}!");

        Assert.assertEquals("hello world!", result);
    }

    @Test
    public void shouldFormatMultible() {
        String result = sut.format("hello ${first.name} ${something.else.where}!");

        Assert.assertEquals("hello world ???!", result);
    }

    @Test
    public void shouldNotMatch() {
        String result = sut.format("hello ${first} ${something else}!");

        Assert.assertEquals("hello ${first} ${something else}!", result);
    }
}
