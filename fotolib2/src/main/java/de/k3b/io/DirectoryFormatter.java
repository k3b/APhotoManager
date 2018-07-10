/*
 * Copyright (c) 2015-2018 by k3b.
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
 
package de.k3b.io;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import de.k3b.FotoLibGlobal;

/**
 * Created by k3b on 12.07.2015.
 */
public class DirectoryFormatter {
    private static final int INTDECADE_LEN = 3;

    /**
     * "/2001/01/16" => 2001-01-16 - 2001-01-17
     * "/2001/01/" => 2001-01-01 - 2001-02-01
     * "/2001/" => 2001-01-01 - 2002-01-01
     */
    public static void getDates(String selectedAbsolutePath, Date from, Date to) {
        Integer year = null;
        Integer month = null;
        Integer day = null;
        Integer decade = (FotoLibGlobal.datePickerUseDecade) ? null : Integer.MIN_VALUE;

        String parts[] = selectedAbsolutePath.split(Directory.PATH_DELIMITER);

        for (String part : parts) {
            if ((part != null) && ((part.length() > 0))) {
                try {
                    if (decade == null) {
                        part = part.substring(0,part.length() - 1); // remove trailing "*"
                    }
                    Integer value = Integer.parseInt(part);
                    if (decade == null) decade = value;
                    else if (year == null) year = value;
                    else if (month == null) month = value;
                    else if (day == null) day = value;
                } catch (NumberFormatException ex) {

                }
            }
        }

        int yearFrom = 0;
        if ((FotoLibGlobal.datePickerUseDecade) && (decade != null)) yearFrom = decade.intValue();
        if (year != null) yearFrom = year.intValue();

        if (yearFrom != 0) {
            if ((year != null) && (yearFrom == 1970)) {
                from.setTime(0);
                to.setTime(0);
            } else {
                int monthFrom = (month != null) ? month.intValue() : 1;
                int dayFrom = (day != null) ? day.intValue() : 1;

                GregorianCalendar cal = new GregorianCalendar(yearFrom, monthFrom - 1, dayFrom, 0, 0, 0);
                from.setTime(cal.getTimeInMillis());

                int field = GregorianCalendar.YEAR;
                int increment = 10;
                if (year != null) increment = 1;
                if (month != null) field = GregorianCalendar.MONTH;
                if (day != null) field = GregorianCalendar.DAY_OF_MONTH;

                cal.add(field, increment);
                to.setTime(cal.getTimeInMillis());
            }
        }
    }

    private static DecimalFormat latLonFormatter6 = new DecimalFormat("0.000000", new DecimalFormatSymbols(Locale.US));

    public static String getLatLonPath(double latitude, double longitude) {
        if ((latitude == 0) && (longitude == 0)) return null;
        StringBuilder result = new StringBuilder();
        result.append("/ ").append(getInt(latitude, 10)).append(",").append(getInt(longitude, 10)).append("/");
        result.append((int) latitude).append(",").append((int) longitude).append("/");

        String lat = latLonFormatter6.format(latitude);
        int latPos = lat.indexOf(".") + 1;
        String lon = latLonFormatter6.format(longitude);
        int lonPos = lon.indexOf(".") + 1;

        for (int i = 1; i <= 2; i++) {
            result.append(lat.substring(0, latPos + i)).append(",").append(lon.substring(0, lonPos + i)).append("/");
        }

        return result.toString();
    }

    public static String formatLatLon(Double latOrLon) {
        if (latOrLon == null) return "0";
        return formatLatLon(latOrLon.doubleValue());
    }

    public static CharSequence formatLatLon(Double... latOrLons) {
        StringBuilder result = new StringBuilder();
        for (Double latOrLon : latOrLons)
            result.append(formatLatLon(latOrLon)).append(" ");
        return result;
    }

    public static String formatLatLon(double latOrLon) {
        if ((latOrLon <= 0.0000005) && (latOrLon >= -0.0000005)) return "0";
        return latLonFormatter6.format(latOrLon);
    }

    private static int getInt(double ll, int factor) {
        return ((int) (ll / factor)) * factor;
    }

