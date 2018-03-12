/*
 * Copyright (c) 2015-2017 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
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
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
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

import org.osmdroid.api.*;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.*;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import de.k3b.android.androFotoFinder.FotoGalleryActivity;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.LockScreen;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.ThumbNailUtils;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailActivityViewPager;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.android.osmdroid.FolderOverlayEx;
import de.k3b.android.osmdroid.GuestureOverlay;
import de.k3b.android.osmdroid.IconOverlay;
import de.k3b.android.osmdroid.ClickableIconOverlay;
import de.k3b.android.osmdroid.MarkerBubblePopup;
import de.k3b.android.osmdroid.MarkerEx;
import de.k3b.android.osmdroid.OsmdroidUtil;
import de.k3b.android.osmdroid.forge.MapsForgeSupport;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.util.ResourceUtils;
import de.k3b.database.QueryParameter;
import de.k3b.io.collections.SelectedItems;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoInfoHandler;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoUri;
import de.k3b.geo.io.gpx.GpxReaderBase;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.GeoRectangle;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.IGeoRectangle;

/**
 * A fragment to display Foto locations in a geofrafic map.
 * Used as Dialog to pick a location-area for filtering.
 */
public class LocationMapFragment extends DialogFragment {
    protected static final int NO_MARKER_ID = -1;

    protected String STATE_LAST_VIEWPORT = "LAST_VIEWPORT";

    private static final int NO_ZOOM = OsmdroidUtil.NO_ZOOM;

    /** If there is more than 200 millisecs no zoom/scroll update markers */
    protected static final int DEFAULT_INACTIVITY_DELAY_IN_MILLISECS = 200;

    // for debugging
    private static int sId = 1;
    private final String mDebugPrefix;
    protected final GeoUri mGeoUriEngine = new GeoUri(GeoUri.OPT_DEFAULT);

    protected MapView mMapView;
    private SeekBar mZoomBar;
    private ImageView mCurrentPhoto;

    /** temporary 1x1 pix view where popup-menu is attached to */
    private View mTempPopupMenuParentView = null;

    /** contain the markers with itmen-count that gets recalculated on every map move/zoom */
    private FolderOverlayEx mFolderOverlayGreenPhotoMarker;
    private FolderOverlayEx mFolderOverlayBlueSelectionMarker;

    /** Selected items in gallery */
    private FolderOverlayEx mFolderOverlayBlueGpxMarker = null;

    // handling current selection
    protected IconOverlay mCurrrentSelectionRedMarker = null;
    protected int mMarkerId = -1;

    // api to fragment owner
    protected OnDirectoryInteractionListener mDirectoryListener;



    /**
     * setCenterZoom does not work in onCreate() because getHeight() and getWidth() are not calculated yet and return 0;
     * setCenterZoom must be set later when getHeight() and getWith() are known (i.e. in onWindowFocusChanged()).
     * <p/>
     * see http://stackoverflow.com/questions/10411975/how-to-get-the-width-and-height-of-an-image-view-in-android/10412209#10412209
     */
    private BoundingBox mDelayedZoomToBoundingBox = null;
    private int mDelayedZoomLevel = NO_ZOOM;
    private boolean mIsInitialized = false;

    private IGalleryFilter mRootFilter;
    private double mMinZoomLevel;
    private double mMaxZoomLevel;

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
        if (mCurrentFotoMarkerLoader != null) mCurrentFotoMarkerLoader.cancel(false);
        mCurrentFotoMarkerLoader = null;

        if (mFotoMarkerRecycler != null) mFotoMarkerRecycler.empty();
        mSelectedItemsHandler = null;
        super.onDestroy();
        // RefWatcher refWatcher = AndroFotoFinderApp.getRefWatcher(getActivity());
        // refWatcher.watch(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mPopup = new MarkerBubblePopup<GeoPointDtoEx>(activity);
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
        if (mPopup != null) {
            mPopup.close();
        }
        mPopup = null;

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
        BoundingBox currentViewPort = this.mMapView.getBoundingBox();

