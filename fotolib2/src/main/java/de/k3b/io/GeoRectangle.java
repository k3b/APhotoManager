package de.k3b.io;

/**
 * Created by k3b on 12.07.2015.
 */
public class GeoRectangle {
    private double latitudeMin = 0;
    private double latitudeMax = 0;
    private double logituedMin = 0;
    private double logituedMax = 0;

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

    @Override
    public String toString() {
        return "" + getLatitudeMin() + "," + getLogituedMin() + ".."  + getLatitudeMax() + "," + getLogituedMax();
    }
}
