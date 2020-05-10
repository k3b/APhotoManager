/*
 * Copyright (c) 2020 by k3b.
 *
 * This file is part of #APhotoManager (https://github.com/k3b/APhotoManager/)
 *              and #toGoZip (https://github.com/k3b/ToGoZip/).
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
package de.k3b.io.filefacade;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import de.k3b.io.FileUtils;

public class StringFileFacadeTest {

    @Test
    public void copyInputStringToOutputString() throws IOException {
        final String message = "hello world\n";
        StringFileFacade in = new StringFileFacade().setInputString(message);
        StringFileFacade out = new StringFileFacade();

        FileUtils.copy(in.openInputStream(), out.openOutputStream(), "StringFileFacadeTest");

        Assert.assertEquals(message, out.getOutputString());
    }
}