        GeoPoint currentCenter = currentViewPort.getCenterWithDateLine();
        int currentZoomLevel = (int) this.mMapView.getZoomLevelDouble();
        String uriCurrentViewport = mGeoUriEngine.toUriString(
                new GeoPointDto(currentCenter.getLatitude(), currentCenter.getLongitude()
                        , currentZoomLevel));
        return uriCurrentViewport;
    }

    private void saveLastViewPort(Bundle savedInstanceState) {
        BoundingBox currentViewPort = this.mMapView.getBoundingBox();

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
        BoundingBox boundingBox = null;
        /** after ratation restore selelected view port */
        if (savedInstanceState != null) {
            boundingBox =  savedInstanceState.getParcelable(STATE_LAST_VIEWPORT);
        }
        if (boundingBox == null) {
            // if not initialized from outside show last used value
            IGeoPointInfo rectangle = loadLastViewPort();
            zoomLevel = rectangle.getZoomMin();

            boundingBox = new BoundingBox(
                    rectangle.getLatitude(),
                    rectangle.getLongitude(),
                    rectangle.getLatitude(),
                    rectangle.getLongitude());
        }
        zoomToBoundingBox("onCreateView", boundingBox , zoomLevel);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_location_map, container, false);

        mMapView = (MapView) view.findViewById(R.id.mapview);

        if ((Global.mapsForgeDir == null) || (!Global.mapsForgeDir.exists()) || (!Global.mapsForgeDir.isDirectory())) {
            Global.mapsForgeEnabled = false;
        }

        if (Global.mapsForgeEnabled) {
            MapsForgeSupport.load(getActivity(), mMapView, Global.mapsForgeDir);
        }

        this.mCurrentPhoto = (ImageView) view.findViewById(R.id.image);
        this.mCurrentPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideImage();
            }
        });
        createZoomBar(view);
        mMapView.addMapListener(new DelayedMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                reloadFotoMarker("onScroll");
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                double zoomLevel1 = mMapView.getZoomLevelDouble();
                setZoomBarZoomLevel("onZoom ", zoomLevel1);

                reloadFotoMarker("onZoom " + zoomLevel1);
                return false;
            }
        }, DEFAULT_INACTIVITY_DELAY_IN_MILLISECS));

        definteOverlays(mMapView);


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

        mSelectedItemsHandler.reloadSelectionMarker();
        return view;
    }

    protected void setZoomBarZoomLevel(String why, double zoomLevel) {
        // map: mMinZoomLevel..mMaxZoomLevel
        // mZoomBar: 0..mMaxZoomLevel-mMinZoomLevel
        int newProgress = (int) (zoomLevel - mMinZoomLevel);
        if (newProgress != mZoomBar.getProgress()) {
            // only if changed to avoid zoom events that modify the map
            if (Global.debugEnabledMap) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix
                        + "setZoomBarZoomLevel(" + why
                        + ") :"
                        + newProgress
                        + " (from "
                        + mZoomBar.getProgress()
                        + ") - " + mMinZoomLevel
                        + ".." + zoomLevel
                        + ".." + mMaxZoomLevel

                );

            }
            mZoomBar.setProgress(newProgress);
        }
    }

    protected void hideImage() {
        mCurrentPhoto.setVisibility(View.GONE);
    }

    protected void definteOverlays(MapView mapView) {
        final List<Overlay> overlays = mapView.getOverlays();

        this.mCurrrentSelectionRedMarker = createSelectedItemOverlay();

        mSelectedItemsHandler.mBlueMarker = ResourceUtils.getDrawable(getActivity(),R.drawable.marker_blue);
        mFolderOverlayGreenPhotoMarker = createFolderOverlay(overlays);

        mFolderOverlayBlueSelectionMarker = createFolderOverlay(overlays);

        overlays.add(new GuestureOverlay(getActivity()));

        mapView.setMultiTouchControls(true);
    }

    protected IconOverlay createSelectedItemOverlay() {
        Drawable currrentSelectionIcon = ResourceUtils.getDrawable(getActivity(), R.drawable.marker_red);
        // fixed positon, not updated on pick
        return new IconOverlay(null, currrentSelectionIcon);
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

	public IGeoRectangle getCurrentGeoRectangle() {
		IGeoRectangle result = getGeoRectangle(mMapView.getBoundingBox());
		return result;
	}

    protected void onOk() {
        if (mDirectoryListener != null) {
            IGeoRectangle result = getCurrentGeoRectangle();
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
        mMinZoomLevel = mMapView.getMinZoomLevel();
        mMaxZoomLevel = mMapView.getMaxZoomLevel();

        mZoomBar.setMax((int) (mMaxZoomLevel - mMinZoomLevel));
        mZoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    mMapView.getController().setZoom(progress + mMinZoomLevel);
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


    private FolderOverlayEx createFolderOverlay(List<Overlay> overlays) {
        FolderOverlayEx result = new FolderOverlayEx();
        overlays.add(result);

        return result;
    }

    public void defineNavigation(IGalleryFilter rootFilter, GeoRectangle rectangle, int zoomlevel,
                                 SelectedItems selectedItems, Uri gpxAdditionalPointsContentUri) {
        if (Global.debugEnabled || Global.debugEnabledMap) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + "defineNavigation: " + rectangle + ";z=" + zoomlevel);
        }

        if (rootFilter != null) {
            this.mRootFilter = rootFilter;
        }

        // load this.mFolderOverlayBlueGpxMarker
        GeoRectangle increasedRrectangle = defineGpxAdditionalPoints(gpxAdditionalPointsContentUri, rectangle);

        mSelectedItemsHandler.define(selectedItems);

        String debugContext = "defineNavigation";
        zoomToBoundingBox(debugContext, increasedRrectangle, zoomlevel);

        if (rootFilter != null) {
            reloadFotoMarker(debugContext);
        }
    }

    protected void zoomToBoundingBox(String debugContext, GeoRectangle rectangle, int zoomlevel) {
        if ((rectangle != null) && !rectangle.isEmpty()) {
            BoundingBox boundingBox = new BoundingBox(
                    rectangle.getLatitudeMax(),
                    rectangle.getLogituedMin(),
                    rectangle.getLatitudeMin(),
                    rectangle.getLogituedMax());

            zoomToBoundingBox(debugContext, boundingBox, zoomlevel);
        }
    }

    /**
     * Loads items from gpxAdditionalPointsContentUri into this.mFolderOverlayBlueGpxMarker.
     * @param gpxAdditionalPointsContentUri where the gpx data comes from or null if no gpx
     * @param rectangle previous geo-rectangle-map-area
     * @return new geo-rectangle-map-area from gpx
     */
    protected GeoRectangle defineGpxAdditionalPoints(Uri gpxAdditionalPointsContentUri, GeoRectangle rectangle) {
        if (gpxAdditionalPointsContentUri != null) {
            if (Global.debugEnabled || Global.debugEnabledMap) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + "defineGpxAdditionalPoints: " + gpxAdditionalPointsContentUri);
            }

            final FolderOverlayEx folderForNewGpxItems = new FolderOverlayEx();

            final GeoRectangle gpxBox = (rectangle == null) ? new GeoRectangle() : null;
            ContentResolver cr = getActivity().getContentResolver();
            final GeoPointDtoEx currentLoadedGeoPoint = new GeoPointDtoEx();
            try {
                InputStream is = cr.openInputStream(gpxAdditionalPointsContentUri);
                GpxReaderBase parser = new GpxReaderBase(new IGeoInfoHandler() {
                    @Override
                    public boolean onGeoInfo(IGeoPointInfo iGeoPointInfo) {
                        return addGpxGeoPoint(folderForNewGpxItems, currentLoadedGeoPoint, gpxBox);
                    }
                }, currentLoadedGeoPoint);
                parser.parse(new InputSource(is));
                if (gpxBox != null) {
                    // box 50% more on right,left,top,button. delta >= 0.01 degrees
                    gpxBox.increase(Global.mapMultiselectionBoxIncreaseByProcent, Global.mapMultiselectionBoxIncreaseMinSizeInDegrees);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (mFolderOverlayBlueGpxMarker != null) {
                mMapView.getOverlays().remove(mFolderOverlayBlueGpxMarker);
                mFolderOverlayBlueGpxMarker = null;
            }
            mFolderOverlayBlueGpxMarker = folderForNewGpxItems;
            mMapView.getOverlays().add(0, mFolderOverlayBlueGpxMarker);
            if (Global.debugEnabled || Global.debugEnabledMap) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + "defineGpxAdditionalPoints itemcount: " + folderForNewGpxItems.getItems().size());
            }


            if (gpxBox != null) return gpxBox;
        }
        return rectangle;
    }

    private MarkerBubblePopup<GeoPointDtoEx> mPopup;

    private static int mId = 1;

    protected boolean addGpxGeoPoint(FolderOverlayEx destination, GeoPointDtoEx point, GeoRectangle box) {
        Overlay marker = createMarkerEx(point);
        destination.add(marker);
        if (box != null) box.inflate(point.getLatitude(), point.getLongitude());
        return true;
    }

    private Overlay createMarkerEx(GeoPointDtoEx point) {
        GeoPointDtoEx position = (GeoPointDtoEx) point.clone();
        // MarkerEx.setDefaultIcon(mSelectedItemsHandler.mBlueMarker);
        // marker.setIcon(mSelectedItemsHandler.mBlueMarker); use global
        return new MarkerEx<GeoPointDtoEx>(mPopup).set(mId++, position, mSelectedItemsHandler.mBlueMarker, position );
    }

    private void zoomToBoundingBox(String why, BoundingBox boundingBox, int zoomLevel) {
        if (boundingBox != null) {
            if (mIsInitialized) {
                // if map is already initialized
                GeoPoint min = new GeoPoint(boundingBox.getLatSouth(), boundingBox.getLonWest());

                if (zoomLevel != NO_ZOOM) {
                    OsmdroidUtil.zoomTo(this.mMapView, zoomLevel, min, null);
                } else {
                    GeoPoint max = new GeoPoint(boundingBox.getLatNorth(), boundingBox.getLonEast());
                    OsmdroidUtil.zoomTo(this.mMapView, OsmdroidUtil.NO_ZOOM, min, max);

                    // this.mMapView.zoomToBoundingBox(boundingBox); this is to inexact
                }
                if (Global.debugEnabledMap) {
                    Log.i(Global.LOG_CONTEXT, mDebugPrefix
                            + "zoomToBoundingBox(" + why
                            + "; z=" + mMapView.getZoomLevelDouble()
                            + ") :"
                            + boundingBox
                            + " <= "
                            + mMapView.getBoundingBox()
                            );
                }
                setZoomBarZoomLevel(why, mMapView.getZoomLevelDouble());
            } else {
                // map not initialized yet. do it later.
                this.mDelayedZoomToBoundingBox = boundingBox;
                this.mDelayedZoomLevel = zoomLevel;
            }
        }
    }

    /** all marker clicks will be delegated to LocationMapFragment#onFotoMarkerClicked() */
    private class FotoMarker extends ClickableIconOverlay<Object> {

        /**
         * @return true if click was handeled.
         */
        @Override
        protected boolean onMarkerClicked(MapView mapView, int markerId, IGeoPoint makerPosition, Object markerData) {
            return LocationMapFragment.this.onFotoMarkerClicked(this, markerId, makerPosition, markerData);
        }

        /**
         * @return true if click was handeled.
         */
        @Override
        protected boolean onMarkerLongPress(MapView mapView, int markerId, IGeoPoint geoPosition, Object data) {
            return LocationMapFragment.this.onFotoMarkerLongPress(this, markerId, geoPosition, data);
        }

    }

    private void reloadFotoMarker(String why) {
        if (mIsInitialized) {
            // initialized
            if (mCurrentFotoMarkerLoader == null) {
                // not active yet
                List<Overlay> oldItems = mFolderOverlayGreenPhotoMarker.getItems();

                mLastZoom = this.mMapView.getZoomLevelDouble();
                double groupingFactor = getGroupingFactor(mLastZoom);
                BoundingBox world = this.mMapView.getBoundingBox();
                if (Global.debugEnabledMap) {
                    Log.d(Global.LOG_CONTEXT, mDebugPrefix + "reloadFotoMarker(" + why + ")"
                            + world + ", zoom " + mLastZoom);
                }

                reloadFotoMarker(world, groupingFactor, oldItems);
            } else {
                // background load is already active. Remember that at least one scroll/zoom was missing
                mFotoMarkerPendingLoads++;
            }
        }
    }

    private void reloadFotoMarker(BoundingBox latLonArea, double groupingFactor, List<Overlay> oldItems) {
        QueryParameter query = FotoSql.getQueryGroupByPlace(groupingFactor);
        query.clearWhere();

        if (this.mRootFilter != null) {
            TagSql.filter2QueryEx(query, this.mRootFilter, true);
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

        mCurrentFotoMarkerLoader = new FotoMarkerLoaderTask(createHashMap(oldItems));
        mCurrentFotoMarkerLoader.execute(query);
    }

    private IGeoRectangle getGeoRectangle(BoundingBox boundingBox) {
        GeoRectangle result = new GeoRectangle();
        result.setLatitude(boundingBox.getLatSouth(), boundingBox.getLatNorth());
        result.setLogitude(boundingBox.getLonWest(), boundingBox.getLonEast());

        return result;
    }

    /** translates map-zoomlevel to groupfactor
     * that tells sql how geo-points are grouped together.
     */
    private double getGroupingFactor(double zoomlevel) {
        // todo
        return FotoSql.getGroupFactor(zoomlevel);
    }

    // for debugginc
    private static int sInstanceCountFotoLoader = 1;

    /** caching support: if zoom level changes the cached items become invalid
     * because the marker clustering is different */
    private double mLastZoom = NO_ZOOM;

    /** how much mCurrentFotoMarkerLoader are tirggerd while task is loading */
    private int mFotoMarkerPendingLoads = 0;

    /** The factory LocationMapFragment.FotoMarkerLoaderTask#createMarker() tries to recycle old
     *     unused Fotomarkers before creating new */
    private Stack<FotoMarker> mFotoMarkerRecycler = new Stack<FotoMarker>();

    /** To allow canceling of loading task. There are 0 or one tasks running at a time */
    private FotoMarkerLoaderTask mCurrentFotoMarkerLoader = null;

    /** to load foto summary marker with numbers in the icons */
    private class FotoMarkerLoaderTask extends MarkerLoaderTaskWithRecycling<FotoMarker> {
        public FotoMarkerLoaderTask(HashMap<Integer, FotoMarker> oldItems) {
            super(getActivity(), LocationMapFragment.this.mDebugPrefix + "-FotoMarkerLoaderTask#" + (sInstanceCountFotoLoader++) + "-",
                    mFotoMarkerRecycler, oldItems, NO_MARKER_COUNT_LIMIT);
        }

        @NonNull
        protected FotoMarker createNewMarker() {
            return new FotoMarker();
        }

        // This is called when doInBackground() is finished
        @Override
        protected void onPostExecute(OverlayManager result) {
            boolean zoomLevelChanged = mMapView.getZoomLevelDouble() != mLastZoom;

            if (isCancelled()) {
                onLoadFinishedFotoMarker(null, zoomLevelChanged);
            } else {
                onLoadFinishedFotoMarker(result, zoomLevelChanged);

                recyleItems(zoomLevelChanged, mOldItems);
            }

            mOldItems.clear();
            mOldItems = null;
            int recyclerSize = mFotoMarkerRecycler.size();
            if (mStatus != null) {
                mStatus.append("\n\tRecycler: ").append(mRecyclerSizeBefore).append(",")
                        .append(mRecyclerSizeAfter).append(",").append(recyclerSize)
                        .append("\n\t").append(mMapView.getBoundingBox())
                        .append(", z= ").append(mMapView.getZoomLevelDouble())
                        .append("\n\tPendingLoads: ").append(mFotoMarkerPendingLoads);
                if (Global.debugEnabledSql) {
                    Log.w(Global.LOG_CONTEXT, mDebugPrefix + mStatus);
                } else {
                    Log.i(Global.LOG_CONTEXT, mDebugPrefix + mStatus);
                }
            }

            // in the meantime the mapview has moved: must recalculate again.
            mCurrentFotoMarkerLoader = null;
            if (mFotoMarkerPendingLoads > 0) {
                String why = (Global.debugEnabledMap) ? ("Lost zoom/scroll event(s): " + mFotoMarkerPendingLoads) : null;
                mFotoMarkerPendingLoads = 0;
                reloadFotoMarker(why);
            }
        }

    } // class FotoMarkerLoaderTask

    /** gets called when FotoMarkerLoaderTask has finished.
     *
     * @param newFotoIcons null if there was an error
     */
    private void onLoadFinishedFotoMarker(OverlayManager newFotoIcons, boolean zoomLevelChanged) {
        StringBuilder dbg = (Global.debugEnabledSql || Global.debugEnabledMap) ? new StringBuilder() : null;
        if (dbg != null) {
            int found = (newFotoIcons != null) ? newFotoIcons.size() : 0;
            dbg.append(mDebugPrefix).append("onLoadFinishedFotoMarker(z=")
                    .append(mMapView.getZoomLevelDouble()).append(") markers created: ").append(found).append(". ");
        }

        if (newFotoIcons != null) {
            OverlayManager oldFotoIcons = mFolderOverlayGreenPhotoMarker.setOverlayManager(newFotoIcons);
            if (oldFotoIcons != null) {
                if (dbg != null) {
                    dbg.append(mDebugPrefix).append(" previous : : ").append(oldFotoIcons.size());
                }
                if (zoomLevelChanged) {
                    if (dbg != null) dbg
                            .append(" zoomLevelChanged - recycling : ")
                            .append(oldFotoIcons.size())
                            .append(" items");

                    for (Overlay item : oldFotoIcons) {
                        mFotoMarkerRecycler.add((FotoMarker) item);
                    }
                }
                oldFotoIcons.onDetach(this.mMapView);
                oldFotoIcons.clear();
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
    protected boolean onFotoMarkerClicked(IconOverlay marker, int markerId, IGeoPoint geoPosition, Object markerData) {

        ThumbNailUtils.getThumb(markerId, mCurrentPhoto);

        this.mCurrentPhoto.setVisibility(View.VISIBLE);

        updateMarker(marker, markerId, geoPosition, markerData);

        return true; // TODO
    }

    protected boolean onFotoMarkerLongPress(IconOverlay marker, int markerId, IGeoPoint geoPosition, Object markerData) {
        onFotoMarkerClicked(marker, markerId, geoPosition, markerData);
        return showContextMenu(this.mMapView, markerId, geoPosition, markerData);
    }

    protected void updateMarker(IconOverlay marker, int markerId, IGeoPoint makerPosition, Object markerData) {
        mMarkerId = markerId;
        if (mCurrrentSelectionRedMarker != null) {
            mMapView.getOverlays().remove(mCurrrentSelectionRedMarker);
            mCurrrentSelectionRedMarker.moveTo(makerPosition, mMapView);
            mMapView.getOverlays().add(mCurrrentSelectionRedMarker);
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

    private SelectedItemsHandler mSelectedItemsHandler = new SelectedItemsHandler();

    /** Support for non-clustered selected items */
    private class SelectedItemsHandler {

        public void define(SelectedItems selectedItems) {
            if ((selectedItems != null) && (this.mSelectedItems != selectedItems)) {
                this.mSelectedItems = selectedItems;
                reloadSelectionMarker();
            }
        }

        private SelectedItems mSelectedItems = null;

        /**
         * To allow canceling of loading task. There are 0 or one tasks running at a time
         */
        private SelectionMarkerLoaderTask mCurrentSelectionMarkerLoader = null;

        private Drawable mBlueMarker;

        /**
         * to load markers for current selected items
         */
        private class SelectionMarkerLoaderTask extends MarkerLoaderTaskWithRecycling<FotoMarker> {
            public SelectionMarkerLoaderTask(HashMap<Integer, FotoMarker> oldItems) {
                super(getActivity(), LocationMapFragment.this.mDebugPrefix + "-SelectionMarkerLoaderTask#" + (sInstanceCountFotoLoader++) + "-", mFotoMarkerRecycler,
                        oldItems, Global.maxSelectionMarkersInMap);
            }

            @NonNull
            protected FotoMarker createNewMarker() {
                return new FotoMarker();
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
                int recyclerSize = mFotoMarkerRecycler.size();
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
                return (BitmapDrawable) mBlueMarker;
            }

        } // class SelectionMarkerLoaderTask

        /** gets called when MarkerLoaderTask has finished.
         *
         * @param loadedBlueMarkers null if there was an error
         */
        protected void onLoadFinishedSelection(OverlayManager loadedBlueMarkers) {
            mCurrentSelectionMarkerLoader = null;
            StringBuilder dbg = (Global.debugEnabledSql || Global.debugEnabledMap) ? new StringBuilder() : null;
            if (dbg != null) {
                int found = (loadedBlueMarkers != null) ? loadedBlueMarkers.size() : 0;
                dbg.append(mDebugPrefix).append("onLoadFinishedSelection() markers created: ").append(found);
            }

            if ((loadedBlueMarkers != null) && (loadedBlueMarkers.size() > 0)) {
                OverlayManager old = mFolderOverlayBlueSelectionMarker.setOverlayManager(loadedBlueMarkers);
                if (old != null) {
                    if (dbg != null) {
                        dbg.append(mDebugPrefix).append(" previous : : ").append(old.size());
                    }
                    old.onDetach(mMapView);
                    old.clear();
                }
                mMapView.invalidate();

                GeoRectangle box = new GeoRectangle();
                for (Overlay item: loadedBlueMarkers) {
                    IGeoPoint pos = ((IconOverlay) item).getPosition();

                    box.inflate(pos.getLatitude(), pos.getLongitude());
                }
                // box 50% more on right,left,top,button. delta >= 0.01 degrees
                box.increase(Global.mapMultiselectionBoxIncreaseByProcent, Global.mapMultiselectionBoxIncreaseMinSizeInDegrees);
                zoomToBoundingBox("onLoadFinished Selection", box, NO_ZOOM);
            }
            if (dbg != null) {
                Log.d(Global.LOG_CONTEXT, dbg.toString());
            }
        }

        private void reloadSelectionMarker() {
            if ((mFolderOverlayBlueSelectionMarker != null) &&
                    (mSelectedItems != null) && (!mSelectedItems.isEmpty())) {
                if (mCurrentSelectionMarkerLoader != null) {
                    mCurrentSelectionMarkerLoader.cancel(false);
                    mCurrentSelectionMarkerLoader = null;
                }

                List<Overlay> oldItems = mFolderOverlayBlueSelectionMarker.getItems();

                QueryParameter query = new QueryParameter(FotoSql.queryGps);
                FotoSql.setWhereSelectionPks(query, mSelectedItems);
                FotoSql.addWhereLatLonNotNull(query);

                mCurrentSelectionMarkerLoader = new SelectionMarkerLoaderTask(createHashMap(oldItems));
                mCurrentSelectionMarkerLoader.execute(query);
            }
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

    protected   boolean showContextMenu(final View parent, final int markerId,
                                        final IGeoPoint geoPosition, final Object markerData) {
        closePopup();
        MenuInflater inflater = getActivity().getMenuInflater();

        mTempPopupMenuParentView = OsmdroidUtil.openMapPopupView(mMapView, 0, new GeoPoint(geoPosition.getLatitude(), geoPosition.getLongitude()));
        PopupMenu menu = new PopupMenu(getActivity(), mTempPopupMenuParentView);

        inflater.inflate(LockScreen.isLocked(this.getActivity()) ? R.menu.menu_map_context_locked :  R.menu.menu_map_context, menu.getMenu());

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                closePopup();

                switch (item.getItemId()) {
                    case R.id.cmd_photo:
                        return showPhoto(getGeoPointById(markerId, geoPosition));
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

                        IntentUtil.cmdStartIntent(getActivity(), null, uri, null, Intent.ACTION_VIEW, R.string.geo_show_as_menu_title, R.string.geo_picker_err_not_found, 0);

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

    protected void closePopup() {
        OsmdroidUtil.closeMapPopupView(mMapView, mTempPopupMenuParentView);
        mTempPopupMenuParentView = null;
    }

    private boolean showPhoto(IGeoPoint geoPosition) {
        GalleryFilterParameter filter = getMarkerFilter(geoPosition);
        QueryParameter query = new QueryParameter();
        TagSql.filter2QueryEx(query, filter, false);
        FotoSql.setSort(query, FotoSql.SORT_BY_DATE, false);

        ImageDetailActivityViewPager.showActivity(this.getActivity(), null, 0, query, 0);
        return true;
    }

    private boolean showGallery(IGeoPoint geoPosition) {
        GalleryFilterParameter filter = getMarkerFilter(geoPosition);
        FotoGalleryActivity.showActivity(this.getActivity(), filter, null, 0);
        return true;
    }

    private boolean zoomToFit(IGeoPoint geoPosition) {
        BoundingBox BoundingBox = null;

        IGeoRectangle fittingRectangle = FotoSql.execGetGeoRectangle(this.getActivity(), getMarkerFilter(geoPosition), null);
        double delta = getDelta(fittingRectangle);
        if (delta < 1e-6) {
            BoundingBox = getMarkerBoundingBox(geoPosition);

        } else {
            double enlarge = delta * 0.2;
            BoundingBox = new BoundingBox(
                    fittingRectangle.getLatitudeMax()+enlarge,
                    fittingRectangle.getLogituedMax()+enlarge,
                    fittingRectangle.getLatitudeMin()-enlarge,
                    fittingRectangle.getLogituedMin()-enlarge);
        }
        if (Global.debugEnabledMap) {
            Log.i(Global.LOG_CONTEXT, "zoomToFit(): " + fittingRectangle +
                    " delta " + delta +
                    " => box " + BoundingBox);
        }
        zoomToBoundingBox("zoomToFit()", BoundingBox, NO_ZOOM);
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
        result.setNonGeoOnly(false);
        result.setLatitude(geoPosition.getLatitude() - delta, geoPosition.getLatitude() + delta);
        result.setLogitude(geoPosition.getLongitude() - delta, geoPosition.getLongitude() + delta);

        return result;
    }

    @NonNull
    private BoundingBox getMarkerBoundingBox(IGeoPoint geoPosition) {
        double delta = getMarkerDelta();

        return new BoundingBox(
                geoPosition.getLatitude()+delta,
                geoPosition.getLongitude()+delta,
                geoPosition.getLatitude()-delta,
                geoPosition.getLongitude()-delta);
    }

    private double getMarkerDelta() {
        double zoomLevel = this.mMapView.getZoomLevelDouble();
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
