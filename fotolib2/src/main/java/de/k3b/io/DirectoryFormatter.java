package de.k3b.io;

import java.awt.Rectangle;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Created by k3b on 12.07.2015.
 */
public class DirectoryFormatter {
    public static void getDates(String selectedAbsolutePath, Date from, Date to) {
        Integer year = null;
        Integer month = null;
        Integer day = null;

        String parts[] = selectedAbsolutePath.split(Directory.PATH_DELIMITER);

        for (String part : parts) {
            if ((part != null) && ((part.length() > 0))) {
                try {
                    Integer value = Integer.parseInt(part);
                    if (year == null) year = value;
                    else if (month == null) month = value;
                    else if (day == null) day = value;
                } catch (NumberFormatException ex) {

                }
            }
        }

        if (year != null) {
            int yearFrom = year.intValue();

            if (yearFrom == 1970) {
                from.setTime(0);
                to.setTime(0);
            } else {
                int monthFrom = (month != null) ? month.intValue() : 1;
                int dayFrom = (day != null) ? day.intValue() : 1;

                GregorianCalendar _from = new GregorianCalendar(yearFrom, monthFrom - 1, dayFrom, 0, 0, 0);
                _from.setTimeInMillis(_from.getTimeInMillis());
                int field = GregorianCalendar.YEAR;
                if (month != null) field = GregorianCalendar.MONTH;
                if (day != null) field = GregorianCalendar.DAY_OF_MONTH;

                GregorianCalendar _to = new GregorianCalendar();
                _to.setTimeInMillis(_from.getTimeInMillis());
                _to.add(field, 1);
                to.setTime(_to.getTimeInMillis());
                from.setTime(_from.getTimeInMillis());
            }
        }
    }

    private static DecimalFormat latLonFormatter = new DecimalFormat("#.0000", new DecimalFormatSymbols(Locale.US));

    public static String getLatLonPath(double latitude, double longitude) {
        if ((latitude == 0) && (longitude == 0)) return null;
        StringBuilder result = new StringBuilder();
        result.append("/ ").append(getInt(latitude, 10)).append(",").append(getInt(longitude,10)).append("/");
        result.append((int) latitude).append(",").append((int)longitude).append("/");

        String lat = latLonFormatter.format(latitude); int latPos = lat.indexOf(".") + 1;
        String lon = latLonFormatter.format(longitude); int lonPos = lon.indexOf(".")+ 1;

        for (int i=1;i <=3;i++) {
            result.append(lat.substring(0,latPos + i)).append(",").append(lon.substring(0,lonPos + i)).append("/");
        }

        return result.toString();
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

    public static GeoRectangle getLatLon(String path) {
        String[] elements = getLastPath(path).split(",");
        String lat = elements[0];
        String lon = elements[1];

        GeoRectangle result = new GeoRectangle();
        result.setLatitudeMin(Double.parseDouble(lat));
        result.setLogituedMin(Double.parseDouble(lon));

        double delta = 1;
        if (lat.startsWith(" ")) {
            delta = 10.0;
        } else {
            int posDecimal = lat.indexOf(".");
            if (posDecimal >= 0) {
                int numberDecimals = lat.length() - posDecimal -1;
                while (numberDecimals > 0) {
                    delta = delta / 10.0;
                    numberDecimals--;
                }
            }
        }

        result.setLatitudeMax(result.getLatitudeMin() + delta);
        result.setLogituedMax(result.getLogituedMin() + delta);

        return result;
    }
}
