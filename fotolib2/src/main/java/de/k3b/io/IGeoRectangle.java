package de.k3b.io;

/**
 * Created by k3b on 13.07.2015.
 */
public interface IGeoRectangle {
    double getLatitudeMin();

    double getLatitudeMax();

    double getLogituedMin();

    double getLogituedMax();

    IGeoRectangle get(IGeoRectangle src);
}
