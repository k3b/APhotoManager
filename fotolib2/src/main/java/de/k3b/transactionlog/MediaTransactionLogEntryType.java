package de.k3b.transactionlog;

import java.util.HashMap;

/**
 * Created by EVE on 26.02.2017.
 */

public enum MediaTransactionLogEntryType {
    DELETE("F-"),
    MOVE("Fm"),
    COPY("F+"),
    GPS("g"),
    TAGSADD("T+"),
    TAGSREMOVE("T-"),
    TAGS("T"),
    DESCRIPTION("d"),
    HEADER("h"),;

    private final String id;

    public String getId() {return id;}

    private static HashMap<String,MediaTransactionLogEntryType> ids = null;
    MediaTransactionLogEntryType(String id) {
        this.id = id;
    }

    public static MediaTransactionLogEntryType get(String id) {
        if (ids == null) {
            ids = new HashMap<String, MediaTransactionLogEntryType>();
            for (MediaTransactionLogEntryType e : MediaTransactionLogEntryType.values()) {
                ids.put(e.toString(), e);
                ids.put(e.id, e);
            }
        }
        MediaTransactionLogEntryType result = ids.get(id);
        if (result == null) throw new IllegalArgumentException("MediaTransactionLogEntryType.get('" +
                id + "')");
        return result;
    }
}
