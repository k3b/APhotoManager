package de.k3b.android.fotoviewer.queries;

import de.k3b.io.GeoRectangle;

/**
 * Created by k3b on 11.07.2015.
 */
public class GalleryFilter extends GeoRectangle {
    private String path = null;

    private boolean includeNoLatLong = false;

    private long dateMin = 0;
    private long dateMax = 0;

    /******************** properties **************************/
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isIncludeNoLatLong() {
        return includeNoLatLong;
    }

    public void setIncludeNoLatLong(boolean includeNoLatLong) {
        this.includeNoLatLong = includeNoLatLong;
    }

    public long getDateMin() {
        return dateMin;
    }

    public void setDateMin(long dateMin) {
        this.dateMin = dateMin;
    }

    public long getDateMax() {
        return dateMax;
    }

    public void setDateMax(long dateMax) {
        this.dateMax = dateMax;
    }

}
