package de.k3b.android.fotoviewer.locationmap;


import android.app.Activity;
import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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

import de.k3b.android.fotoviewer.Global;
import de.k3b.android.fotoviewer.R;
import de.k3b.android.fotoviewer.queries.FotoSql;
import de.k3b.android.fotoviewer.queries.GalleryFilterParameterParcelable;
import de.k3b.android.fotoviewer.queries.QueryParameterParcelable;
import de.k3b.android.osmdroid.DefaultResourceProxyImplEx;
import de.k3b.android.osmdroid.FolderOverlay;
import de.k3b.android.osmdroid.GuestureOverlay;
import de.k3b.android.osmdroid.IconFactory;
import de.k3b.android.osmdroid.MarkerBase;
import de.k3b.android.osmdroid.ZoomUtil;
import de.k3b.database.QueryParameter;
import de.k3b.io.GeoRectangle;
import de.k3b.io.IGeoRectangle;

/**
 * A fragment to display Foto locations in a geofrafic map.
 * A location-area can be picked for filtering.
 * A simple {@link Fragment} subclass.
 */
public class LocationMapFragment extends DialogFragment {

    private static final String STATE_LAST_VIEWPORT = "LAST_VIEWPORT";
    // for debugging
    private static int id = 1;
    private final String debugPrefix;

    private MapView mMapView;
    private FolderOverlay mFolderOverlay;
    IconFactory mIconFactory = null;
    private DefaultResourceProxyImplEx mResourceProxy;

    // api to fragment owner
    private OnDirectoryInteractionListener mDirectoryListener;



    /**
     * setCenterZoom does not work in onCreate() because getHeight() and getWidth() are not calculated yet and return 0;
     * setCenterZoom must be set later when getHeight() and getWith() are known (i.e. in onWindowFocusChanged()).
     * <p/>
     * see http://stackoverflow.com/questions/10411975/how-to-get-the-width-and-height-of-an-image-view-in-android/10412209#10412209
     */
    private BoundingBoxE6 mDelayedZoomToBoundingBox = null;
    private SeekBar mZoomBar;
    private ImageView mImage;
    private GalleryFilterParameterParcelable mRootFilter;

