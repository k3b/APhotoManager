/*
 * Copyright (c) 2016-2017 by k3b.
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

package de.k3b.tagDB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.k3b.io.ListUtils;

/**
 * Created by k3b on 29.09.2016.
 */

public class TagConverter {
    public static final String TAG_DB_DELIMITER = ";";

    public static String asDbString(String wildcard, List<String> tags) {
        if (tags == null) return null;
        String[] tagsArray = ListUtils.asStringArray(tags);
        if (tagsArray == null) return null;
        if (wildcard == null)
            return asDbString("","", ", ", "", tagsArray);
        return asDbString(wildcard, tagsArray);
    }

    private static String asDbString(String wildcard, String prefix, String seperator, String Suffix, String... tags) {
        StringBuilder result = null;
        if ((tags != null) && (tags.length > 0)) {
            Arrays.sort(tags);
            String nextSeperator = "";
            for (String tag : tags) {
                if ((tag != null) && (tag.length() > 0)) {
                    if (result == null) result = new StringBuilder().append(wildcard);
                    result  .append(nextSeperator)
                            .append(prefix)
                            .append(tag.replace(",", "").replace(" ", ""))
                            .append(Suffix).append(wildcard);
                    nextSeperator = seperator;
                }
            }
        }
        if (result == null) return null;
        return result.toString();
    }

    /** format tags for bat-command "tag1" "tag2" .... */
    public static String asBatString(List<String> tags) {
        String[] tagsArray = ListUtils.asStringArray(tags);

        return asBatString(tagsArray);
    }

    public static String asBatString(String... tagsArray) {
        String tagsString = (tagsArray != null) ? asDbString("", "\"", " ", "\"", tagsArray) : null;

        return tagsString;
    }

    /**
     * @param wildcard
     * @param tags  @return i.e. "%;tag1;%;tag2;%" or ";tag1;;tag2;%"
     * */
    public static String asDbString(String wildcard, String... tags) {
        return asDbString(wildcard,TAG_DB_DELIMITER, "", TAG_DB_DELIMITER, tags);
    }
    public static List<String> fromString(Object tags) {
        if (tags == null) return null;
        ArrayList<String> result = new ArrayList<String>();
        for(String elem : tags.toString().split("[,;:]")) {
            if ((elem != null) && (elem.length() > 0) && ("%".compareTo(elem) != 0)) {
                result.add(elem.trim());
            }
        }
        if (result.size() == 0) return null;
        return result;
    }
}
