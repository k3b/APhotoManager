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
    protected String mCurrentSelection = null;

    public QueryParameter() {
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

    public QueryParameter addFrom(String... froms) {
        return addToList(mFrom, false, froms);
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

    public QueryParameter addWhere(String where, String... parameters) {
        mWhere.add(where);
        return addToList(mParameters, true, parameters);
    }

    /** android content-queries do not support GROUP BY.
     * Therefore this sql is added to the WHERE part.
     * [select ... from ... where (] [mWhere) GROUP BY (mGroupBy] [) ORDER BY ] [mOrderBy]*/
    public String toAndroidWhere() {
        boolean hasWhere = Helper.isNotEmpty(mWhere);
        boolean hasGroup = Helper.isNotEmpty(mGroupBy);
        if (!hasWhere && !hasGroup) return null;

        StringBuilder result = new StringBuilder();
        if (!Helper.append(result, null, mWhere, " AND ", "(", ")")) {
            result.append("1=1");
        }
        Helper.append(result, ") GROUP BY (", mGroupBy, "), (", "", "");

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
        return toString(toFrom(), toColumns(), toAndroidWhere(), toAndroidParameters(), toOrderBy());
    }

    public static String toString(String from, String[] sqlProjection, String sqlWhereStatement, String[] sqlWhereParameters, String sqlSortOrder) {
        StringBuilder result = new StringBuilder();
        Helper.append(result, " SELECT ", sqlProjection, ", ", "", "");
        Helper.append(result, " \nFROM ", from);
        Helper.append(result, " \nWHERE (", sqlWhereStatement);
        Helper.append(result, ") \nORDER BY ", sqlSortOrder);

        Helper.append(result, " \n\tPARAMETERS ", sqlWhereParameters, ", ", "", "");

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

        private static boolean append(StringBuilder result, String blockPrefix, String list) {
            if ((list != null) && (list.length() > 0)) {
                if (blockPrefix != null) {
                    result.append(blockPrefix);
                }
                result.append(list);
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
                for (int i = 0; i < listSize; i++) {
                    if (!first) {
                        result.append(delimiter);
                    }

                    result.append(before);
                    result.append(list[i]);
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

    public String getCurrentSelection() {
        return mCurrentSelection;
    }

    public QueryParameter setCurrentSelection(String value) {
        this.mCurrentSelection = value;
        return this;
    }
}


