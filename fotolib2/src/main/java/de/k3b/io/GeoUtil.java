package de.k3b.io;

/**
 * Created by k3b on 19.10.2016.
 */

public class GeoUtil {
    public static float parse(String degreeString, String plusMinus) {
        boolean isNegativ = false;
        float result = 0;
        int factor = 1;
        try {
            // degree, hours, minutes, seconds
            String [] parts = degreeString.toUpperCase().split("[:,; '\"°]");

            for (String part : parts) {
                if ((part != null) && (part.length() > 0)) {
                    StringBuilder numberPart = new StringBuilder(part);
                    if (isNegative(numberPart, part, plusMinus)) {
                        isNegativ = true;
                    }
                    if (numberPart.length() > 0) {
                        float doubleValue = Float.parseFloat(numberPart.toString());
                        result += (doubleValue / factor);
                        factor *= 60;
                    }
                }

            }

        } catch (NumberFormatException e) {
            // Some of the nubmers are not valid
            throw new IllegalArgumentException(degreeString, e);
        }

        if (isNegativ) return -result;

        return result;
    }

    private static boolean isNegative(StringBuilder doubleString, String value, String plusMinus) {
        boolean result = false;
        int plusMinusOffset = getPlusMinusOffset(value, plusMinus);
        if (plusMinusOffset >= 0) {
            if (plusMinus.charAt(1) == doubleString.charAt(plusMinusOffset)) {
                result = true;
            }
            doubleString.deleteCharAt(plusMinusOffset);
        }
        return result;
    }

    private static int getPlusMinusOffset(String value, String plusMinus) {
        for (char c : plusMinus.toCharArray()) {
            int offset = value.indexOf(c);
            if (offset >= 0) return offset;
        }
        return -1;
    }
}
        