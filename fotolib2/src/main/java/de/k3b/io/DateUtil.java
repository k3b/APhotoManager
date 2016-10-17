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
    public static final DateFormat IsoDateTimeFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ROOT);
    public static final DateFormat IsoDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    public static final DateFormat IsoDateFormat2 = new SimpleDateFormat("yyyyMMdd", Locale.ROOT);

    static {
        DateUtil.IsoDateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        DateUtil.IsoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
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
        for (DateFormat formatCandidate : formatCandidates) {
            try {
                result = formatCandidate.parse(dateString);
                if (result != null) break;
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
