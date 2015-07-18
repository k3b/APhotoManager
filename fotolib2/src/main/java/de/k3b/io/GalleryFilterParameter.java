package de.k3b.io;

/**
 * parameter for foto filter: only fotos from certain filepath, date and/or lat/lon will be visible.
 * Created by k3b on 11.07.2015.
 */
public class GalleryFilterParameter extends GeoRectangle implements IGalleryFilter {
    private String path = null;

    private long dateMin = 0;
    private long dateMax = 0;

    public GalleryFilterParameter get(IGalleryFilter src) {
        super.get(src);
        this.setDateMax(src.getDateMax());
        this.setDateMin(src.getDateMin());
        this.setPath(src.getPath());
        return this;
    }

    /******************** properties **************************/
    @Override
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public long getDateMin() {
        return dateMin;
    }

    public void setDateMin(long dateMin) {
        this.dateMin = dateMin;
    }

    @Override
    public long getDateMax() {
        return dateMax;
    }

    public void setDateMax(long dateMax) {
        this.dateMax = dateMax;
    }

}
