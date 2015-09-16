/*
 * Copyright (c) 2015 by k3b.
 *
 * This file is part of AndroFotoFinder.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
 
package de.k3b.android.androFotoFinder.locationmap;


import android.app.Activity;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;


import org.osmdroid.ResourceProxy;
import org.osmdroid.api.*;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayManager;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import de.k3b.android.androFotoFinder.FotoGalleryActivity;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.QueryParameterParcelable;
import de.k3b.android.osmdroid.DefaultResourceProxyImplEx;
import de.k3b.android.osmdroid.FolderOverlay;
import de.k3b.android.osmdroid.GuestureOverlay;
import de.k3b.android.osmdroid.IconOverlay;
import de.k3b.android.osmdroid.MarkerBase;
import de.k3b.android.osmdroid.ZoomUtil;
import de.k3b.database.SelectedItems;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoUri;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.GeoRectangle;
import de.k3b.io.IGeoRectangle;

/**
 * A fragment to display Foto locations in a geofrafic map.
 * A location-area can be picked for filtering.
 * A simple {@link Fragment} subclass.
 */
public class LocationMapFragment extends DialogFragment {
    protected static final int NO_MARKER_ID = -1;

    public String STATE_LAST_VIEWPORT = "LAST_VIEWPORT";
    private static final int NO_ZOOM = ZoomUtil.NO_ZOOM;
    // for debugging
    private static int sId = 1;
    private final String mDebugPrefix;
    protected final GeoUri mGeoUriEngine = new GeoUri(GeoUri.OPT_DEFAULT);

    protected MapView mMapView;
    private SeekBar mZoomBar;
    private ImageView mImage;
    private DefaultResourceProxyImplEx mResourceProxy;

    /** contain the markers with itmen-count that gets recalculated on every map move/zoom */
    private FolderOverlay mFolderOverlaySummaryMarker;
    private FolderOverlay mFolderOverlaySelectionMarker;

    // handling current selection
    protected IconOverlay mCurrrentSelectionMarker = null;
    protected int mMarkerId = -1;

    // api to fragment owner
    protected OnDirectoryInteractionListener mDirectoryListener;



    /**
     * setCenterZoom does not work in onCreate() because getHeight() and getWidth() are not calculated yet and return 0;
     * setCenterZoom must be set later when getHeight() and getWith() are known (i.e. in onWindowFocusChanged()).
     * <p/>
     * see http://stackoverflow.com/questions/10411975/how-to-get-the-width-and-height-of-an-image-view-in-android/10412209#10412209
     */
    private BoundingBoxE6 mDelayedZoomToBoundingBox = null;
    private int mDelayedZoomLevel = NO_ZOOM;
    private boolean mIsInitialized = false;

    private GalleryFilterParameter mRootFilter;

