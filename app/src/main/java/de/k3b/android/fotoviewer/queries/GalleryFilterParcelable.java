package de.k3b.android.fotoviewer.queries;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.GregorianCalendar;

import de.k3b.io.DirectoryFormatter;

/**
 * Created by k3b on 11.07.2015.
 */
public class GalleryFilterParcelable extends GalleryFilter  implements Parcelable {
    /**
     * Classes implementing the Parcelable
     * interface must also have a static field called <code>CREATOR</code>, which
     * is an object implementing the {@link Parcelable.Creator Parcelable.Creator}
     * interface.
     */
    public static final Parcelable.Creator<GalleryFilterParcelable> CREATOR
            = new Parcelable.Creator<GalleryFilterParcelable>() {
        public GalleryFilterParcelable createFromParcel(Parcel in) {
            return new GalleryFilterParcelable(in);
        }

        public GalleryFilterParcelable[] newArray(int size) {
            return new GalleryFilterParcelable[size];
        }
    };

    public GalleryFilterParcelable() {};

    /************* parcable support **********************/

    /** to desirialize from Parcel */
    private GalleryFilterParcelable(Parcel in) {
        setPath(in.readString());

        setLatitudeMin(in.readDouble());
        setLatitudeMax(in.readDouble());
        setLogituedMin(in.readDouble());
        setLogituedMax(in.readDouble());
        setIncludeNoLatLong(in.readInt() != 0);
        setDateMin(in.readLong());
        setDateMax(in.readLong());
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     *              May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getPath());

        dest.writeDouble(getLatitudeMin());
        dest.writeDouble(getLatitudeMax());
        dest.writeDouble(getLogituedMin());
        dest.writeDouble(getLogituedMax());
        dest.writeInt((isIncludeNoLatLong()) ? 1 : 0);
        dest.writeLong(getDateMin());
        dest.writeLong(getDateMax());

    }

    /**
     * Describe the kinds of special objects contained in this Parcelable's
     * marshalled representation.
     *
     * @return a bitmask indicating the set of special object types marshalled
     * by the Parcelable.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    public boolean set(String selectedAbsolutePath, int queryTypeId) {
        switch (queryTypeId) {
            case FotoSql.QUERY_TYPE_GROUP_ALBUM:
                setPath(selectedAbsolutePath);
                return true;
            case FotoSql.QUERY_TYPE_GROUP_DATE:
                Date from = new Date();
                Date to = new Date();

                DirectoryFormatter.getDates(selectedAbsolutePath, from, to);
                setDateMin(from.getTime());
                setDateMax(to.getTime());
                return true;

        }
        return false;
    }
}
