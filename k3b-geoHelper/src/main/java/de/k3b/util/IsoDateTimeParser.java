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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to parse iso 8601 dateTime.<br/>
 *
 * Created by k3b on 12.02.2015.
 */
public class IsoDateTimeParser {
    // "(?:" start of non capturing group
    public static final Pattern ISO8601_FRACTIONAL_PATTERN
            = Pattern.compile("((\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})(?:[\\.,](\\d{1,3}))?(Z|[\\+-]\\d{2}(?::?\\d{2})?Z?)?)");
    //                            year     month     day T  hour    minute    sec             millisec   Z or +/-  hours  :   minutes

    private static final int YEAR = 0;
    private static final int MONTH = 1;
    private static final int DAY = 2;
    private static final int HOUR = 3;
    private static final int MINUTE = 4;
    private static final int SECOND = 5;
    private static final int FRACTIONAL_SECONDS = 6;
    private static final int TIMEZONE = 7;

    /**
     * Similar to
     * SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse()
     * where SSS (millisecs) and Z(timezone in hours i.e. "-01:00") are optional.
     *
     * returns null if not a valid date */
    public static Date parse(String dateString) {
        if (dateString != null) {
            final Matcher matcher = ISO8601_FRACTIONAL_PATTERN.matcher(dateString);
            if (matcher.matches()) {
                try {
                    // +2: matcher.group(0) returns the whole expression, matcher.group(1) is overall "()"
                    // matchingGroup starts with 2
                    return toDate(matcher.group(YEAR + 2),
                            matcher.group(MONTH + 2),
                            matcher.group(DAY + 2),
                            matcher.group(HOUR + 2),
                            matcher.group(MINUTE + 2),
                            matcher.group(SECOND + 2),
                            matcher.group(FRACTIONAL_SECONDS + 2),
                            matcher.group(TIMEZONE + 2));
                } catch (NumberFormatException nfe) {
                }
            }
        }
        return null;
    }

    /**
     * Convert the given timestamp parameters into a number of milliseconds
     * @param dateFragments year month day hour minute seconds fractionsOfASecond timezone
     * @return number of milliseconds
     */
    private static Date toDate(String... dateFragments)
    {
        return toDate(get(dateFragments, YEAR, -1),
                get(dateFragments, MONTH, -1),
                get(dateFragments, DAY, -1),
                get(dateFragments, HOUR, -1),
                get(dateFragments, MINUTE, -1),
                get(dateFragments, SECOND, -1),
                getFraction(dateFragments),
                getTimeZone(dateFragments));

    }

    public static Date toDate(int year, int month, int day, int hour, int minute, int second, int millisec, TimeZone timeZone) {
        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);

        cal.set(Calendar.MILLISECOND, millisec);
        cal.setTimeZone(timeZone);
        return new Date(cal.getTimeInMillis());
    }

    private static int getFraction(String[] dateFragments) {
        String inFraction = get(dateFragments, FRACTIONAL_SECONDS, null);
        int millis = 0;
        if (inFraction != null)
        {
            try {
                millis = Integer.parseInt((inFraction+"000").substring(3));
            }
            catch (NumberFormatException nfe) {} // ignore errors, millis stay at 0
        }
        return millis;
    }

    /** returns values[paramNo] or notFoundValue if result is empty or if values is too short */
    private static String get(String[] values, int paramNo, String notFoundValue) {
        if (paramNo >= values.length) return notFoundValue;
        String strValue = values[paramNo];
        return ((strValue != null) && (strValue.length() > 0)) ? strValue : null;
    }

    /** returns int of values[paramNo] or notFoundValue if result is empty or if values is too short */
    private static int get(String[] values, int paramNo, int notFoundValue) throws NumberFormatException {
        String strValue = get(values, paramNo, null);
        if (strValue != null) {
            return Integer.parseInt(strValue);
        }
        return notFoundValue;
    }

    private static TimeZone getTimeZone(String[] values) {
        String inTimezone = get(values, TIMEZONE, null);
        if (inTimezone == null) {
            // No timezone : use local
            return TimeZone.getDefault();
        } else if (inTimezone.equals("Z")) {
            // timezone "Z" : use zulu
            return TimeZone.getTimeZone("GMT");
        } else {
            // Timezone specified, pass to calendar
            return TimeZone.getTimeZone("GMT" + inTimezone);
        }

    }
}
