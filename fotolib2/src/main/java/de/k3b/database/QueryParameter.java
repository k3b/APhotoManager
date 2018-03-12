/*
 * Copyright (c) 2015-2017 by k3b.
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

import java.util.ArrayList;
import java.util.List;

/**
 * Utility to collect query parameters for content-provider.
 * No dependencies to android so it can be unittested
 *
 * SELECT {mColumns[]} FROM {mFrom} WHERE ({mWhere})
 * GROUP BY ({mGroupBy}) HAVING ({mHaving}) ORDER BY ({mOrderBy})
 * PARAMETERS {mParameters[]} PARAMETERS {mHavingParameters[]}
 *
 * Created by k3b on 04.06.2015.
 */
public class QueryParameter {
    /** added to every serialized item if != null. Example "Generated on 2015-10-19 with myApp Version 0815." */
    public static String sFileComment = null;

    /** added to parsed Query if it does not contain table-uris belonging to the "FROM"  keyword */
    public static String sParserDefaultFrom = null;

    /** added to parsed Query if it does not contain the "QUERY-TYPE-ID"  keyword */
    public static int sParserDefaultQueryTypeId = 0;

    /** added to parsed Query if it does not contain fields belonging to the "SELECT"  keyword */
    public static List<String> sParserDefaultSelect = null;

    // the members are protected to allow serialisation via android specific Parcles
    protected int mID = 0;
    protected final List<String> mColumns = new ArrayList<String>();
    protected final List<String> mFrom = new ArrayList<String>();
    protected final List<String> mWhere = new ArrayList<String>(); // concateneted with AND
    protected final List<String> mGroupBy = new ArrayList<String>();
    protected final List<String> mHaving = new ArrayList<String>();
    protected final List<String> mOrderBy = new ArrayList<String>();
    protected final List<String> mParameters = new ArrayList<String>();
    protected final List<String> mHavingParameters = new ArrayList<String>();
    // protected String mCurrentSelection = null;

    public QueryParameter() {
    }

    public QueryParameter(QueryParameter src) {
        getFrom(src);
    }

    /** replace all local settings from src */
    public QueryParameter getFrom(QueryParameter src) {
        if (src != null) {
            setID(src.getID());
            copy(mColumns, src.mColumns);
            copy(mFrom, src.mFrom);
            copy(mWhere, src.mWhere);
            copy(mGroupBy, src.mGroupBy);
            copy(mHaving, src.mHaving);
            copy(mOrderBy, src.mOrderBy);
            copy(mParameters, src.mParameters);
            copy(mHavingParameters, src.mHavingParameters);
        }
        return this;
    }

    /************************** begin properties *********************/

    public QueryParameter addColumn(String... columns) {
        return addToList(mColumns, false, columns);
    }

    public String[] toColumns() {
        return Helper.toList(mColumns);
    }

    public String removeFirstColumnThatContains(String namePart) {
        return removeFirstThatContains(this.mColumns, namePart);
    }

    private static String removeFirstThatContains(List<String> items, String namePart) {
        String found;
        if (items != null) {
            for(int index = items.size() -1; index >= 0; index--) {
                found = items.get(index);
                if ((found != null) && (found.contains(namePart))) {
                    items.remove(index);
                    return found;
                }
            }
        }
        return null;
    }

    public QueryParameter addFrom(String... froms) {
        return addToList(mFrom, false, froms);
    }

    public QueryParameter replaceFrom(String... froms) {
        mFrom.clear();
        return addFrom(froms);
    }

    public String toFrom() {
        StringBuilder result = new StringBuilder();
        if (!Helper.append(result, null, mFrom, ", ", "", "")) {
            return null;
        }

        return result.toString();
    }

    public QueryParameter getWhereFrom(QueryParameter src, boolean append) {
        if (src != null) {
            copy(mWhere, src.mWhere, append);
            copy(mParameters, src.mParameters, append);
        }
        return this;
    }

    public QueryParameter clearWhere() {
        mWhere.clear();
        mParameters.clear();
        return this;
    }

    public QueryParameter addWhere(String where, String... parameters) {
        mWhere.add(where);
        return addToList(mParameters, true, parameters);
    }

    public QueryParameter removeWhere(String where) {
        getWhereParameter(where,true);
        return this;
    }

    /** @return return all params for sqlExprWithParameters inside this. null if sqlExprWithParameters is not in this */
    public String[] getWhereParameter(String sqlExprWithParameters, boolean remove) {
        return getExpresionParameter(sqlExprWithParameters, mWhere, mParameters, remove);
    }

