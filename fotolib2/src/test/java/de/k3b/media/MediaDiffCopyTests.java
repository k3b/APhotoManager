/*
 * Copyright (c) 2017-2018 by k3b.
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

package de.k3b.media;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import de.k3b.TestUtil;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.io.DateUtil;
import de.k3b.io.ListUtils;
import de.k3b.io.VISIBILITY;

/**
 * Created by k3b on 07.07.2017.
 */

public class MediaDiffCopyTests {
    @Test
    public void shouldHandleNoChanges() {
        IMetaApi initialData = TestUtil.createTestMediaDTO(1);
        IMetaApi modifiedData = TestUtil.createTestMediaDTO(1);
        MediaDiffCopy sut = new MediaDiffCopy(true, true).setDiff(initialData, modifiedData);

        Assert.assertEquals("null means no changes: " + sut, null, sut);
    }

    // #91: Fix Photo without geo may have different representations values
    @Test
    public void shouldHandleGpsNanNullNoValue() {
        IMetaApi initialData = TestUtil.createTestMediaDTO(1).setLatitudeLongitude(Double.NaN, Double.NaN);
        IMetaApi modifiedData = TestUtil.createTestMediaDTO(1).setLatitudeLongitude(null, IGeoPointInfo.NO_LAT_LON);
        MediaDiffCopy sut = new MediaDiffCopy(true, true).setDiff(initialData, modifiedData);

        Assert.assertEquals("null means no changes: " + sut, null, sut);
    }

    @Test
    public void shouldOverwriteNullDate() {
        IMetaApi initialData = TestUtil.createTestMediaDTO(1).setDateTimeTaken(null);
        IMetaApi modifiedData = TestUtil.createTestMediaDTO(1);
        MediaDiffCopy sut = new MediaDiffCopy(true, true).setDiff(initialData, modifiedData);

        IMetaApi destintaion = createMediaDto().setDateTimeTaken(new Date());
        int numberofChanges = sut.applyChanges(destintaion).size();
        Assert.assertEquals("#changed date" + sut, modifiedData.getDateTimeTaken(), destintaion.getDateTimeTaken());
        Assert.assertEquals("#changes " + sut, 1, numberofChanges);
    }

    @Test
    public void shouldSetDateNull() {
        IMetaApi initialData = TestUtil.createTestMediaDTO(1);
        IMetaApi modifiedData = TestUtil.createTestMediaDTO(1).setDateTimeTaken(null);
        MediaDiffCopy sut = new MediaDiffCopy(true, true).setDiff(initialData, modifiedData);

        IMetaApi destintaion = createMediaDto().setDateTimeTaken(new Date());
        int numberofChanges = sut.applyChanges(destintaion).size();
        Assert.assertEquals("#changed date" + sut, null, destintaion.getDateTimeTaken());
        Assert.assertEquals("#changes " + sut, 1, numberofChanges);
    }


    @Test
    public void shouldShiftDate() {
        // the examples from documentation
        IMetaApi initialData  = TestUtil.createTestMediaDTO(1).setDateTimeTaken(DateUtil.parseIsoDate("2001-01-01T04:11:52"));
        IMetaApi modifiedData = TestUtil.createTestMediaDTO(1).setDateTimeTaken(DateUtil.parseIsoDate("2017-07-03T14:22:52"));
        // Added 16 years 6 months 2 days 10 hours and 11 minutes.
        MediaDiffCopy sut = new MediaDiffCopy(true, true).setDiff(initialData, modifiedData);

        // example date1
        IMetaApi destintaion = createMediaDto().setDateTimeTaken(DateUtil.parseIsoDate("2001-01-03T08:06:05:52"));
        int numberofChanges = sut.applyChanges(destintaion).size();
        Assert.assertEquals("#changed date1" + sut, "2017-07-05T18:17:05", DateUtil.toIsoDateTimeString(destintaion.getDateTimeTaken()));
        Assert.assertEquals("#changes1 " + sut, 1, numberofChanges);

        // example date2
        destintaion = createMediaDto().setDateTimeTaken(DateUtil.parseIsoDate("2001-01-04T09:12:08"));
        numberofChanges = sut.applyChanges(destintaion).size();
        Assert.assertEquals("#changed date2" + sut, "2017-07-06T19:23:08", DateUtil.toIsoDateTimeString(destintaion.getDateTimeTaken()));
        Assert.assertEquals("#changes2 " + sut, 1, numberofChanges);

        // special use case overwrite null in shift mode
        destintaion = createMediaDto().setDateTimeTaken(null);
        numberofChanges = sut.applyChanges(destintaion).size();
        Assert.assertEquals("#changed verwrite null in shift mode" + sut, "2017-07-03T14:22:52", DateUtil.toIsoDateTimeString(destintaion.getDateTimeTaken()));
        Assert.assertEquals("#changes3 " + sut, 1, numberofChanges);

    }

