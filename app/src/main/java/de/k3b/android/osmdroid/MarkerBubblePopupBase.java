package de.k3b.android.osmdroid;

/** {@link de.k3b.android.osmdroid.MarkerBubblePopupBase} is a (pop-up-) View that can
 * be displayed on an {@link org.osmdroid.views.MapView}, associated to a
 * {@link org.osmdroid.api.IGeoPoint}.
 *
 * Typical usage: cartoon-like bubbles displayed when clicking an overlay item (i.e. a
 * {@link org.osmdroid.views.overlay.Marker}).
 * It mimics the InfoWindow class of Google Maps JavaScript API V3.
 * Main differences are:
 * <ul>
 * <li>Structure and content of the view is let to the responsibility of the caller. </li>
 * <li>The same InfoWindow can be associated to many items. </li>
 * </ul>
 *
 */

import android.view.MotionEvent;
import android.view.View;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.views.MapView;

/**
 * Reimplementation of InfoWindow
 * Created by k3b on 12.09.2016.
 */
public abstract class MarkerBubblePopupBase<DataType> {
    /** owner of popup-view or null if popup not visible */
    private MapView mMapView;

    /** popup-view or null if not visible */
    private View mPopupView = null;

    /** data that belongs to popup or null if popup not visible */
    private DataType mData;

    private int m_id_bubble_layout;

    public MarkerBubblePopupBase(int id_bubble_layout) {
        m_id_bubble_layout = id_bubble_layout;
    }

    /**
     * open the InfoWindow at the specified geoPosition + offset.
     * If it was already opened, close it before reopening.
     * @param geoPosition to place the window on the map
     * @param offsetX (&offsetY) the offset of the view to the position, in pixels.
     * @param data data that defines the content of the popup
     */
    public void open(MapView mapView, IGeoPoint geoPosition, int offsetX, int offsetY, DataType data) {
        close(); //if it was already opened
        mMapView = mapView;
        setData(data);
        mPopupView = OsmdroidUtil.openMapPopupView(mapView, m_id_bubble_layout, geoPosition, MapView.LayoutParams.BOTTOM_CENTER, offsetX, offsetY);
        this.mPopupView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                if(e.getAction() == MotionEvent.ACTION_UP) {
                    close();
                }

                return true;
            }
        });

        onCreate(mapView, mPopupView, data);
    }

    /** is called to populate the popupBubble */
    abstract protected void onCreate(MapView mapView, View popupView, DataType data);

    public void close() {
        OsmdroidUtil.closeMapPopupView(mMapView, mPopupView);
        mPopupView = null;
        mMapView = null;
        mData = null;
    }

    /** data that belongs to popup or null if popup not visible */
    public DataType getData() {
        return mData;
    }

    /** data that belongs to popup or null if popup not visible */
    public void setData(DataType mData) {
        this.mData = mData;
    }

    /** owner of popup-view or null if popup not visible */
    public MapView getMapView() {
        return mMapView;
    }
}
