/*
 * Copyright (c) 2019-2020 by k3b.
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
package de.k3b.android.androFotoFinder.media;

import android.content.Context;

import org.junit.Test;
import org.mockito.Matchers;

import de.k3b.media.PhotoPropertiesFormatter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AndroidLabelGeneratorTest {

    @Test
    public void shouldHaveCompleteTranslations() {
        Context mockedContext = mock(Context.class);
        when(mockedContext.getString(Matchers.anyInt())).thenReturn("res" );

        AndroidLabelGenerator sut = new AndroidLabelGenerator(mockedContext, "\n");

        for (PhotoPropertiesFormatter.FieldID id : PhotoPropertiesFormatter.FieldID.values()) {
            // will throw if id is undefined
            sut.get(id);
        }
    }
}