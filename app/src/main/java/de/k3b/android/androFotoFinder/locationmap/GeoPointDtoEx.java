package de.k3b.android.androFotoFinder.locationmap;

import android.graphics.drawable.Drawable;

import org.osmdroid.api.IGeoPoint;

import de.k3b.android.osmdroid.infowindow.IMarkerInfoData;
import de.k3b.geo.api.GeoPointDto;

/**
 * geopoint that is osmdroid-IGeoPoint and k3b-IGeoPointInfo
 * Created by k3b on 23.08.2016.
 */
public class GeoPointDtoEx extends GeoPointDto implements IGeoPoint, IMarkerInfoData {
    @Override
    public int getLatitudeE6() {
        return (int) (getLatitude() * 1E6);
    }

    @Override
    public int getLongitudeE6() {
        return (int) (getLongitude() * 1E6);
    }

    @Override
    public String getTitle() {
        return getName();
    }

    @Override
    public String getSnippet() {
        return getDescription();
    }

    @Override
    public String getSubDescription() {
        return null;
    }

    @Override
    public Drawable getImage() {
        return null;
    }
}
