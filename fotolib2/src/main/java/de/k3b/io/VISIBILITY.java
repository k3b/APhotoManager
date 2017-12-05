package de.k3b.io;

/**
 * Created by EVE on 05.12.2017.
 */
public enum VISIBILITY {
    DEFAULT(0),
    PRIVATE(1),
    PUBLIC(2),
    PRIVATE_PUBLIC(3);

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
            try {

                int i = Integer.parseInt(value, 10);
                return fromInt(i);
            } catch (Exception ex) {
            }
        }
        return null;
    }
}