    // package to allow unit testing
    public static String getLastPath(String path) {
        if (path != null) {
            String[] elements = path.split("/");
            for (int i = elements.length - 1; i >= 0; i--) {
                String candidate = elements[i];
                if ((candidate != null) && (candidate.length() > 0)) return candidate;
            }
        }
        return null;
    }

    /**
     * Format "lat,lon" or "lat,lon-lat,lon"
     */
    public static IGeoRectangle parseLatLon(String path) {
        String lastPath = getLastPath(path);
        if (lastPath != null) {
            String[] minMax = lastPath.split(GeoRectangle.DELIM_FIELD);
            if ((minMax == null) || (minMax.length == 0)) return null;

            String[] elements = minMax[0].split(GeoRectangle.DELIM_SUB_FIELD);
            if ((elements != null) && (elements.length == 2)) {
                String lat = elements[0];
                String lon = elements[1];

                GeoRectangle result = new GeoRectangle();
                result.setLatitudeMin(getLatLon(lat));
                result.setLogituedMin(getLatLon(lon));

                if (minMax.length == 1) {
                    double delta = 1;
                    if (lat.startsWith(" ")) {
                        delta = 10.0;
                    } else {
                        int posDecimal = lat.indexOf(".");
                        if (posDecimal >= 0) {
                            int numberDecimals = lat.length() - posDecimal - 1;
                            while (numberDecimals > 0) {
                                delta = delta / 10.0;
                                numberDecimals--;
                            }
                        }
                    }

                    result.setLatitudeMax(result.getLatitudeMin() + delta);
                    result.setLogituedMax(result.getLogituedMin() + delta);
                    return result;
                } else if (minMax.length == 2) {
                    elements = minMax[1].split(GeoRectangle.DELIM_SUB_FIELD);
                    if ((elements != null) && (elements.length == 2)) {
                        lat = elements[0];
                        lon = elements[1];

                        result.setLatitudeMax(getLatLon(lat));
                        result.setLogituedMax(getLatLon(lon));
                        return result;
                    } // if lat+lon
                } // if 1 or 2 lat,lon elements
            }
        }
        return null;
    }

    private static double getLatLon(String latOrLon) {
        if ((latOrLon == null) || (latOrLon.length() == 0)) return Double.NaN;
        return Double.parseDouble(latOrLon);
    }

    public static String getDecade(String stringWithYear, int posOfYear) {
        if ((stringWithYear == null) || (stringWithYear.length() - posOfYear < INTDECADE_LEN)) {
            return null;
        }
        return stringWithYear.substring(posOfYear, posOfYear + INTDECADE_LEN)  + "0*";
    }

    public static String getDatePath(final boolean withDecade, long dateFrom, long dateTo) {
        // special cases if null, empty or only one value
        if (dateFrom == 0) dateFrom = dateTo;
        if (dateTo == 0) dateTo = dateFrom;
        if (dateTo == 0) return null;

        final long diffTage = Math.abs (dateTo - dateFrom) / (1000 * 60 * 60 * 24);

        final Calendar date = Calendar.getInstance(); // TimeZone.getTimeZone("UTC"));
        date.setTimeInMillis(dateFrom);
        final int year = date.get(Calendar.YEAR);

        final StringBuilder result = new StringBuilder();
        if ((withDecade) && (diffTage < 3800)) {
            result.append("/").append(year / 10).append("0*");
        }
        if (diffTage < 380) {
            result.append("/").append(year);
        }
        if (diffTage < 40) {
            final int month = date.get(Calendar.MONTH) + 1;
            result.append("/").append(n2(month) );
        }
        if (diffTage <= 2) {
            final int day = date.get(Calendar.DAY_OF_MONTH);
            result.append("/").append(n2(day));
        }
        if (result.length() == 0) return null;
        return result.toString();
    }

    private static int getYear(Date dateTo) {
        int year = dateTo.getYear();
        if ((year >= 0) && (year < 1000)) year += 1900;
        return year;
    }


    private static String n2(int i) {
        if (i < 10) return "0"+i;

        return ""+i;
    }
}
