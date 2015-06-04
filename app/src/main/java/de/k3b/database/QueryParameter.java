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
    protected int mID = 0;
    protected final List<String> mColumns = new ArrayList<String>();
    protected final List<String> mFrom = new ArrayList<String>();
    protected final List<String> mWhere = new ArrayList<String>(); // concateneted with AND
    protected final List<String> mGroupBy = new ArrayList<String>();
    protected final List<String> mHaving = new ArrayList<String>();
    protected final List<String> mOrderBy = new ArrayList<String>();
    protected final List<String> mParameters = new ArrayList<String>();
    protected final List<String> mHavingParameters = new ArrayList<String>();

    public QueryParameter() {
    }

    /** replace all local settings from src */
    public QueryParameter set(QueryParameter src) {
        setID(src.getID());
        get(mColumns            , src.mColumns           );
        get(mFrom               , src.mFrom              );
        get(mWhere              , src.mWhere             );
        get(mGroupBy            , src.mGroupBy           );
        get(mHaving             , src.mHaving            );
        get(mOrderBy            , src.mOrderBy           );
        get(mParameters         , src.mParameters        );
        get(mHavingParameters   , src.mHavingParameters  );
        return this;
    }

    /************************** begin properties *********************/

    public QueryParameter addColumn(String... columns) {
        return addToList(mColumns, false, columns);
    }

    public String[] toColumns() {
        return toList(mColumns);
    }

    public QueryParameter addFrom(String... froms) {
        return addToList(mFrom, false, froms);
    }

    public String toFrom() {
        StringBuilder result = new StringBuilder();
        if (!append(result, null, mFrom, ", ", "", "")) {
            return null;
        }

        return result.toString();
    }

    public QueryParameter setWhere(QueryParameter src, boolean append) {
        get(mWhere              , src.mWhere             ,append);
        get(mParameters         , src.mParameters        ,append);
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
        boolean hasWhere = isNotEmpty(mWhere);
        boolean hasGroup = isNotEmpty(mGroupBy);
        if (!hasWhere && !hasGroup) return null;

        StringBuilder result = new StringBuilder();
        if (!append(result, null, mWhere, " AND ", "(", ")")) {
            result.append("1=1");
        }
        append(result, ") GROUP BY (", mGroupBy, ", ", "", "");

        return result.toString();
    }

    public String[] toAndroidParameters() {
        return toList(mParameters, mHavingParameters);
    }

    public QueryParameter addGroupBy(String... parameters) {
        return addToList(mGroupBy, false, parameters);
    }

    public QueryParameter addHaving(String having, String... parameters) {
        mHaving.add(having);
        return addToList(mHavingParameters, true, parameters);
    }

    public QueryParameter setOrderBy(QueryParameter src, boolean append) {
        get(mOrderBy            , src.mOrderBy           ,append);
        return this;
    }

    public QueryParameter addOrderBy(String... orders) {
        return addToList(mOrderBy, false, orders);
    }

    public String toOrderBy() {
        StringBuilder result = new StringBuilder();
        if (!append(result, null, mOrderBy, ", ", "", "")) {
            return null;
        }

        return result.toString();
    }

    /************************** end properties *********************/
    public String toSqlString() {
        StringBuilder result = new StringBuilder();
        append(result, " SELECT ", mColumns, ", ", "", "");
        append(result, " \nFROM ", mFrom, ", ", "", "");
        append(result, " \nWHERE ", mWhere, " AND ", "(", ")");
        append(result, " \n\tPARAMETERS ", mParameters, ", ", "", "");
        append(result, " \nGROUP BY ", mGroupBy, ", ", "", "");
        append(result, " \nHAVING ", mHaving, " AND ", "(", ")");
        append(result, " \n\tPARAMETERS ", mHavingParameters, ", ", "", "");
        append(result, " \nORDER BY ", mOrderBy, ", ", "", "");

        if (result.length() == 0) return null;

        return result.toString();
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

    private String[] toList(List<String>... lists) {
        int size = 0;
        for (List<String> list : lists) {
            if (list != null) size += list.size();
        }
        if (size == 0) return null;

        String[] result = new String[size];
        int next=0;
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

    private boolean append(StringBuilder result, String blockPrefix, List<String> list, String delimiter, String before, String after) {
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

    private boolean isNotEmpty(List<String> list) {
        return (list != null) && (list.size() > 0);
    }

    private void get(List<String> dest, List<String> src, boolean append) {
        if (!append) dest.clear();
        dest.addAll(src);
    }

    private void get(List<String> dest, List<String> src) {
        get(dest, src, false);
    }

    public int getID() {
        return mID;
    }

    public QueryParameter setID(int mID) {
        this.mID = mID;
        return this;
    }
}


