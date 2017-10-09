package de.k3b.transactionlog;

import java.util.HashMap;

/**
 * Created by EVE on 26.02.2017.
 */

public enum MediaTransactionLogEntryType {
// file operations affects path
    DELETE("F-", "apmDelete"),
    MOVE("Fm", "apmMove"),
    COPY("F+", "apmCopy"),
// IMetaApi
    GPS("g", "apmGps"),
    TAGSADD("T+", "apmTagsAdd"),
    TAGSREMOVE("T-", "apmTagsRemove"),
    TAGS("T", ""),
    DESCRIPTION("d", "apmDescription"),
    HEADER("h", "apmTitle"),
    RATING("r", "apmRating"),
    DATE("dm", "apmDateTimeOriginal");

// implementaion
    private final String id;
    private final String batCommand;

    public String getId() {return id;}

    private static HashMap<String,MediaTransactionLogEntryType> ids = null;
    MediaTransactionLogEntryType(String id, String batCommand) {
        this.id = id;
        this.batCommand = batCommand;
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

    public Object[] getCommand(String path, String parameter, boolean quoteParam) {
        Object r[] = new Object[8];
        int i =0;

        if ((batCommand == null) || (batCommand.length() == 0)) throw new IllegalArgumentException(this +":"+id + " has no batCommand assigned");
        r[i++] = "call ";
        r[i++] = batCommand;
        r[i++] = ".cmd \"";
        r[i++] = path;
        r[i++] = "\" ";
        r[i++] = quoteParam ? "\"" : "";
        r[i++] = parameter;
        r[i++] = quoteParam ? "\"" : "";
        return r;
    }
}