    public LocationMapFragment() {
        // Required empty public constructor
        mDebugPrefix = "LocationMapFragment#" + (sId++)  + " ";
        Global.debugMemory(mDebugPrefix, "ctor");
        // Required empty public constructor
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + "()");
        }
    }

    @Override public void onDestroy() {
        saveLastViewPort(null);
        if (mCurrentSummaryMarkerLoader != null) mCurrentSummaryMarkerLoader.cancel(false);
        mCurrentSummaryMarkerLoader = null;

        if (mMarkerRecycler != null) mMarkerRecycler.empty();
        super.onDestroy();
        // RefWatcher refWatcher = AndroFotoFinderApp.getRefWatcher(getActivity());
        // refWatcher.watch(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            if (activity instanceof  OnDirectoryInteractionListener) {
                mDirectoryListener = (OnDirectoryInteractionListener) activity;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnDirectoryInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mDirectoryListener = null;
    }

    /** on ratation save current selelected view port */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        saveLastViewPort(savedInstanceState);
    }

    public String getCurrentGeoUri() {
        BoundingBoxE6 currentViewPort = this.mMapView.getBoundingBox();

        GeoPoint currentCenter = currentViewPort.getCenter();
        int currentZoomLevel = this.mMapView.getZoomLevel();
        String uriCurrentViewport = mGeoUriEngine.toUriString(currentCenter.getLatitude(), currentCenter.getLongitude(), currentZoomLevel);
        return uriCurrentViewport;
    }

    private void saveLastViewPort(Bundle savedInstanceState) {
        BoundingBoxE6 currentViewPort = this.mMapView.getBoundingBox();

        if (savedInstanceState != null) {
            savedInstanceState.putParcelable(STATE_LAST_VIEWPORT, currentViewPort);
        }

        String uriCurrentViewport = getCurrentGeoUri();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor edit = sharedPref.edit();

        edit.putString(STATE_LAST_VIEWPORT, uriCurrentViewport);

        edit.commit();

        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + "saveLastViewPort: " + uriCurrentViewport);
        }
    }

    @NonNull
    private IGeoPointInfo loadLastViewPort() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        final String defaultValue = "geo:49,10?z=2";
        String uri = sharedPref.getString(STATE_LAST_VIEWPORT, defaultValue);
        IGeoPointInfo result = mGeoUriEngine.fromUri(uri);

        if ((result == null) || Double.isNaN(result.getLongitude()) || (NO_ZOOM == result.getZoomMin())) {
            result = mGeoUriEngine.fromUri(defaultValue);
        }

        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + "loadLastViewPort: " + uri);
        }

        return result;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        int zoomLevel = NO_ZOOM;
        BoundingBoxE6 boundingBoxE6 = null;
        /** after ratation restore selelected view port */
        if (savedInstanceState != null) {
            boundingBoxE6 =  savedInstanceState.getParcelable(STATE_LAST_VIEWPORT);
        }
        if (boundingBoxE6 == null) {
            // if not initialized from outside show last used value
            IGeoPointInfo rectangle = loadLastViewPort();
            zoomLevel = rectangle.getZoomMin();

            boundingBoxE6 = new BoundingBoxE6(
                    rectangle.getLatitude(),
                    rectangle.getLongitude(),
                    rectangle.getLatitude(),
                    rectangle.getLongitude());
        }
        zoomToBoundingBox(boundingBoxE6 , zoomLevel);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_location_map, container, false);

        mMapView = (MapView) view.findViewById(R.id.mapview);
        this.mImage = (ImageView) view.findViewById(R.id.image);
        this.mImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideImage();
            }
        });
        createZoomBar(view);
        mMapView.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                reloadSummaryMarker();
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                mZoomBar.setProgress(mMapView.getZoomLevel());

                reloadSummaryMarker();
                return false;
            }
        });

        mResourceProxy = new DefaultResourceProxyImplEx(getActivity().getApplicationContext());

        definteOverlays(mMapView, mResourceProxy);


        // mFolderOverlay.add(createMarker(mMapView, ...));

        if (getShowsDialog()) {
            defineButtons(view);

            String title = getActivity().getString(
                    R.string.action_area_title);
            getDialog().setTitle(title);

        }


        mMapView.addOnFirstLayoutListener(new MapView.OnFirstLayoutListener() {
            @Override
            public void onFirstLayout(View v, int left, int top, int right, int bottom) {
                mIsInitialized = true;
                zoomToBoundingBox(mDelayedZoomToBoundingBox, mDelayedZoomLevel);
                mDelayedZoomToBoundingBox = null;
                mDelayedZoomLevel = NO_ZOOM;
            }
        });

        reloadSelectionMarker();
        return view;
    }

    protected void hideImage() {
        mImage.setVisibility(View.GONE);
    }

    protected void definteOverlays(MapView mapView, DefaultResourceProxyImplEx resourceProxy) {
        final List<Overlay> overlays = mapView.getOverlays();

        this.mCurrrentSelectionMarker = createSelectedItemOverlay(resourceProxy);

        this.mSelectionMarker = getActivity().getResources().getDrawable(R.drawable.marker_blue);
        mFolderOverlaySummaryMarker = createFolderOverlay(overlays);

        mFolderOverlaySelectionMarker = createFolderOverlay(overlays);

        overlays.add(new GuestureOverlay(getActivity()));

        mapView.setMultiTouchControls(true);
    }

    protected IconOverlay createSelectedItemOverlay(DefaultResourceProxyImplEx resourceProxy) {
        Drawable currrentSelectionIcon = getActivity().getResources().getDrawable(R.drawable.marker_red);
        // fixed positon, not updated on pick
        return new IconOverlay(resourceProxy, null, currrentSelectionIcon);
    }

    protected void defineButtons(View view) {
        Button cmdCancel = (Button) view.findViewById(R.id.cmd_cancel);
        if (cmdCancel != null) {
            // only available on tablets.
            // on small screen it would block the zoom out button
            cmdCancel.setVisibility(View.VISIBLE);
            cmdCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onCancel();
                }
            });
        }

        Button cmdOk = (Button) view.findViewById(R.id.ok);
        cmdOk.setVisibility(View.VISIBLE);
        cmdOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOk();
            }
        });
    }

    protected void onCancel() {
        dismiss();
    }

    protected void onOk() {
        if (mDirectoryListener != null) {
            IGeoRectangle result = getGeoRectangle(mMapView.getBoundingBox());
            if (Global.debugEnabled) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + "onOk: " + result);
            }

            mDirectoryListener.onDirectoryPick(result.toString(), FotoSql.QUERY_TYPE_GROUP_PLACE_MAP);
            dismiss();
        }
    }

    private void createZoomBar(View view) {
        mMapView.setBuiltInZoomControls(true);

        mZoomBar = (SeekBar) view.findViewById(R.id.zoomBar);

        mZoomBar.setMax(mMapView.getMaxZoomLevel() - mMapView.getMinZoomLevel());
        mZoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    mMapView.getController().setZoom(progress - mMapView.getMinZoomLevel());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    private FolderOverlay createFolderOverlay(List<Overlay> overlays) {
        FolderOverlay result = new FolderOverlay(this.getActivity());
        overlays.add(result);

        return result;
    }

    public void defineNavigation(GalleryFilterParameter rootFilter, GeoRectangle rectangle, int zoomlevel, SelectedItems selectedItems) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + "defineNavigation: " + rectangle + ";z=" + zoomlevel);
        }

        if (rootFilter != null) {
            this.mRootFilter = rootFilter;
        }

        if ((selectedItems != null) && (this.mSelectedItems != selectedItems)) {
            this.mSelectedItems = selectedItems;
            reloadSelectionMarker();
        }

        if (rectangle != null) {
            if (!Double.isNaN(rectangle.getLatitudeMin())) {
                BoundingBoxE6 boundingBox = new BoundingBoxE6(
                        rectangle.getLatitudeMax(),
                        rectangle.getLogituedMin(),
                        rectangle.getLatitudeMin(),
                        rectangle.getLogituedMax());

                zoomToBoundingBox(boundingBox, zoomlevel);
            }
        }

        if (rootFilter != null) {
            reloadSummaryMarker();
        }
    }

    private void zoomToBoundingBox(BoundingBoxE6 boundingBox, int zoomLevel) {
        if (boundingBox != null) {
            if (mIsInitialized) {
                // if map is already initialized
                GeoPoint min = new GeoPoint(boundingBox.getLatSouthE6(), boundingBox.getLonWestE6());

                if (zoomLevel != NO_ZOOM) {
                    ZoomUtil.zoomTo(this.mMapView, zoomLevel, min, null);
                } else {
                    GeoPoint max = new GeoPoint(boundingBox.getLatNorthE6(), boundingBox.getLonEastE6());
                    ZoomUtil.zoomTo(this.mMapView, ZoomUtil.NO_ZOOM, min, max);
                    // this.mMapView.zoomToBoundingBox(boundingBox); this is to inexact
                }
                if (Global.debugEnabled) {
                    Log.i(Global.LOG_CONTEXT, mDebugPrefix + "zoomToBoundingBox(" + boundingBox
                            + ") => " + mMapView.getBoundingBox() + "; z=" + mMapView.getZoomLevel());
                }
            } else {
                // map not initialized yet. do it later.
                this.mDelayedZoomToBoundingBox = boundingBox;
                this.mDelayedZoomLevel = zoomLevel;
            }
        }
    }

    /** all marker clicks will be delegated to LocationMapFragment#onMarkerClicked() */
    private class FotoMarker extends MarkerBase<Object> {

        public FotoMarker(ResourceProxy pResourceProxy) {
            super(pResourceProxy);
        }

        /**
         * @return true if click was handeled.
         */
        @Override
        protected boolean onMarkerClicked(MapView mapView, int markerId, IGeoPoint makerPosition, Object markerData) {
            return LocationMapFragment.this.onMarkerClicked(this, markerId, makerPosition, markerData);
        }

        /**
         * @return true if click was handeled.
         */
        @Override
        protected boolean onMarkerLongPress(MapView mapView, int markerId, IGeoPoint geoPosition, Object data) {
            return LocationMapFragment.this.onMarkerLongPress(this, markerId, geoPosition, data);
        }

    }

    private void reloadSummaryMarker() {
        if (mIsInitialized) {
            // initialized
            if (mCurrentSummaryMarkerLoader == null) {
                // not active yet
                List<Overlay> oldItems = mFolderOverlaySummaryMarker.getItems();

                mLastZoom = this.mMapView.getZoomLevel();
                double groupingFactor = getGroupingFactor(mLastZoom);
                BoundingBoxE6 world = this.mMapView.getBoundingBox();

                reloadSummaryMarker(world, groupingFactor, oldItems);
            } else {
                mSummaryMarkerPendingLoads++;
            }
        }
    }

    private void reloadSummaryMarker(BoundingBoxE6 latLonArea, double groupingFactor, List<Overlay> oldItems) {
        QueryParameterParcelable query = FotoSql.getQueryGroupByPlace(groupingFactor);
        query.clearWhere();

        if (this.mRootFilter != null) {
            FotoSql.setWhereFilter(query, this.mRootFilter);
        }

        // delta: make the grouping area a little bit bigger than the viewport
        // so that counts at the borders are correct.
        double delta = (groupingFactor > 0) ? (2.0 / groupingFactor) : 0.0;
        IGeoRectangle rect = getGeoRectangle(latLonArea);
        FotoSql.addWhereFilterLatLon(query
                , rect.getLatitudeMin() - delta
                , rect.getLatitudeMax() + delta
                , rect.getLogituedMin() - delta
                , rect.getLogituedMax() + delta);

        mCurrentSummaryMarkerLoader = new SummaryMarkerLoaderTask(createHashMap(oldItems));
        mCurrentSummaryMarkerLoader.execute(query);
    }

    private IGeoRectangle getGeoRectangle(BoundingBoxE6 boundingBox) {
        GeoRectangle result = new GeoRectangle();
        result.setLatitude(boundingBox.getLatSouthE6() * 1E-6, boundingBox.getLatNorthE6() * 1E-6);
        result.setLogitude(boundingBox.getLonWestE6() * 1E-6, boundingBox.getLonEastE6() * 1E-6);

        return result;
    }

    /** translates map-zoomlevel to groupfactor
     * that tells sql how geo-points are grouped together.
     */
    private double getGroupingFactor(int zoomlevel) {
        // todo
        return FotoSql.getGroupFactor(zoomlevel);
    }

    // for debugginc
    private static int sInstanceCountFotoLoader = 1;

    /** caching support: if zoom level changes the cached items become invalid
     * because the marker clustering is different */
    private int mLastZoom = NO_ZOOM;

    /** how much mCurrentSummaryMarkerLoader are tirggerd while task is loading */
    private int mSummaryMarkerPendingLoads = 0;

    /** The factory LocationMapFragment.SummaryMarkerLoaderTask#createMarker() tries to recycle old
     *     unused Fotomarkers before creating new */
    private Stack<FotoMarker> mMarkerRecycler = new Stack<FotoMarker>();

    /** To allow canceling of loading task. There are 0 or one tasks running at a time */
    private SummaryMarkerLoaderTask mCurrentSummaryMarkerLoader = null;

    /** to load summary marker with numbers in the icons */
    private class SummaryMarkerLoaderTask extends MarkerLoaderTaskWithRecycling<FotoMarker> {
        public SummaryMarkerLoaderTask(HashMap<Integer, FotoMarker> oldItems) {
            super(getActivity(), LocationMapFragment.this.mDebugPrefix + "-SummaryMarkerLoaderTask#" + (sInstanceCountFotoLoader++) + "-",
                    mMarkerRecycler, oldItems, NO_MARKER_COUNT_LIMIT);
        }

        @NonNull
        protected FotoMarker createNewMarker() {
            return new FotoMarker(mResourceProxy);
        }

        // This is called when doInBackground() is finished
        @Override
        protected void onPostExecute(OverlayManager result) {
            boolean zoomLevelChanged = mMapView.getZoomLevel() != mLastZoom;

            if (isCancelled()) {
                onLoadFinishedSummaryMarker(null, zoomLevelChanged);
            } else {
                onLoadFinishedSummaryMarker(result, zoomLevelChanged);

                recyleItems(zoomLevelChanged, mOldItems);
            }

            mOldItems.clear();
            mOldItems = null;
            int recyclerSize = mMarkerRecycler.size();
            if (mStatus != null) {
                mStatus.append("\n\tRecycler: ").append(mRecyclerSizeBefore).append(",")
                        .append(mRecyclerSizeAfter).append(",").append(recyclerSize)
                        .append("\n\t").append(mMapView.getBoundingBox())
                        .append(", z= ").append(mMapView.getZoomLevel())
                        .append("\n\tPendingLoads: ").append(mSummaryMarkerPendingLoads);
                if (Global.debugEnabledSql) {
                    Log.w(Global.LOG_CONTEXT, mDebugPrefix + mStatus);
                } else {
                    Log.i(Global.LOG_CONTEXT, mDebugPrefix + mStatus);
                }
            }

            // in the meantime the mapview has moved: must recalculate again.
            mCurrentSummaryMarkerLoader = null;
            if (mSummaryMarkerPendingLoads > 0) {
                mSummaryMarkerPendingLoads = 0;
                reloadSummaryMarker();
            }
        }

    } // class SummaryMarkerLoaderTask

    /** gets called when SummaryMarkerLoaderTask has finished.
     *
     * @param result null if there was an error
     * @param zoomLevelChanged
     */
    private void onLoadFinishedSummaryMarker(OverlayManager result, boolean zoomLevelChanged) {
        StringBuilder dbg = (Global.debugEnabledSql || Global.debugEnabled) ? new StringBuilder() : null;
        if (dbg != null) {
            int found = (result != null) ? result.size() : 0;
            dbg.append(mDebugPrefix).append("onLoadFinishedSummaryMarker() markers created: ").append(found).append(". ");
        }

        if (result != null) {
            OverlayManager old = mFolderOverlaySummaryMarker.setOverlayManager(result);
            if (old != null) {
                if (dbg != null) {
                    dbg.append(mDebugPrefix).append(" previous : : ").append(old.size());
                }
                if (zoomLevelChanged) {
                    if (dbg != null) dbg
                            .append(" zoomLevelChanged - recycling : ")
                            .append(old.size())
                            .append(" items");

                    for (Overlay item : old) {
                        mMarkerRecycler.add((FotoMarker) item);
                    }
                }
                old.onDetach(this.mMapView);
                old.clear();
            }
            this.mMapView.invalidate();
        }
        if (dbg != null) {
            Log.d(Global.LOG_CONTEXT, dbg.toString());
        }
    }

    /**
     * @return true if click was handeled.
     */
    protected boolean onMarkerClicked(IconOverlay marker, int markerId, IGeoPoint geoPosition, Object markerData) {
        this.mImage.setImageBitmap(getBitmap(markerId));
        this.mImage.setVisibility(View.VISIBLE);

        updateMarker(marker, markerId, geoPosition, markerData);

        return true; // TODO
    }

    protected boolean onMarkerLongPress(IconOverlay marker, int markerId, IGeoPoint geoPosition, Object markerData) {
        onMarkerClicked(marker, markerId, geoPosition, markerData);
        return showContextMenu(this.mMapView, markerId, geoPosition, markerData);
    }

    protected void updateMarker(IconOverlay marker, int markerId, IGeoPoint makerPosition, Object markerData) {
        mMarkerId = markerId;
        if (mCurrrentSelectionMarker != null) {
            mMapView.getOverlays().remove(mCurrrentSelectionMarker);
            mCurrrentSelectionMarker.moveTo(makerPosition, mMapView);
            mMapView.getOverlays().add(mCurrrentSelectionMarker);
        }
    }


    private Bitmap getBitmap(int id) {
        final Bitmap thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                getActivity().getContentResolver(),
                id,
                MediaStore.Images.Thumbnails.MICRO_KIND,
                new BitmapFactory.Options());

        return thumbnail;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnDirectoryInteractionListener {
        /** called when upresses "OK" button */
        void onDirectoryPick(String selectedAbsolutePath, int queryTypeId);
    }

    /**************************** Support for non-clustered selected items ********************/

    private SelectedItems mSelectedItems = null;

    /** To allow canceling of loading task. There are 0 or one tasks running at a time */
    private SelectionMarkerLoaderTask mCurrentSelectionMarkerLoader = null;

    private Drawable mSelectionMarker;

    /** to load markers for current selected items */
    private class SelectionMarkerLoaderTask extends MarkerLoaderTaskWithRecycling<FotoMarker> {
        public SelectionMarkerLoaderTask(HashMap<Integer, FotoMarker> oldItems) {
            super(getActivity(), LocationMapFragment.this.mDebugPrefix + "-SelectionMarkerLoaderTask#" + (sInstanceCountFotoLoader++) + "-", mMarkerRecycler,
                    oldItems, Global.maxSelectionMarkersInMap);
        }

        @NonNull
        protected FotoMarker createNewMarker() {
            return new FotoMarker(mResourceProxy);
        }

        // This is called when doInBackground() is finished
        @Override
        protected void onPostExecute(OverlayManager result) {
            if (isCancelled()) {
                onLoadFinishedSelection(null);
            } else {
                onLoadFinishedSelection(result);

                recyleItems(false, mOldItems);
            }

            mOldItems.clear();
            mOldItems = null;
            int recyclerSize = mMarkerRecycler.size();
            if (mStatus != null) {
                mStatus.append("\n\tRecycler: ").append(mRecyclerSizeBefore).append(",")
                        .append(mRecyclerSizeAfter).append(",").append(recyclerSize);
                if (Global.debugEnabledSql) {
                    Log.w(Global.LOG_CONTEXT, mDebugPrefix + mStatus);
                } else {
                    Log.i(Global.LOG_CONTEXT, mDebugPrefix + mStatus);
                }
            }
        }

        protected BitmapDrawable createIcon(String iconText) {
            return (BitmapDrawable) mSelectionMarker;
        }

    } // class SelectionMarkerLoaderTask

    private void reloadSelectionMarker() {
        if ((mFolderOverlaySelectionMarker != null) &&
                (mSelectedItems != null) && (!mSelectedItems.isEmpty())) {
            if (mCurrentSelectionMarkerLoader != null) {
                mCurrentSelectionMarkerLoader.cancel(false);
                mCurrentSelectionMarkerLoader = null;
            }

            List<Overlay> oldItems = mFolderOverlaySelectionMarker.getItems();

            QueryParameterParcelable query = new QueryParameterParcelable(FotoSql.queryGps);
            FotoSql.setWhereSelection(query, mSelectedItems);
            FotoSql.addWhereLatLonNotNull(query);

            mCurrentSelectionMarkerLoader = new SelectionMarkerLoaderTask(createHashMap(oldItems));
            mCurrentSelectionMarkerLoader.execute(query);
        }
    }

    @NonNull
    private HashMap<Integer, FotoMarker> createHashMap(List<Overlay> oldItems) {
        HashMap<Integer, FotoMarker> oldItemsHash = new HashMap<Integer, FotoMarker>();
        for (Overlay o : oldItems) {
            FotoMarker marker = (FotoMarker) o;
            oldItemsHash.put(marker.getID(), marker);
        }
        return oldItemsHash;
    }

    /** gets called when MarkerLoaderTask has finished.
     *
     * @param result null if there was an error
     */
    protected void onLoadFinishedSelection(OverlayManager result) {
        mCurrentSelectionMarkerLoader = null;
        StringBuilder dbg = (Global.debugEnabledSql || Global.debugEnabled) ? new StringBuilder() : null;
        if (dbg != null) {
            int found = (result != null) ? result.size() : 0;
            dbg.append(mDebugPrefix).append("onLoadFinishedSelection() markers created: ").append(found);
        }

        if (result != null) {
            OverlayManager old = mFolderOverlaySelectionMarker.setOverlayManager(result);
            if (old != null) {
                if (dbg != null) {
                    dbg.append(mDebugPrefix).append(" previous : : ").append(old.size());
                }
                old.onDetach(this.mMapView);
                old.clear();
            }
            this.mMapView.invalidate();
        }
        if (dbg != null) {
            Log.d(Global.LOG_CONTEXT, dbg.toString());
        }
    }

    protected   boolean showContextMenu(final View parent, final int markerId,
                                        final IGeoPoint geoPosition, final Object markerData) {
        MenuInflater inflater = getActivity().getMenuInflater();
        PopupMenu menu = new PopupMenu(getActivity(), this.mImage);

        inflater.inflate(R.menu.menu_map_context, menu.getMenu());

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.cmd_gallery:
                        return showGallery(getiGeoPointById(markerId, geoPosition));
                    case R.id.cmd_zoom:
                        return zoomToFit(getiGeoPointById(markerId, geoPosition));
                    default:
                        return false;
                }
            }
        });
        menu.show();
        return true;
    }

    private boolean showGallery(IGeoPoint geoPosition) {
        GalleryFilterParameter filter = getMarkerFilter(geoPosition);
        FotoGalleryActivity.showActivity(this.getActivity(), filter, 0);
        return true;
    }

    private boolean zoomToFit(IGeoPoint geoPosition) {
        BoundingBoxE6 boundingBoxE6 = null;

        IGeoRectangle fittingRectangle = FotoSql.execGetGeoRectangle(this.getActivity(), getMarkerFilter(geoPosition), null);
        double delta = getDelta(fittingRectangle);
        if (delta < 1e-6) {
            boundingBoxE6 = getMarkerBoundingBox(geoPosition);

        } else {
            double enlarge = delta * 0.2;
            boundingBoxE6 = new BoundingBoxE6(
                    fittingRectangle.getLatitudeMax()+enlarge,
                    fittingRectangle.getLogituedMax()+enlarge,
                    fittingRectangle.getLatitudeMin()-enlarge,
                    fittingRectangle.getLogituedMin()-enlarge);
        }
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, "zoomToFit(): " + fittingRectangle +
                    " delta " + delta +
                    " => box " + boundingBoxE6);
        }
        zoomToBoundingBox(boundingBoxE6, NO_ZOOM);
        return true;
    }

    private double getDelta(IGeoRectangle fittingRectangle) {
        if (fittingRectangle == null) return 0;

        return Math.max(Math.abs(fittingRectangle.getLogituedMax() - fittingRectangle.getLogituedMin())
                , Math.abs(fittingRectangle.getLatitudeMax() - fittingRectangle.getLatitudeMin()));
    }

    private GalleryFilterParameter getMarkerFilter(IGeoPoint geoPosition) {
        double delta = getMarkerDelta();

        GalleryFilterParameter result = new GalleryFilterParameter().get(mRootFilter);
        result.setLatitude(geoPosition.getLatitude() - delta, geoPosition.getLatitude() + delta);
        result.setLogitude(geoPosition.getLongitude() - delta, geoPosition.getLongitude() + delta);

        return result;
    }

    @NonNull
    private BoundingBoxE6 getMarkerBoundingBox(IGeoPoint geoPosition) {
        double delta = getMarkerDelta();

        return new BoundingBoxE6(
                geoPosition.getLatitude()+delta,
                geoPosition.getLongitude()+delta,
                geoPosition.getLatitude()-delta,
                geoPosition.getLongitude()-delta);
    }

    private double getMarkerDelta() {
        int zoomLevel = this.mMapView.getZoomLevel();
        double groupingFactor = getGroupingFactor(zoomLevel);
        return 1/groupingFactor/2;
    }

    private IGeoPoint getiGeoPointById(int markerId, IGeoPoint notFoundValue) {
        if (markerId != NO_MARKER_ID) {
            IGeoPoint pos = FotoSql.execGetPosition(this.getActivity(), markerId);
            if (pos != null) {
                notFoundValue = pos;
            }
        }
        return notFoundValue;
    }

}