    public LocationMapFragment() {
        // Required empty public constructor
        debugPrefix = "LocationMapFragment#" + (id++)  + " ";
        Global.debugMemory(debugPrefix, "ctor");
        // Required empty public constructor
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, debugPrefix + "()");
        }
    }

    @Override public void onDestroy() {
        if (mCurrentLoader != null) mCurrentLoader.cancel(false);
        mCurrentLoader = null;

        if (mRecycler != null) mRecycler.empty();
        super.onDestroy();
        // RefWatcher refWatcher = FotoGalleryApp.getRefWatcher(getActivity());
        // refWatcher.watch(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mDirectoryListener = (OnDirectoryInteractionListener) activity;
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
        savedInstanceState.putParcelable(STATE_LAST_VIEWPORT, this.mMapView.getBoundingBox());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        /** after ratation restore selelected view port */
        if (savedInstanceState != null) {
            this.mDelayedZoomToBoundingBox = savedInstanceState.getParcelable(STATE_LAST_VIEWPORT);
        }
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_location_map, container, false);

        mMapView = (MapView) view.findViewById(R.id.mapview);
        this.mImage = (ImageView) view.findViewById(R.id.image);
        this.mImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mImage.setVisibility(View.GONE);
            }
        });
        createZoomBar(view);
        mMapView.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                reload();
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                mZoomBar.setProgress(mMapView.getZoomLevel());

                reload();
                return false;
            }
        });

        mResourceProxy = new DefaultResourceProxyImplEx(getActivity().getApplicationContext());

        mIconFactory = new IconFactory(mResourceProxy, getResources().getDrawable(R.drawable.marker_green));
        final List<Overlay> overlays = this.mMapView.getOverlays();

        mFolderOverlay = createFolderOverlay(overlays);

        overlays.add(new GuestureOverlay(getActivity()));

        mMapView.setMultiTouchControls(true);


        // mFolderOverlay.add(createMarker(mMapView, ...));

        if (getShowsDialog()) {
            Button cmdCancel = (Button) view.findViewById(R.id.cmd_cancel);
            cmdCancel.setVisibility(View.VISIBLE);
            cmdCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();
                }
            });

            Button cmdOk = (Button) view.findViewById(R.id.ok);
            cmdOk.setVisibility(View.VISIBLE);
            cmdOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onOk();
                }
            });

            String title = getActivity().getString(
                    R.string.directory_fragment_dialog_title,
                    getActivity().getString(R.string.gallery_location));
            getDialog().setTitle(title);

        }

        if (this.mDelayedZoomToBoundingBox != null) {
            mMapView.addOnFirstLayoutListener(new MapView.OnFirstLayoutListener() {
                @Override
                public void onFirstLayout(View v, int left, int top, int right, int bottom) {
                    zoomToBoundingBox(mDelayedZoomToBoundingBox);
                    mDelayedZoomToBoundingBox = null;
                }
            });
        }

        return view;
    }

    private void onOk() {
        if (mDirectoryListener != null) {
            IGeoRectangle result = getGeoRectangle(mMapView.getBoundingBox());
            if (Global.debugEnabled) {
                Log.i(Global.LOG_CONTEXT, debugPrefix + "onOk: " + result);
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

    public void defineNavigation(GalleryFilterParameterParcelable rootFilter, GeoRectangle rectangle, int queryType) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + "defineNavigation: " + rectangle);
        }

        this.mRootFilter = rootFilter;
        BoundingBoxE6 boundingBox = new BoundingBoxE6(
                rectangle.getLatitudeMax(),
                rectangle.getLogituedMin(),
                rectangle.getLatitudeMin(),
                rectangle.getLogituedMax());

        zoomToBoundingBox(boundingBox);
    }

    private void zoomToBoundingBox(BoundingBoxE6 boundingBox) {
        if (this.mMapView != null) {
            GeoPoint min = new GeoPoint(boundingBox.getLatSouthE6(), boundingBox.getLonWestE6());
            GeoPoint max = new GeoPoint(boundingBox.getLatNorthE6(), boundingBox.getLonEastE6());
            ZoomUtil.zoomTo(this.mMapView, ZoomUtil.NO_ZOOM, min, max);
            // this.mMapView.zoomToBoundingBox(boundingBox); this is to inexact

            if (Global.debugEnabled) {
                Log.i(Global.LOG_CONTEXT, debugPrefix + "zoomToBoundingBox(" + boundingBox
                        + ") => " + mMapView.getBoundingBox() + "; z=" + mMapView.getZoomLevel());
            }
        } else {
            this.mDelayedZoomToBoundingBox = boundingBox;
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
            return LocationMapFragment.this.onMarkerClicked(markerId, makerPosition, markerData);
        }
    }

    /** The factory LocationMapFragment.FotoMarkerLoaderTask#createMarker() tries to recycle old
     *     unused Fotomarkers before creating new */
    private Stack<FotoMarker> mRecycler = new Stack<FotoMarker>();

    private void reload() {
        if (mMapView.getHeight() > 0) {
            // initialized
            if (mCurrentLoader == null) {
                // not active yet
                List<Overlay> oldItems = mFolderOverlay.getItems();

                mLastZoom = this.mMapView.getZoomLevel();
                double groupingFactor = getGroupingFactor(mLastZoom);
                BoundingBoxE6 world = this.mMapView.getBoundingBox();

                reload(world, groupingFactor, oldItems);
            } else {
                mPendingLoads++;
            }
        }
    }

    private void reload(BoundingBoxE6 latLonArea, double groupingFactor, List<Overlay> oldItems) {
        QueryParameterParcelable query = FotoSql.getQueryGroupByPlace(groupingFactor);
        query.clearWhere();

        if (this.mRootFilter != null) {
            FotoSql.setWhereFilter(query, this.mRootFilter);
        }

        // delta: make the grouping area a little bit bigger than the viewport
        // so that counts at the borders are correct.
        double delta = (groupingFactor > 0) ? (2.0 / groupingFactor) : 0.0;
        IGeoRectangle rect = getGeoRectangle(latLonArea);
        FotoSql.addWhereFilteLatLon(query
                , rect.getLatitudeMin() - delta
                , rect.getLatitudeMax() + delta
                , rect.getLogituedMin() - delta
                , rect.getLogituedMax() + delta);

        HashMap<Integer, FotoMarker> oldItemsHash = new HashMap<Integer, FotoMarker>();
        for (Overlay o : oldItems) {
            FotoMarker marker = (FotoMarker) o;
            oldItemsHash.put(marker.getID(), marker);
        }

        mCurrentLoader = new FotoMarkerLoaderTask(oldItemsHash);
        mCurrentLoader.execute(query);
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

    private int mLastZoom = -1;
    private int mPendingLoads = 0;
    private FotoMarkerLoaderTask mCurrentLoader = null;
    private class FotoMarkerLoaderTask extends MarkerLoaderTask<FotoMarker> {

        private final int mRecyclerBefore;
        private int mRecyclerAfter;

        public FotoMarkerLoaderTask(HashMap<Integer, FotoMarker> oldItems) {
            super(getActivity(), debugPrefix + "-MarkerLoader#" + (sInstanceCountFotoLoader++) + "-", oldItems);
            mRecyclerBefore = mRecycler.size();
        }

        @Override
        protected FotoMarker createMarker() {
            if (Global.debugEnabledViewItem) {
                Log.i(Global.LOG_CONTEXT, debugPrefix + "createMarker() " + mRecycler.size());
            }

            if (mRecycler.isEmpty()) {
                return new FotoMarker(mResourceProxy);
            }

            FotoMarker marker = mRecycler.pop();
            if (Global.debugEnabledViewItem) {
                Log.i(Global.LOG_CONTEXT, debugPrefix + "recycled viewitem");
            }
            return marker;
        }

        @Override
        protected OverlayManager doInBackground(QueryParameter... queryParameter) {
            OverlayManager result = super.doInBackground(queryParameter);
            mRecyclerAfter = mRecycler.size();
            return result;
        }
            // This is called when doInBackground() is finished
        protected void onPostExecute(OverlayManager result) {
            boolean zoomLevelChanged = mMapView.getZoomLevel() != mLastZoom;

            if (isCancelled()) {
                onLoadFinished(null, zoomLevelChanged);
            } else {
                onLoadFinished(result, zoomLevelChanged);

                recyleItems(zoomLevelChanged, mOldItems);
            }
            mOldItems.clear();
            mOldItems = null;
            int recycler = mRecycler.size();
            if (mStatus != null) {
                mStatus.append("\n\tRecycler: ").append(mRecyclerBefore).append(",")
                        .append(mRecyclerAfter).append(",").append(recycler)
                        .append("\n\t").append(mMapView.getBoundingBox())
                        .append(", z= ").append(mMapView.getZoomLevel())
                        .append("\n\tPendingLoads").append(mPendingLoads);
                if (Global.debugEnabledSql) {
                    Log.w(Global.LOG_CONTEXT, debugPrefix + mStatus);
                } else {
                    Log.i(Global.LOG_CONTEXT, debugPrefix + mStatus);
                }
            }

            // in the meantime the mapview has moved: must recalculate again.
            mCurrentLoader = null;
            if (mPendingLoads > 0) {
                mPendingLoads = 0;
                reload();
            }
        }

        private void recyleItems(boolean zoomLevelChanged, HashMap<Integer, FotoMarker> unusedItems) {
            if (!zoomLevelChanged) {
                if (Global.debugEnabledViewItem) {
                    Log.d(Global.LOG_CONTEXT, debugPrefix + "recyleItems() : " + unusedItems.size());
                }

                // unused old items go into recycler
                for (Integer id : unusedItems.keySet()) {
                    FotoMarker marker = unusedItems.get(id);
                    marker.set(0, null, null, null);
                    mRecycler.add(marker);
                }
            }
        }
    }

    /** gets called when FotoMarkerLoaderTask has finished.
     *
     * @param result null if there was an error
     * @param zoomLevelChanged
     */
    private void onLoadFinished(OverlayManager result, boolean zoomLevelChanged) {
        StringBuilder dbg = (Global.debugEnabledSql || Global.debugEnabled) ? new StringBuilder() : null;
        if (dbg != null) {
            int found = (result != null) ? result.size() : 0;
            dbg.append(debugPrefix).append("onLoadFinished() markers created: ").append(found);
        }

        if (result != null) {
            OverlayManager old = mFolderOverlay.setOverlayManager(result);
            if (old != null) {
                if (dbg != null) {
                    dbg.append(debugPrefix).append(" previous : : ").append(old.size());
                }
                if (zoomLevelChanged) {
                    if (dbg != null) dbg
                            .append(" zoomLevelChanged - recycling : ")
                            .append(old.size())
                            .append(" items");

                    for (Overlay item : old) {
                        mRecycler.add((FotoMarker) item);
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
    private boolean onMarkerClicked(int markerId, IGeoPoint makerPosition, Object markerData) {
        this.mImage.setImageBitmap(getBitmap(markerId));
        this.mImage.setVisibility(View.VISIBLE);
        return true; // TODO
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
        /** called when user picks a new directory */
        void onDirectoryPick(String selectedAbsolutePath, int queryTypeId);

        /** called when user cancels picking of a new directory
         * @param queryTypeId*/
        void onDirectoryCancel(int queryTypeId);

        /** called after the selection in tree has changed */
        void onDirectorySelectionChanged(String selectedChild, int queryTypeId);
    }

}
