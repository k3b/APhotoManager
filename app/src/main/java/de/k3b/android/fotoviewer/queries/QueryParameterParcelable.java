package de.k3b.android.fotoviewer.queries;

import android.os.Parcel;
import android.os.Parcelable;

import de.k3b.database.QueryParameter;

/**
 * Utility to collect query parameters for content-provider with Android Parcelable support.
 *
 * SELECT {mColumns[]} FROM {mFrom} WHERE ({mWhere})
 * GROUP BY ({mGroupBy}) HAVING ({mHaving}) ORDER BY ({mOrderBy})
 * PARAMETERS {mParameters[]} PARAMETERS {mHavingParameters[]}
 *
 * Created by k3b on 04.06.2015.
 */
public class QueryParameterParcelable extends QueryParameter implements Parcelable {
    /*************************** Parcelable support ***************************/

    /**
     * Classes implementing the Parcelable
     * interface must also have a static field called <code>CREATOR</code>, which
     * is an object implementing the {@link Parcelable.Creator Parcelable.Creator}
     * interface.
     */
    public static final Parcelable.Creator<QueryParameterParcelable> CREATOR
            = new Parcelable.Creator<QueryParameterParcelable>() {
        public QueryParameterParcelable createFromParcel(Parcel in) {
            return new QueryParameterParcelable(in);
        }

        public QueryParameterParcelable[] newArray(int size) {
            return new QueryParameterParcelable[size];
        }
    };

    public QueryParameterParcelable() {};

    public QueryParameterParcelable(QueryParameterParcelable src)
    {
        this.getFrom(src);
    }

    /** to desirialize from Parcel */
    private QueryParameterParcelable(Parcel in) {
        setID(in.readInt());
        in.readList(mColumns, null);
        in.readList(mFrom, null);
        in.readList(mWhere, null);
        in.readList(mGroupBy, null);
        in.readList(mHaving, null);
        in.readList(mOrderBy, null);
        in.readList(mParameters, null);
        in.readList(mHavingParameters,null);
        mCurrentSelection = in.readString();
    }

    /**
     * Parcelable: Describe the kinds of special objects contained in this Parcelable's
     * marshalled representation.
     *
     * @return a bitmask indicating the getFrom of special object types marshalled
     * by the Parcelable.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Parcelable: Flatten this object in to a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     *              May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.getID());
        dest.writeList(mColumns);
        dest.writeList(mFrom);
        dest.writeList(mWhere);
        dest.writeList(mGroupBy);
        dest.writeList(mHaving);
        dest.writeList(mOrderBy);
        dest.writeList(mParameters);
        dest.writeList(mHavingParameters);
        dest.writeString(mCurrentSelection);
    }

}
