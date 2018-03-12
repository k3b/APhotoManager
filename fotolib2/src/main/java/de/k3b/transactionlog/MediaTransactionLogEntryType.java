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

package de.k3b.transactionlog;

import java.util.HashMap;

/**
 * Defines all possible media operations.
 *
 * Created by k3b on 26.02.2017.
 */

public enum MediaTransactionLogEntryType {
// file operations affects path
    DELETE("F-", "apmDelete"),
    MOVE("Fm", "apmMove",true),
    COPY("F+", "apmCopy",true),
// IMetaApi
    GPS("g", "apmGps"),
    TAGSADD("T+", "apmTagsAdd"),
    TAGSREMOVE("T-", "apmTagsRemove"),
    TAGS("T", "rem apmTagsSet"),
    DESCRIPTION("d", "apmDescription",true),
    HEADER("h", "apmTitle",true),
    RATING("r", "apmRating"),
    VISIBILITY("s","rem apmSecurity"),
    DATE("dm", "apmDateTimeOriginal"),
    COMMENT(null, "rem"),
// dir operatins affects path
    MOVE_DIR("Dm", "apmMoveDir",true),
    ;

// implementaion
    private final String id;
    private final String batCommand;
    private final boolean mustQuoteParam;

    public String getId() {return id;}

    private static HashMap<String,MediaTransactionLogEntryType> ids = null;

    MediaTransactionLogEntryType(String id, String batCommand) {
        this(id,batCommand, false);
    }

    MediaTransactionLogEntryType(String id, String batCommand, boolean mustQuoteParam) {
        this.mustQuoteParam = mustQuoteParam;
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

    public Object[] getCommand(String path, String parameter) {
        Object r[] = new Object[10];
        int i =0;

        if ((batCommand == null) || (batCommand.length() == 0)) throw new IllegalArgumentException(this +":"+id + " has no batCommand assigned");

        if (!isComment(this)) {
            r[i++] = "call ";
        }
        r[i++] = batCommand;
        if (!isComment(this)) {
            r[i++] = ".cmd";
        }

        r[i++] = " \"";
        r[i++] = path;
        r[i++] = "\" ";
        r[i++] = mustQuoteParam ? "\"" : "";
        r[i++] = parameter;
        r[i++] = mustQuoteParam ? "\"" : "";
        return r;
    }

    public static boolean isComment(MediaTransactionLogEntryType item) {
        return ((item == null) || isComment(item.batCommand));
    }

    private static boolean isComment(String batCommand) {
        return ((batCommand == null)
                || (batCommand.toLowerCase().startsWith(COMMENT.batCommand.toLowerCase())));
    }
}