    protected static String[] getExpresionParameter(String sqlExprWithParameters, List<String> expressions, List<String> parameters, boolean remove) {
        if ((sqlExprWithParameters != null) && (expressions != null) && (parameters != null)) {
            int paramNo = 0;
            for (String p : expressions) {
                int paramCount = getParamCount(p, parameters);
                if (sqlExprWithParameters.equalsIgnoreCase(p)) {
                    String[] result = new String[paramCount];

                    int sourceIndex = paramNo;
                    for (int i=0; i < paramCount; i++) {
                        result[i] = (sourceIndex < parameters.size()) ? parameters.get(sourceIndex) : null;
                        sourceIndex++;
                    }

                    if (remove) {
                        for (int i=0; i < paramCount; i++) {
                            if (parameters.size() > paramNo) {
                                parameters.remove(paramNo);
                            }
                        }
                        expressions.remove(p);
                    }
                    return result;
                }

                paramNo += paramCount;
            }
        }
        return null;
    }

    /** counts how many "?" are inside sqlWhereWithParameters */
    private static int getParamCount(String sqlWhereWithParameters, List<String> parameters) {
        int result = 0;
        int last = sqlWhereWithParameters.indexOf("?");
        while (last >= 0) {
            result++;
            last = sqlWhereWithParameters.indexOf("?", last + 1);
        }
        return result;
    }

    /** android content-queries do not support GROUP BY.
     * Therefore this sql is added to the WHERE part.
     * [select ... from ... where (] [[mWhere][) GROUP BY (mGroupBy][) HAVING (mHaving]] [) ORDER BY ] [mOrderBy]*/
    public String toAndroidWhere() {
        boolean hasWhere = Helper.isNotEmpty(mWhere);
        boolean hasGroup = Helper.isNotEmpty(mGroupBy);
        boolean hasHaving = Helper.isNotEmpty(mHaving);
        if (!hasWhere && !hasGroup && !hasHaving) return null;

        StringBuilder result = new StringBuilder();
        if (!Helper.append(result, null, mWhere, " AND ", "(", ")")) {
            result.append("1=1");
        }
        Helper.append(result, ") GROUP BY (", mGroupBy, "), (", "", "");
        if (hasHaving) {
            Helper.append(result, ") HAVING (", mHaving, "), (", "", "");
        }

        return result.toString();
    }

    public String[] toAndroidParameters() {
        return Helper.toList(mParameters, mHavingParameters);
    }

    public QueryParameter addGroupBy(String... parameters) {
        return addToList(mGroupBy, false, parameters);
    }

    public QueryParameter addHaving(String having, String... parameters) {
        mHaving.add(having);
        return addToList(mHavingParameters, true, parameters);
    }

    public QueryParameter getOrderByFrom(QueryParameter src, boolean append) {
        if (src != null) {
            copy(mOrderBy, src.mOrderBy, append);
        }
        return this;
    }

    public QueryParameter replaceOrderBy(String... orders) {
        mOrderBy.clear();
        return addOrderBy(orders);
    }
    public QueryParameter addOrderBy(String... orders) {
        return addToList(mOrderBy, false, orders);
    }

    public String toOrderBy() {
        StringBuilder result = new StringBuilder();
        if (!Helper.append(result, null, mOrderBy, ", ", "", "")) {
            return null;
        }

        return result.toString();
    }

    /************************** end properties *********************/
    public String toReParseableString() {
        StringBuilder result = new StringBuilder();
        if (sFileComment != null) result.append("# ").append(sFileComment).append("\n");
        Helper.append(result, "\nFROM ", mFrom, "", "\n\t", "");
        if (mID != 0) result.append("\n\tQUERY-TYPE-ID\n\t\t").append(mID);
        Helper.append(result, "\nSELECT ", mColumns, "", "\n\t", "");
        Helper.append(result, "\nWHERE ", mWhere, "", "\n\t", "");
        Helper.append(result, "\n\tWHERE-PARAMETERS ", mParameters, "", "\n\t\t", "");
        Helper.append(result, "\nGROUP-BY ", mGroupBy, "", "\n\t", "");
        Helper.append(result, "\nHAVING ", mHaving, "", "\n\t", "");
        Helper.append(result, "\n\tHAVING-PARAMETERS ", mHavingParameters, "", "\n\t\t", "");
        Helper.append(result, "\nORDER-BY ", mOrderBy, "", "\n\t", "");

        // protected String mCurrentSelection = null;

        if (result.length() == 0) return null;

        return result.toString();
    }

