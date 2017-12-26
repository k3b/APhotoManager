package de.k3b.io;

import java.util.Enumeration;

/**
 * Created by EVE on 05.12.2017.
 */
public enum VISIBILITY {
    DEFAULT(0),
    PRIVATE(1),
    PUBLIC(2),
    PRIVATE_PUBLIC(3);

    public static final VISIBILITY MAX = PRIVATE_PUBLIC;
    public final int value;

    private VISIBILITY(int value) {
        this.value = value;
    }

    private static VISIBILITY[] values = null;
    public static VISIBILITY fromInt(int i) {
        if(VISIBILITY.values == null) {
            VISIBILITY.values = VISIBILITY.values();
        }
        for (VISIBILITY elem : values) {
            if (elem.value == i) return elem;
        }
        return null;
    }
    public static VISIBILITY fromString(String value) {
        if ((value != null) && (value.length() > 0)) {
            String lower = value.toLowerCase();
            if (lower.startsWith("pr")) return PRIVATE;
            if (lower.startsWith("pu")) return PUBLIC;
            try {
                int i = Integer.parseInt(value, 10);
                return fromInt(i);
            } catch (Exception ex) {
            }
        }
        return null;
    }

    public static boolean isChangingValue(VISIBILITY value) {
        return ((value == PRIVATE) || (value == PUBLIC));
    }
}
