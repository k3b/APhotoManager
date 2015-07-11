package de.k3b.android.fotoviewer.queries;

import android.os.Parcel;
import android.os.Parcelable;

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
        setDateMin(in.readInt());
        setDateMax(in.readInt());
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
        dest.writeInt(getDateMin());
        dest.writeInt(getDateMax());

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
}
