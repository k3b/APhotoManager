package de.k3b.tagDB;

import java.util.Arrays;

/**
 * Created by k3b on 29.09.2016.
 */

public class TagConverter {
    public static final String TAG_DELIMITER = ";";

    /**
     *
     * @param extraDelimiter
     * @param tags
     * @return i.e. "%;tag1;%;tag2;%" or ";tag1;;tag2;"
     */
    public static String tagsAsString(String extraDelimiter, String... tags) {
        StringBuilder result = null;
        if (tags != null) {
            Arrays.sort(tags);
            for (String tag : tags) {
                if ((tag != null) && (tag.length() > 0)) {
                    if (result == null) result = new StringBuilder().append(extraDelimiter);
                    result.append(TAG_DELIMITER)
                            .append(tag.replace(",", "").replace(" ", ""))
                            .append(TAG_DELIMITER).append(extraDelimiter);
                }
            }
        }
        if (result == null) return null;
        return result.toString();
    }
}
