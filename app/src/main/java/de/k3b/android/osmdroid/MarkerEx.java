package de.k3b.android.osmdroid;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.views.MapView;

import java.lang.ref.WeakReference;

/**
 * Created by k3b on 12.09.2016.
 */
public class MarkerEx<DataType> extends ClickableIconOverlay<DataType> {
    private final WeakReference<MarkerBubblePopupBase<DataType>> mPopupHandler;

    public MarkerEx(MarkerBubblePopupBase<DataType> popupHandler) {
        super();
        this.mPopupHandler = (popupHandler == null)
                ? null
                : new WeakReference<MarkerBubblePopupBase<DataType>>(popupHandler);
    }

    /**
     * @param mapView
     * @param markerId
     * @param makerPosition
     * @param markerData
     * @return true if click was handeled.
     */
    @Override
    protected boolean onMarkerClicked(MapView mapView, int markerId, IGeoPoint makerPosition, DataType markerData) {
        MarkerBubblePopupBase<DataType> popupHandler = (this.mPopupHandler == null) ? null : this.mPopupHandler.get();

        if (popupHandler != null) {
            popupHandler.close();
            popupHandler.open(mapView, makerPosition, 0, 0, markerData);
        }
        return true;
    }
}
