package de.k3b.android.fotoviewer.queries;

/**
 * Created by k3b on 11.07.2015.
 */
public class GalleryFilter {
    private String path = null;

    private double latitudeMin = 0;
    private double latitudeMax = 0;
    private double logituedMin = 0;
    private double logituedMax = 0;
    private boolean includeNoLatLong = false;

    private int dateMin = 0;
    private int dateMax = 0;

    /******************** properties **************************/
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public double getLatitudeMin() {
        return latitudeMin;
    }

    public void setLatitudeMin(double latitudeMin) {
        this.latitudeMin = latitudeMin;
    }

    public double getLatitudeMax() {
        return latitudeMax;
    }

    public void setLatitudeMax(double latitudeMax) {
        this.latitudeMax = latitudeMax;
    }

    public double getLogituedMin() {
        return logituedMin;
    }

    public void setLogituedMin(double logituedMin) {
        this.logituedMin = logituedMin;
    }

    public double getLogituedMax() {
        return logituedMax;
    }

    public void setLogituedMax(double logituedMax) {
        this.logituedMax = logituedMax;
    }

    public boolean isIncludeNoLatLong() {
        return includeNoLatLong;
    }

    public void setIncludeNoLatLong(boolean includeNoLatLong) {
        this.includeNoLatLong = includeNoLatLong;
    }

    public int getDateMin() {
        return dateMin;
    }

    public void setDateMin(int dateMin) {
        this.dateMin = dateMin;
    }

    public int getDateMax() {
        return dateMax;
    }

    public void setDateMax(int dateMax) {
        this.dateMax = dateMax;
    }

}
