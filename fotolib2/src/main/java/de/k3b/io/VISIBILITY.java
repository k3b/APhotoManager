package de.k3b.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;

import de.k3b.FotoLibGlobal;

/**
 * Created by EVE on 05.12.2017.
 */
public enum VISIBILITY {
    DEFAULT(0),
    PRIVATE(1),
    PUBLIC(2),
    PRIVATE_PUBLIC(3);

    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);

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
            try {
                int i = Integer.parseInt(value, 10);
                return fromInt(i);
            } catch (Exception ex) {
                if (lower.startsWith("private_p")) return PRIVATE_PUBLIC;
                if (lower.startsWith("pr")) return PRIVATE;
                if (lower.startsWith("pu")) return PUBLIC;
                switch (lower.charAt(0)) {
                    case 'd' :
                        return DEFAULT;
                    case 'f' : // false
                        return PRIVATE;
                    case 't' : // true
                        return PUBLIC;
                    default:
                        break;
                }

            }
            logger.warn(VISIBILITY.class.getSimpleName() + ".fromString " + value + ": unknown value");
        }
        return null;
    }

    public static boolean isChangingValue(VISIBILITY value) {
        return ((value == PRIVATE) || (value == PUBLIC));
    }
}
