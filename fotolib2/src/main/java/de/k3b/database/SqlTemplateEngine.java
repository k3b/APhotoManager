/*
 * Copyright (c) 2017 by k3b.
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

package de.k3b.database;

/**
 * Translates strings with jsp like macros to sqlLite string expressions.
 * "hello ${expression}"
 *
 * Created by k3b on 12.06.2017.
 */

public class SqlTemplateEngine {

    private static final String MACRO_START = "${";
    private static final String MACRO_END = "}";

    public static String toSql(String input) {
        if (input == null) return null;

        int lastStart = 0;
        int length = input.length();
        StringBuilder result = new StringBuilder();
        int startMacro;

        while (lastStart < length) {
            startMacro = input.indexOf(MACRO_START, lastStart);
            if (startMacro >= lastStart) {
                addConst(result, input, lastStart, startMacro);
                int endMacro = input.indexOf(MACRO_END, startMacro);
                if (endMacro > startMacro) {
                    addExpression(result, input, startMacro + MACRO_START.length(), endMacro);
                    lastStart = endMacro + MACRO_END.length();
                } else {
                    // start without end
                    addConst(result, input, startMacro, length);
                    lastStart = length; // terminate loop
                }
            } else {
                if (result.length() == 0) {
                    // special case no macros included
                    result.append(input);
                } else {
                    addConst(result, input, lastStart, length);
                }
                lastStart = length; // terminate loop
            }

        }

        return result.toString();
    }

    private static void addExpression(StringBuilder result, String input, int start, int end) {
        int endExpression = end;
        while ((endExpression > start) && (input.charAt(endExpression - 1) == '|'))  {
            endExpression--;
        }

        if (endExpression > start) {
            // expression exist
            if (result.length() > 0) result.append("||");

            result
                    .append("ifnull(")
                    .append(input.substring(start,endExpression));
            addConst(result, input, endExpression, end);
            result
                    .append(",'')");
        }

    }

    private static void addConst(StringBuilder result, String input, int start, int end) {
        if ((start >= 0) && (end > start) && (end <= input.length())) {
            if (result.length() > 0) result.append("||");
            result
                .append("'")
                .append(input.substring(start,end).replace('\'','\"'))
                .append("'");
        }
    }
}