    private static final String PARSER_KEYWORDS = ";FROM;QUERY-TYPE-ID;SELECT;WHERE;WHERE-PARAMETERS;GROUP-BY;HAVING;HAVING-PARAMETERS;ORDER-BY;";

    public static QueryParameter parse(String stringToBeParsed) {
        List<QueryParameter> result = (stringToBeParsed != null) ? parseMultiple(stringToBeParsed) : null;
        if (result == null) return null;
        return result.get(0);
    }

    public static List<QueryParameter> parseMultiple(String stringToBeParsed) {
        List<QueryParameter> result = new ArrayList<QueryParameter>();
        QueryParameter currentParseItem = null;

        List<String> params = null;

        String[] lines = stringToBeParsed.split("\n");
        int i = 0;
        String line = null;
        while(i < lines.length) {
            line = lines[i++].trim();
            if (isKeyword(line)) {
                final String keyword = line.toUpperCase();

                if (keyword.compareTo("FROM") == 0) {
                    // next "FROM" occured. finish previos query if available
                    fixQuery(currentParseItem);
                    currentParseItem = null;
                }

                // a keyword has occured: now there is a current-query
                if (currentParseItem == null) {
                    currentParseItem = new QueryParameter();
                    result.add(currentParseItem);
                }
                switch (keyword)
                {
                    case "QUERY-TYPE-ID":
                        line = (i < lines.length) ? lines[i].trim() : "0";
                        if (!isKeyword(line)) {
                            i++;
                            currentParseItem.setID(Integer.parseInt(line));
                        }
                        continue;

                    case "FROM": params = currentParseItem.mFrom; break;
                    case "SELECT": params = currentParseItem.mColumns; break;
                    case "WHERE": params = currentParseItem.mWhere; break;
                    case "WHERE-PARAMETERS": params = currentParseItem.mParameters; break;
                    case "GROUP-BY": params = currentParseItem.mGroupBy; break;
                    case "HAVING": params = currentParseItem.mHaving; break;
                    case "HAVING-PARAMETERS": params = currentParseItem.mHavingParameters; break;
                    case "ORDER-BY": params = currentParseItem.mOrderBy; break;
                    default:break;
                }
            } else if ((params != null) && (isNoComment(line)) && (line.trim().length() > 0)) {
                params.add(line);
            }
        }

        if (result.size() > 0) {
            // make shure that last query has been fixed.
            fixQuery(currentParseItem);

            return result;
        }

        // indicate nothing found
        return null;
    }

    private static void fixQuery(QueryParameter current) {
        if (current != null) {
            // default values if not found in sql-string
            if ((current.getID() == 0)) current.setID(QueryParameter.sParserDefaultQueryTypeId);
            if ((current.mFrom.size() == 0) && (QueryParameter.sParserDefaultFrom != null)) current.mFrom.add(QueryParameter.sParserDefaultFrom);
            if ((current.mColumns.size() == 0) && (QueryParameter.sParserDefaultSelect != null)) current.mColumns.addAll(QueryParameter.sParserDefaultSelect);
        }
    }

    private static boolean isNoComment(String line) {
        return ((line != null) && !line.startsWith("#") && !line.startsWith("//") && !line.startsWith("--") );
    }

    private static boolean isKeyword(String line) {
        String fixedLine = (line != null) ? (";" + line.trim().toUpperCase() + ";") : null;
        return ((fixedLine != null) && (PARSER_KEYWORDS.contains(fixedLine)));
    }

    public String toSqlString() {
        StringBuilder result = new StringBuilder();
        Helper.append(result, " SELECT ", mColumns, ", ", "", "");
        Helper.append(result, " \nFROM ", mFrom, ", ", "", "");
        Helper.append(result, " \nWHERE ", mWhere, " AND ", "(", ")");
        Helper.append(result, " \n\tPARAMETERS ", mParameters, ", ", "", "");
        Helper.append(result, " \nGROUP BY ", mGroupBy, ", ", "", "");
        Helper.append(result, " \nHAVING ", mHaving, " AND ", "(", ")");
        Helper.append(result, " \n\tPARAMETERS ", mHavingParameters, ", ", "", "");
        Helper.append(result, " \nORDER BY ", mOrderBy, ", ", "", "");

        if (result.length() == 0) return null;

        return result.toString();
    }