    @Test
    public void shouldApplyTagDelta() {
        String added = "newTag";
        String removed = "oldTag";

        IMetaApi initialData  = TestUtil.createTestMediaDTO(1); initialData.getTags().add(removed);
        IMetaApi modifiedData = TestUtil.createTestMediaDTO(1); modifiedData.getTags().add(added);
        MediaDiffCopy sut = new MediaDiffCopy(true, true).setDiff(initialData, modifiedData);

        IMetaApi destintaion = createMediaDto();
        List<String> tagsUnderTest = TestUtil.createTestMediaDTO(2).getTags();
        tagsUnderTest.add(removed);
        destintaion.setTags(tagsUnderTest);

        int numberofChanges = sut.applyChanges(destintaion).size();
        tagsUnderTest = destintaion.getTags();

        Assert.assertEquals("#added tags " + ListUtils.toString(tagsUnderTest) + "-" + sut, 3, tagsUnderTest.size());
        Assert.assertEquals("#changes " + sut, 1, numberofChanges);
    }

    @Test
    public void shouldApplyTitleDelta() {
        IMetaApi initialData  = TestUtil.createTestMediaDTO(1).setTitle("Hello world");
        IMetaApi modifiedData = TestUtil.createTestMediaDTO(1).setTitle("+ appended");
        MediaDiffCopy sut = new MediaDiffCopy(true, true).setDiff(initialData, modifiedData);

        IMetaApi destintaion = createMediaDto().setTitle("some title");
        int numberofChanges = sut.applyChanges(destintaion).size();

        Assert.assertEquals("#added title missing " + sut, "some title appended", destintaion.getTitle());
        Assert.assertEquals("#changes1 " + sut, 1, numberofChanges);

        destintaion = createMediaDto().setTitle(null);
        numberofChanges = sut.applyChanges(destintaion).size();

        Assert.assertEquals("#added title null " + sut, "appended", destintaion.getTitle());
        Assert.assertEquals("#changes2 " + sut, 1, numberofChanges);

        destintaion = createMediaDto().setTitle("already-appended");
        List<MediaUtil.FieldID> changes = sut.applyChanges(destintaion);

        Assert.assertEquals("#added title already appended " + sut, "already-appended", destintaion.getTitle());
        Assert.assertNull("#changes3 " + sut, changes);
    }

    @Test
    public void shouldApplyDescriptionDelta() {
        IMetaApi initialData  = TestUtil.createTestMediaDTO(1).setDescription("Hello world");
        IMetaApi modifiedData = TestUtil.createTestMediaDTO(1).setDescription("+ appended");
        MediaDiffCopy sut = new MediaDiffCopy(true, true).setDiff(initialData, modifiedData);

        IMetaApi destintaion = createMediaDto().setDescription("some Description");
        int numberofChanges = sut.applyChanges(destintaion).size();

        Assert.assertEquals("#added Description " + sut, "some Description appended", destintaion.getDescription());
        Assert.assertEquals("#changes1 " + sut, 1, numberofChanges);
    }

    @Test
    public void shouldToString() {
        IMetaApi initialData = TestUtil.createTestMediaDTO(1);
        IMetaApi modifiedData = TestUtil.createTestMediaDTO(2);
        MediaDiffCopy sut = new MediaDiffCopy(true, true).setDiff(initialData, modifiedData);

        Assert.assertNotNull(sut.toString(), sut.toString());
    }

    private static IMetaApi createMediaDto() {
        return new MediaDTO().setVisibility(VISIBILITY.PUBLIC);
    }
}
