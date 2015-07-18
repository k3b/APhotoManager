package de.k3b.io;

/**
 * parameter for foto filter by location: only fotos from certain lat/lon will be visible.
 * Created by k3b on 12.07.2015.
 */
public class GeoRectangle implements IGeoRectangle {
    private double latitudeMin = Double.NaN;
    private double latitudeMax = Double.NaN;
    private double logituedMin = Double.NaN;
    private double logituedMax = Double.NaN;

    public GeoRectangle get(IGeoRectangle src) {
        this.setLogituedMin(src.getLogituedMin());
        this.setLatitudeMin(src.getLatitudeMin());
        this.setLatitudeMax(src.getLatitudeMax());
        this.setLogituedMax(src.getLogituedMax());
        return this;
    }

    @Override
    public double getLatitudeMin() {
        return latitudeMin;
    }

    public void setLatitudeMin(double latitudeMin) {
        this.latitudeMin = latitudeMin;
    }

    @Override
    public double getLatitudeMax() {
        return latitudeMax;
    }

    public void setLatitudeMax(double latitudeMax) {
        this.latitudeMax = latitudeMax;
    }

    @Override
    public double getLogituedMin() {
        return logituedMin;
    }

    public void setLogituedMin(double logituedMin) {
        this.logituedMin = logituedMin;
    }

    @Override
    public double getLogituedMax() {
        return logituedMax;
    }

    public void setLogituedMax(double logituedMax) {
        this.logituedMax = logituedMax;
    }

    @Override
    public String toString() {
        return "" + getLatitudeMin() + "," + getLogituedMin() + "-"  + getLatitudeMax() + "," + getLogituedMax();
    }
}
