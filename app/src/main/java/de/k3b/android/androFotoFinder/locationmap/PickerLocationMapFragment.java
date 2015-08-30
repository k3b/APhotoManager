package de.k3b.android.androFotoFinder.locationmap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.GalleryFilterParameterParcelable;
import de.k3b.android.osmdroid.DefaultResourceProxyImplEx;
import de.k3b.android.osmdroid.IconOverlay;
import de.k3b.database.SelectedItems;
import de.k3b.io.GeoRectangle;

/**
 * LocationMapFragment working as Picker. Current result is the red marker.
 *
 * Created by k3b on 30.08.2015.
 */
public class PickerLocationMapFragment extends LocationMapFragment {
    private final String mDebugPrefix;
    private IconOverlay mCurrrentSelectionMarker = null;
    private int mMarkerId = -1;
    private boolean mUsePicker;

    public PickerLocationMapFragment() {
        mDebugPrefix = "PickerLocationMapFragment ";
    }
    /**
     * non tap-able marker that moves to last tap position
     */
    private class CurrentSelectionMarker extends IconOverlay {
        public CurrentSelectionMarker(ResourceProxy pResourceProxy, IGeoPoint position, Drawable icon) {
            super(pResourceProxy, position, icon);
        }

        /**
         * remember last tap position in this marker
         */
        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e, final MapView mapView) {
            if (isEnabled()) {
                mMarkerId = -1;
                moveTo(e, mapView);
                updateMarker(null);
                hideImage();
            }
            return super.onSingleTapConfirmed(e, mapView);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Intent intent = this.getActivity().getIntent();
        mUsePicker = ((Intent.ACTION_PICK.equals(intent.getAction())) || (Intent.ACTION_GET_CONTENT.equals(intent.getAction())));

        View result = super.onCreateView(inflater, container, savedInstanceState);

        if (mUsePicker) {
            defineButtons(result);
        }
        return result;
    }

    @Override
    protected void definteOverlays(MapView mapView, DefaultResourceProxyImplEx resourceProxy) {
        super.definteOverlays(mapView, resourceProxy);

        if (true) {
            Drawable currrentSelectionIcon = getActivity().getResources().getDrawable(R.drawable.marker_red);
            if (mUsePicker) {
                // updateed position on tap
                this.mCurrrentSelectionMarker = new CurrentSelectionMarker(resourceProxy, null, currrentSelectionIcon);
            } else {
                // fixed positon, not updated on pick
                this.mCurrrentSelectionMarker = new IconOverlay(resourceProxy, null, currrentSelectionIcon);
            }
            mapView.getOverlays().add(mCurrrentSelectionMarker);
        }
    }

    @Override
    protected boolean onMarkerClicked(IconOverlay marker, int markerId, IGeoPoint makerPosition, Object markerData) {
        boolean result = super.onMarkerClicked(marker, markerId, makerPosition, markerData);

        if (mUsePicker) {
            mCurrrentSelectionMarker.moveTo(makerPosition, mMapView);
            mMarkerId = markerId;
            updateMarker(marker);
        }

        return result;
    }

    private void updateMarker(IconOverlay marker) {
        if ((mUsePicker) && (mCurrrentSelectionMarker != null)) {
            mMapView.getOverlays().remove(mCurrrentSelectionMarker);
            mMapView.getOverlays().add(mCurrrentSelectionMarker);
        }
    }

    @Override
    public void defineNavigation(GalleryFilterParameterParcelable rootFilter, GeoRectangle rectangle, int zoomlevel, SelectedItems selectedItems) {
        super.defineNavigation(rootFilter, rectangle, zoomlevel, selectedItems);
        if ((rectangle != null) && (mCurrrentSelectionMarker != null) && (mMapView != null)) {
            mCurrrentSelectionMarker.moveTo(new GeoPoint(rectangle.getLatitudeMin(), rectangle.getLogituedMin()), mMapView);
        }
    }

    @Override
    protected void onOk() {
        IGeoPoint result = null;

        Activity activity = getActivity();
        if (mMarkerId != -1) {
            result = FotoSql.getPosition(activity, mMarkerId);
        }

        if (result == null) {
            result = this.mCurrrentSelectionMarker.getPosition();
        }

        if (result != null) {
            int currentZoomLevel = this.mMapView.getZoomLevel();
            String uriCurrentViewport = mGeoUriEngine.toUriString(result.getLatitude(), result.getLongitude(), currentZoomLevel);

            if (Global.debugEnabled) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + "onOk: " + uriCurrentViewport);
            }
            Intent resultIntent = new Intent(activity.getIntent());
            resultIntent.setData(Uri.parse(uriCurrentViewport));
            activity.setResult(1, resultIntent);
            activity.finish();
        }
    }

    @Override
    protected void onCancel() {
        this.getActivity().finish();
    }

    // warum ist roter marker nicht sichtbar wenn auf grünen marker gecklickt wurde ???
}
