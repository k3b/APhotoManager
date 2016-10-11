/*
 * Copyright (c) 2016 by k3b.
 *
 * This file is part of AndroFotoFinder.
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.SimpleFormatter;

import de.k3b.tagDB.TagConverter;
import de.k3b.util.IsoDateTimeParser;

/**
 * Created by k3b on 10.10.2016.
 */

public class MediaUtil {
    private static final DateFormat IsoDateTimeFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ROOT);
    private static final DateFormat IsoDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    static {
        IsoDateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        IsoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static String toString(IMetaApi item) {
        if (item == null) return "";
        return item.getClass().getSimpleName() + ":" +
                " path " + item.getPath() +
                " dateTimeTaken " + toIsoDateString(item.getDateTimeTaken()) +
                " title " + item.getTitle() +
                " description " + item.getDescription() +
                " latitude " + item.getLatitude() +
                " longitude " + item.getLongitude() +
                " tags " + TagConverter.asString("", item.getTags());
    }

    public static void copy(IMetaApi destination, IMetaApi source, boolean allowSetNull, boolean overwriteExisting) {
        String sValue = source.getDescription();
        if (allowed(allowSetNull, sValue, overwriteExisting, destination.getDescription()))
            destination.setDescription(sValue);

        sValue = source.getPath();
        if (allowed(allowSetNull, sValue, overwriteExisting, destination.getPath()))
            destination.setPath(sValue);

        sValue = source.getTitle();
        if (allowed(allowSetNull, sValue, overwriteExisting, destination.getTitle()))
            destination.setTitle(sValue);

        Date dValue = source.getDateTimeTaken();
        if (allowed(allowSetNull, dValue, overwriteExisting, destination.getDateTimeTaken()))
            destination.setDateTimeTaken(dValue);

        Double doValue = source.getLatitude();
        if (allowed(allowSetNull, doValue, overwriteExisting, destination.getLatitude()))
            destination.setLatitude(doValue);

        doValue = source.getLongitude();
        if (allowed(allowSetNull, doValue, overwriteExisting, destination.getLongitude()))
            destination.setLongitude(doValue);

        List<String> tValue = source.getTags();
        if (allowed(allowSetNull, tValue, overwriteExisting, destination.getTags()))
            destination.setTags(tValue);
    }

    private static boolean allowed(boolean allowSetNull, Object newValue,
                                   boolean overwriteExisting, Object oldValue) {
        if ((!overwriteExisting) && (oldValue != null)) return false;
        return ((newValue != null) || allowSetNull);
    }

    public static Date parseIsoDate(String dateString) {
        Date result = IsoDateTimeParser.parse(dateString);
        if (result == null) {
            try {
                result = IsoDateFormat.parse(dateString);
            } catch (ParseException e) {
            }
        }
        return result;
    }

    public static String toIsoDateString(Date date) {
        if (date == null) return "";
        return IsoDateTimeFormat.format(date);
    }
}