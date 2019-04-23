/*
 * Copyright (c) 2016-2018 by k3b.
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

package de.k3b.io;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import de.k3b.util.IsoDateTimeParser;

/**
 * Created by k3b on 15.10.2016.
 */

public class DateUtil {
    // cannot use Locale.ROOT because it requires api-9. this is api-7
    public static final DateFormat IsoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    public static final DateFormat IsoDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    public static final DateFormat IsoDateFormat2 = new SimpleDateFormat("yyyyMMdd", Locale.US);

    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    static {
        DateUtil.IsoDateTimeFormat.setTimeZone(UTC);
        DateUtil.IsoDateFormat.setTimeZone(UTC);
        DateUtil.IsoDateFormat2.setTimeZone(UTC);
        TimeZone.setDefault(UTC);
    }

    public static Date parseIsoDate(String dateString) {
        if (dateString == null) return null;
        Date result = IsoDateTimeParser.parse(dateString);
        if (result == null) {
            result = parseDateTime(dateString, IsoDateTimeFormat, IsoDateFormat, IsoDateFormat2);
        }
        return result;
    }

    private static Date parseDateTime(String dateString, DateFormat... formatCandidates) {
        Date result = null;
        if (dateString != null) {
            for (DateFormat formatCandidate : formatCandidates) {
                try {
                    result = formatCandidate.parse(dateString);
                    if (result != null) break;
                } catch (ParseException e) {
                }
            }
        }
        return result;
    }

    public static String toIsoDateTimeString(Date date) {
        if (date == null) return null;
        return IsoDateTimeFormat.format(date);
    }

    public static String toIsoDateString(Date date) {
        if (date == null) return null;
        return IsoDateFormat.format(date);
    }
}
