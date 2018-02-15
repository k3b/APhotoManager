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
package de.k3b.io;

/**
 * Created by k3b on 29.05.2017.
 */

public class StringUtils {

    public static int compare(String lhs, String rhs) {
        if (lhs != null) return (rhs != null) ? lhs.compareTo(rhs) : +1;
        return (rhs == null) ? 0 : -1;
    }

    /** null save function: return if both are null or both non-null are the same */
    public static boolean equals(Object lhs, Object rhs) {
        if (lhs != null) return (rhs != null) ? lhs.equals(rhs) : false;
        return (rhs == null);
    }

    public static int getTagStart(CharSequence s, int start) {
        if (start <= s.length()) {
            int i = start -1;
            while ((i>=0) && (isTagChar(s.charAt(i)))) i--;
            if ((i < 0) || (s.charAt(i) != '#')) return -1;  // begin of string  or not starting with '#'
            if ((i == 0) || isTagDelimiterChar(s.charAt(i - 1))) return i; // blank before hash or start of string
        }
        return -1;
    }
    public static int getTagEnd(CharSequence s, int start) {
        int len = s.length();
        int i = start;
        while ((i<len) && (isTagChar(s.charAt(i)))) i++;
        if ((i >= len) || isTagDelimiterChar(s.charAt(i))) return i; // blank after end of string
        return -1;
    }

    private static boolean isTagChar(char c) {
        return Character.isJavaIdentifierPart(c) || (c == '-');
    }
    private static boolean isTagDelimiterChar(char c) {
        return Character.isSpaceChar(c) || (",;(){}".indexOf(c) >= 0);
    }
    /** words surrounded by blank and starting with '#' */
    public static CharSequence getTag(CharSequence s, int start) {
        int tagStart = getTagStart(s,start);
        int tagEnd = getTagEnd(s,start);
        if ((tagStart >= 0) && (tagEnd > tagStart)) {
            return s.subSequence(tagStart,tagEnd);
        }
        return null;
    }

    public static int length(CharSequence str) {
        return (str != null) ? str.length() : 0;
    }

    public static boolean isNullOrEmpty(CharSequence str) {
        return (0 == length(str));
    }

    public static String merge(String lhs, String rhs) {
        int lhsLength = lhs.length();
        int rhsLength = rhs.length();
        int len = Math.max(lhsLength,rhsLength);

        StringBuilder result = new StringBuilder(lhsLength + rhsLength);
        for(int i = 0; i < len; i++) {
            if (i < lhsLength) result.append(lhs.charAt(i));
            if (i < rhsLength) result.append(rhs.charAt(i));
        }

        return result.toString();
    }
}
