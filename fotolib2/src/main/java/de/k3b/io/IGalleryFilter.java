package de.k3b.io;

/**
 * Created by k3b on 13.07.2015.
 */
public interface IGalleryFilter extends IGeoRectangle {
    /******************** properties **************************/
    String getPath();

    long getDateMin();

    long getDateMax();

    IGalleryFilter get(IGalleryFilter src);


}
