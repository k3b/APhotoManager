/*
 * Copyright (c) 2015-2016 by k3b.
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceManager;
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
import org.osmdroid.events.DelayedMapListener;
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
import de.k3b.android.androFotoFinder.ThumbNailUtils;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailActivityViewPager;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.osmdroid.DefaultResourceProxyImplEx;
import de.k3b.android.osmdroid.FolderOverlay;
import de.k3b.android.osmdroid.GuestureOverlay;
import de.k3b.android.osmdroid.IconOverlay;
import de.k3b.android.osmdroid.ClickableIconOverlay;
import de.k3b.android.osmdroid.ZoomUtil;
import de.k3b.android.osmdroid.forge.MapsForgeSupport;
import de.k3b.android.util.IntentUtil;
import de.k3b.database.QueryParameter;
import de.k3b.database.SelectedItems;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoUri;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.GeoRectangle;
import de.k3b.io.IGalleryFilter;
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

    /** If there is more than 200 millisecs no zoom/scroll update markers */
    protected static final int DEFAULT_INACTIVITY_DELAY_IN_MILLISECS = 200;

    // for debugging
    private static int sId = 1;
    private final String mDebugPrefix;
    protected final GeoUri mGeoUriEngine = new GeoUri(GeoUri.OPT_DEFAULT);

    protected MapView mMapView;
    private SeekBar mZoomBar;
    private ImageView mImage;
    private DefaultResourceProxyImplEx mResourceProxy;

    /** temporary 1x1 pix view where popup-menu is attached to */
    private View mTempPopupMenuParentView = null;

    /** contain the markers with itmen-count that gets recalculated on every map move/zoom */
    private FolderOverlay mFolderOverlayGreenSummaryMarker;
    private FolderOverlay mFolderOverlayBlueSelectionMarker;

    // handling current selection
    protected IconOverlay mCurrrentRedSelectionMarker = null;
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

    private IGalleryFilter mRootFilter;

    public LocationMapFragment() {
        // Required empty public constructor
        mDebugPrefix = "LocationMapFragment#" + (sId++)  + " ";
        Global.debugMemory(mDebugPrefix, "ctor");
        // Required empty public constructor
        if (Global.debugEnabled || Global.debugEnabledMap) {
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
        String uriCurrentViewport = mGeoUriEngine.toUriString(
                new GeoPointDto(currentCenter.getLatitude(), currentCenter.getLongitude()
                        , currentZoomLevel));
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

        edit.apply();

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
        zoomToBoundingBox("onCreateView", boundingBoxE6 , zoomLevel);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_location_map, container, false);

        mMapView = (MapView) view.findViewById(R.id.mapview);

        if ((Global.mapsForgeDir == null) || (!Global.mapsForgeDir.exists()) || (!Global.mapsForgeDir.isDirectory())) {
            Global.mapsForgeEnabled = false;
        }

        if (Global.mapsForgeEnabled) {
            MapsForgeSupport.load(getActivity(), mMapView, Global.mapsForgeDir);
        }

        this.mImage = (ImageView) view.findViewById(R.id.image);
        this.mImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideImage();
            }
        });
        createZoomBar(view);
        mMapView.setMapListener(new DelayedMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                reloadSummaryMarker("onScroll");
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                int zoomLevel1 = mMapView.getZoomLevel();
                mZoomBar.setProgress(zoomLevel1);

                reloadSummaryMarker("onZoom " + zoomLevel1);
                return false;
            }
        }, DEFAULT_INACTIVITY_DELAY_IN_MILLISECS));

        mResourceProxy = new DefaultResourceProxyImplEx(getActivity().getApplicationContext());

        definteOverlays(mMapView, mResourceProxy);


        // mFolderOverlay.add(createMarker(mMapView, ...));

        if (getShowsDialog()) {
            defineButtons(view);

            String title = getActivity().getString(
                    R.string.area_menu_title);
            getDialog().setTitle(title);

        }


        mMapView.addOnFirstLayoutListener(new MapView.OnFirstLayoutListener() {
            @Override
            public void onFirstLayout(View v, int left, int top, int right, int bottom) {
                mIsInitialized = true;
                zoomToBoundingBox("onFirstLayout", mDelayedZoomToBoundingBox, mDelayedZoomLevel);
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

        this.mCurrrentRedSelectionMarker = createSelectedItemOverlay(resourceProxy);

        this.mSelectionMarker = getActivity().getResources().getDrawable(R.drawable.marker_blue);
        mFolderOverlayGreenSummaryMarker = createFolderOverlay(overlays);

        mFolderOverlayBlueSelectionMarker = createFolderOverlay(overlays);

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
                /* change ignored */
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                /* change ignored */
            }
        });
    }


    private FolderOverlay createFolderOverlay(List<Overlay> overlays) {
        FolderOverlay result = new FolderOverlay(this.getActivity());
        overlays.add(result);

        return result;
    }

    public void defineNavigation(IGalleryFilter rootFilter, GeoRectangle rectangle, int zoomlevel, SelectedItems selectedItems) {
        if (Global.debugEnabled || Global.debugEnabledMap) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + "defineNavigation: " + rectangle + ";z=" + zoomlevel);
        }

        if (rootFilter != null) {
            this.mRootFilter = rootFilter;
        }

        if ((selectedItems != null) && (this.mSelectedItems != selectedItems)) {
            this.mSelectedItems = selectedItems;
            reloadSelectionMarker();
        }

        if ((rectangle != null) && !Double.isNaN(rectangle.getLatitudeMin())) {
            BoundingBoxE6 boundingBox = new BoundingBoxE6(
                    rectangle.getLatitudeMax(),
                    rectangle.getLogituedMin(),
                    rectangle.getLatitudeMin(),
                    rectangle.getLogituedMax());

            zoomToBoundingBox("defineNavigation", boundingBox, zoomlevel);
        }

        if (rootFilter != null) {
            reloadSummaryMarker("defineNavigation");
        }
    }

    private void zoomToBoundingBox(String why, BoundingBoxE6 boundingBox, int zoomLevel) {
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
                if (Global.debugEnabledMap) {
                    Log.i(Global.LOG_CONTEXT, mDebugPrefix
                            + "zoomToBoundingBox(" + why
                            + "; z=" + mMapView.getZoomLevel()
                            + ") :"
                            + boundingBox
                            + " <= "
                            + mMapView.getBoundingBox()
                            );
                }
            } else {
                // map not initialized yet. do it later.
                this.mDelayedZoomToBoundingBox = boundingBox;
                this.mDelayedZoomLevel = zoomLevel;
            }
        }
    }

    /** all marker clicks will be delegated to LocationMapFragment#onMarkerClicked() */
    private class FotoMarker extends ClickableIconOverlay<Object> {

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

    private void reloadSummaryMarker(String why) {
        if (mIsInitialized) {
            // initialized
            if (mCurrentSummaryMarkerLoader == null) {
                // not active yet
                List<Overlay> oldItems = mFolderOverlayGreenSummaryMarker.getItems();

                mLastZoom = this.mMapView.getZoomLevel();
                double groupingFactor = getGroupingFactor(mLastZoom);
                BoundingBoxE6 world = this.mMapView.getBoundingBox();
                if (Global.debugEnabledMap) {
                    Log.d(Global.LOG_CONTEXT, mDebugPrefix + "reloadSummaryMarker(" + why + ")"
                            + world + ", zoom " + mLastZoom);
                }

                reloadSummaryMarker(world, groupingFactor, oldItems);
            } else {
                // background load is already active. Remember that at least one scroll/zoom was missing
                mSummaryMarkerPendingLoads++;
            }
        }
    }

    private void reloadSummaryMarker(BoundingBoxE6 latLonArea, double groupingFactor, List<Overlay> oldItems) {
        QueryParameter query = FotoSql.getQueryGroupByPlace(groupingFactor);
        query.clearWhere();

        if (this.mRootFilter != null) {
            FotoSql.setWhereFilter(query, this.mRootFilter, true);
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
                String why = (Global.debugEnabledMap) ? ("Lost zoom/scroll event(s): " + mSummaryMarkerPendingLoads) : null;
                mSummaryMarkerPendingLoads = 0;
                reloadSummaryMarker(why);
            }
        }

    } // class SummaryMarkerLoaderTask

    /** gets called when SummaryMarkerLoaderTask has finished.
     *
     * @param newSummaryIcons null if there was an error
     */
    private void onLoadFinishedSummaryMarker(OverlayManager newSummaryIcons, boolean zoomLevelChanged) {
        StringBuilder dbg = (Global.debugEnabledSql || Global.debugEnabledMap) ? new StringBuilder() : null;
        if (dbg != null) {
            int found = (newSummaryIcons != null) ? newSummaryIcons.size() : 0;
            dbg.append(mDebugPrefix).append("onLoadFinishedSummaryMarker() markers created: ").append(found).append(". ");
        }

        if (newSummaryIcons != null) {
            OverlayManager oldSummaryIcons = mFolderOverlayGreenSummaryMarker.setOverlayManager(newSummaryIcons);
            if (oldSummaryIcons != null) {
                if (dbg != null) {
                    dbg.append(mDebugPrefix).append(" previous : : ").append(oldSummaryIcons.size());
                }
                if (zoomLevelChanged) {
                    if (dbg != null) dbg
                            .append(" zoomLevelChanged - recycling : ")
                            .append(oldSummaryIcons.size())
                            .append(" items");

                    for (Overlay item : oldSummaryIcons) {
                        mMarkerRecycler.add((FotoMarker) item);
                    }
                }
                oldSummaryIcons.onDetach(this.mMapView);
                oldSummaryIcons.clear();
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

        ThumbNailUtils.getThumb(markerId, mImage);

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
        if (mCurrrentRedSelectionMarker != null) {
            mMapView.getOverlays().remove(mCurrrentRedSelectionMarker);
            mCurrrentRedSelectionMarker.moveTo(makerPosition, mMapView);
            mMapView.getOverlays().add(mCurrrentRedSelectionMarker);
        }
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
        if ((mFolderOverlayBlueSelectionMarker != null) &&
                (mSelectedItems != null) && (!mSelectedItems.isEmpty())) {
            if (mCurrentSelectionMarkerLoader != null) {
                mCurrentSelectionMarkerLoader.cancel(false);
                mCurrentSelectionMarkerLoader = null;
            }

            List<Overlay> oldItems = mFolderOverlayBlueSelectionMarker.getItems();

            QueryParameter query = new QueryParameter(FotoSql.queryGps);
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
        StringBuilder dbg = (Global.debugEnabledSql || Global.debugEnabledMap) ? new StringBuilder() : null;
        if (dbg != null) {
            int found = (result != null) ? result.size() : 0;
            dbg.append(mDebugPrefix).append("onLoadFinishedSelection() markers created: ").append(found);
        }

        if (result != null) {
            OverlayManager old = mFolderOverlayBlueSelectionMarker.setOverlayManager(result);
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
        mMapView.removeView(mTempPopupMenuParentView);

        PopupMenu menu = new PopupMenu(getActivity(), createTempPopupParentMenuView(new GeoPoint(geoPosition.getLatitudeE6(), geoPosition.getLongitudeE6())));

        inflater.inflate(R.menu.menu_map_context, menu.getMenu());

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mMapView.removeView(mTempPopupMenuParentView);

                switch (item.getItemId()) {
                    case R.id.cmd_photo:
                        return showPoto(getGeoPointById(markerId, geoPosition));
                    case R.id.cmd_gallery:
                        return showGallery(getGeoPointById(markerId, geoPosition));
                    case R.id.cmd_zoom:
                        return zoomToFit(getGeoPointById(markerId, geoPosition));

                    case R.id.cmd_show_geo_as: {
                        IGeoPoint _geo = getGeoPointById(markerId, geoPosition);
                        GeoPointDto geo = new GeoPointDto(_geo.getLatitude(), _geo.getLongitude(), GeoPointDto.NO_ZOOM);
                        geo.setId(""+markerId);
                        geo.setName("#"+markerId);
                        GeoUri PARSER = new GeoUri(GeoUri.OPT_PARSE_INFER_MISSING);
                        String uri = PARSER.toUriString(geo);

                        IntentUtil.cmdStartIntent(getActivity(), null, uri, null, Intent.ACTION_VIEW, R.string.geo_show_as_menu_title, R.string.geo_picker_err_not_found);

                        return true;
                    }




                    default:
                        return false;
                }
            }
        });
        menu.show();
        return true;
    }

    // inspired by org.osmdroid.bonuspack.overlays.InfoWindow
    private View createTempPopupParentMenuView(GeoPoint position) {
        if (mTempPopupMenuParentView != null) mMapView.removeView(mTempPopupMenuParentView);
        mTempPopupMenuParentView = new View(getActivity());
        MapView.LayoutParams lp = new MapView.LayoutParams(
                1,
                1,
                position, MapView.LayoutParams.CENTER,
                0, 0);
        mTempPopupMenuParentView.setVisibility(View.VISIBLE);
        mMapView.addView(mTempPopupMenuParentView, lp);
        return mTempPopupMenuParentView;
    }

    private boolean showPoto(IGeoPoint geoPosition) {
        GalleryFilterParameter filter = getMarkerFilter(geoPosition);
        QueryParameter query = new QueryParameter();
        FotoSql.setWhereFilter(query, filter, false);
        FotoSql.setSort(query, FotoSql.SORT_BY_DATE, false);

        ImageDetailActivityViewPager.showActivity(this.getActivity(), null, 0, query);
        return true;
    }

    private boolean showGallery(IGeoPoint geoPosition) {
        GalleryFilterParameter filter = getMarkerFilter(geoPosition);
        FotoGalleryActivity.showActivity(this.getActivity(), filter, null, 0);
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
        if (Global.debugEnabledMap) {
            Log.i(Global.LOG_CONTEXT, "zoomToFit(): " + fittingRectangle +
                    " delta " + delta +
                    " => box " + boundingBoxE6);
        }
        zoomToBoundingBox("zoomToFit()", boundingBoxE6, NO_ZOOM);
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

    private IGeoPoint getGeoPointById(int markerId, IGeoPoint notFoundValue) {
        if (markerId != NO_MARKER_ID) {
            IGeoPoint pos = FotoSql.execGetPosition(this.getActivity(), null, markerId);
            if (pos != null) {
                return pos;
            }
        }
        return notFoundValue;
    }

}
