/*
 * Copyright (c) 2018 by k3b.
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
package de.k3b.translations;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Created by EVE on 15.01.2018.
 */

public class TranslationStatisticsTests {
    @Test
    public void shouldMatchFastlane() {
        Assert.assertEquals(true, TranslationStatistics.getFastlanePattern("en-US").matcher("en-US").matches());
    }

    @Test
    public void shouldMatchString_de() {
        Assert.assertEquals(true, TranslationStatistics.PATTERN_ANDROID_RES_STRING_LOCALE.matcher("values-de").matches());
    }

    @Test
    public void dumpAsMD() {
        final TranslationStatistics translationStatistics = new TranslationStatistics();
        System.out.println("<!-- generated with TranslationStatisticsTests#dumpAsMD -->\n" +
                translationStatistics.formatterMarkdown.toString(translationStatistics.getLocaleInfos(), translationStatistics.english));
    }

    @Test
    public void dupmpAsIni() {
        final TranslationStatistics translationStatistics = new TranslationStatistics();
        System.out.println("# generated with TranslationStatisticsTests#dupmpAsIni\n" +
                translationStatistics.formatterIni.toString(translationStatistics.getLocaleInfos(), translationStatistics.english));
    }


}