    public String toSqlStringAndroid() {
        return toString(toColumns(), null, toFrom(), toAndroidWhere(), toAndroidParameters(), toOrderBy(), -1);
    }

    /** Creates sql debug string */
    public static String toString(String[] sqlSelectFields, String sqlUpdateValues,
                                  String from,
                                  String sqlWhereStatement, String[] sqlWhereParameters,
                                  String sqlSortOrder, int rowcount) {
        StringBuilder result = new StringBuilder();
        Helper.append(result, " SELECT ", sqlSelectFields, ", ", "", "");
        Helper.append(result, " UPDATE ", sqlUpdateValues);
        Helper.append(result, " \nFROM ", from);
        Helper.append(result, " \nWHERE (", sqlWhereStatement, ")");
        Helper.append(result, " \nORDER BY ", sqlSortOrder);
        Helper.append(result, " \n\tPARAMETERS ", sqlWhereParameters, ", ", "", "");

        if (rowcount >= 0) {
            result.append(" \n=> ").append(rowcount).append(" rows affected");
        }
        if (result.length() == 0) return null;

        return result.toString();
    }

    @Override
    public String toString() {
        return toSqlString().replace("\n", " ");
    }

    /************************** local helpers *********************/

    private QueryParameter addToList(final List<String> list, boolean allowNull, final String[] parameters) {
        for (String parameter : parameters) {
            if ((allowNull) || (parameter != null)) {
                list.add(parameter);
            }
        }
        return this;
    }

    private static class Helper {

        private static String[] toList(List<String>... lists) {
            int size = 0;
            for (List<String> list : lists) {
                if (list != null) size += list.size();
            }
            if (size == 0) return null;

            String[] result = new String[size];
            int next = 0;
            for (List<String> list : lists) {
                if (list != null) {
                    int listSize = list.size();
                    for (int i = 0; i < listSize; i++) {
                        result[next++] = list.get(i);
                    }
                }
            }

            return result;
        }

        private static boolean append(StringBuilder result, String blockPrefix, List<String> list, String delimiter, String before, String after) {
            if (isNotEmpty(list)) {
                if (blockPrefix != null) {
                    result.append(blockPrefix);
                }

                boolean first = true;
                int listSize = list.size();
                for (int i = 0; i < listSize; i++) {
                    if (!first) {
                        result.append(delimiter);
                    }

                    result.append(before);
                    result.append(list.get(i));
                    result.append(after);
                    first = false;
                }
                return true;
            }
            return false;
        }

        private static boolean append(StringBuilder result, String blockPrefix, String nonEmptyItem) {
            return append(result, blockPrefix, nonEmptyItem, null);
        }

        private static boolean append(StringBuilder result, String blockPrefix, String nonEmptyItem, String blockSuffix) {
            if ((nonEmptyItem != null) && (nonEmptyItem.length() > 0)) {
                if (blockPrefix != null) {
                    result.append(blockPrefix);
                }
                result.append(nonEmptyItem);
                if (blockSuffix != null) {
                    result.append(blockSuffix);
                }
                return true;
            }
            return false;
        }

        private static boolean append(StringBuilder result, String blockPrefix, String[] list, String delimiter, String before, String after) {
            if ((list != null) && (list.length > 0)) {
                if (blockPrefix != null) {
                    result.append(blockPrefix);
                }

                boolean first = true;
                int listSize = list.length;
                for (String listElement : list) {
                    if (!first) {
                        result.append(delimiter);
                    }

                    result.append(before);
                    result.append(listElement);
                    result.append(after);
                    first = false;
                }
                return true;
            }
            return false;
        }

        private static boolean isNotEmpty(List<String> list) {
            return (list != null) && (list.size() > 0);
        }
    }

    private void copy(List<String> dest, List<String> src, boolean append) {
        if (!append) dest.clear();
        dest.addAll(src);
    }

    private void copy(List<String> dest, List<String> src) {
        copy(dest, src, false);
    }

    public int getID() {
        return mID;
    }

    public QueryParameter setID(int mID) {
        this.mID = mID;
        return this;
    }

    /*
    public String getCurrentSelection() {
        return mCurrentSelection;
    }

    public QueryParameter setCurrentSelection(String value) {
        this.mCurrentSelection = value;
        return this;
    }
    */
}


