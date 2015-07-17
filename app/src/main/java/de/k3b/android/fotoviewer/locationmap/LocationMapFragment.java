package de.k3b.android.fotoviewer.locationmap;


import android.app.DialogFragment;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayManager;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import de.k3b.android.fotoviewer.Global;
import de.k3b.android.fotoviewer.R;
import de.k3b.android.fotoviewer.queries.FotoSql;
import de.k3b.android.fotoviewer.queries.QueryParameterParcelable;
import de.k3b.android.osmdroid.DefaultResourceProxyImplEx;
import de.k3b.android.osmdroid.FolderOverlay;
import de.k3b.android.osmdroid.IconFactory;
import de.k3b.android.osmdroid.MarkerBase;
import de.k3b.database.QueryParameter;

/**
 * A simple {@link Fragment} subclass.
 */
public class LocationMapFragment extends DialogFragment {

    // for debugging
    private static int id = 1;
    private final String debugPrefix;

    private MapView mMapView;
    private FolderOverlay mFolderOverlay;
    IconFactory mIconFactory = null;
    private DefaultResourceProxyImplEx mResourceProxy;

    public LocationMapFragment() {
        // Required empty public constructor
        debugPrefix = "DirectoryPickerFragment#" + (id++)  + " ";
        Global.debugMemory(debugPrefix, "ctor");
        // Required empty public constructor
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + "()");
        }
    }

    @Override public void onDestroy() {
        if (mRecycler != null) mRecycler.empty();
        super.onDestroy();
        // RefWatcher refWatcher = FotoGalleryApp.getRefWatcher(getActivity());
        // refWatcher.watch(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_location_map, container, false);

        mMapView = (MapView) view.findViewById(R.id.mapview);
        mResourceProxy = new DefaultResourceProxyImplEx(getActivity().getApplicationContext());

        mIconFactory = new IconFactory(mResourceProxy, getResources().getDrawable(R.drawable.marker_green));
        final List<Overlay> overlays = this.mMapView.getOverlays();

        mFolderOverlay = createFolderOverlay(overlays);

        // mFolderOverlay.add(createMarker(mMapView, ...));

        if (getShowsDialog()) {
            Button cmdOk = (Button) view.findViewById(R.id.ok);
            cmdOk.setVisibility(View.VISIBLE);
            cmdOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    /*
                    if (currentSelectedPosition != null) {

                        GeoPointDto geoPoint = new GeoPointDto().setLatitude(currentSelectedPosition.getPosition().getLatitude()).setLongitude(currentSelectedPosition.getPosition().getLongitude());
                        String uri = new GeoUri(GeoUri.OPT_DEFAULT).toUriString(geoPoint);
                        setResult(0, new Intent(Intent.ACTION_PICK, Uri.parse(uri)));
                    }

                    finish();
                */
                }
            });
        }

        return view;
    }

    private FolderOverlay createFolderOverlay(List<Overlay> overlays) {
        FolderOverlay result = new FolderOverlay(this.getActivity());
        overlays.add(result);

        return result;
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
        /*
        int zoomlevel = this.mMapView.getZoomLevel();
        BoundingBoxE6 world = this.mMapView.getBoundingBox();

        !!!
        QueryParameterParcelable query = FotoSql.getQueryGroupByPlace(getGroupingFactor(zoomlevel))
                .clearWhere().addWhere("(" + FotoSql.SQL_COL_LAT + " >= ? " +
                                "AND " + FotoSql.SQL_COL_LAT + " < ?", world.getLatSouthE6() )

        HashMap<Integer, FotoMarker> oldItems = new HashMap<Integer, FotoMarker>();
        for (Overlay o : mFolderOverlay.getItems()) {
            FotoMarker marker = (FotoMarker) o;
            oldItems.put(marker.getID(), marker);
        }

        FotoMarkerLoaderTask loader = new FotoMarkerLoaderTask(oldItems);
        loader.execute(query);
        */
    }

    /** translates map-zoomlevel to groupfactor
     * that tells sql how geo-points are grouped together.
     */
    private int getGroupingFactor(int zoomlevel) {
        // todo
        return 100;
    }

    // for debugginc
    private static int sInstanceCountFotoLoader = 1;
    private class FotoMarkerLoaderTask extends MarkerLoaderTask<FotoMarker> {

        public FotoMarkerLoaderTask(HashMap<Integer, FotoMarker> oldItems) {
            super(getActivity(), debugPrefix + "-MarkerLoader#" + sInstanceCountFotoLoader++, oldItems);
        }

        @Override
        protected FotoMarker createMarker() {
            FotoMarker marker = (mRecycler.isEmpty()) ? new FotoMarker(mResourceProxy) : mRecycler.pop();
            return marker;
        }

        // This is called when doInBackground() is finished
        protected void onPostExecute(OverlayManager result) {
            if (isCancelled()) {
                onLoadFinished(null);
            } else {
                onLoadFinished(result);

                // unused old items go into recycler
                for (Integer id : mOldItems.keySet()) {
                    FotoMarker marker = mOldItems.get(id);
                    marker.set(0,null,null,null);
                    mRecycler.add(marker);
                }
            }
            mOldItems.clear();
            mOldItems = null;
        }

    }

    /** gets called when FotoMarkerLoaderTask has finished.
     *
     * @param result null if there was an error
     */
    private void onLoadFinished(OverlayManager result) {
        if (Global.debugEnabled) {
            int found = (result != null) ? result.size() : 0;
            Log.i(Global.LOG_CONTEXT, debugPrefix + "onLoadFinished() markers created: " + found);
        }

        if (result != null) {
            OverlayManager old = mFolderOverlay.setOverlayManager(result);
            if (old != null) {
                old.onDetach(this.mMapView);
                old.clear();
            }
            this.mMapView.invalidate();
        }
    }

    /**
     * @return true if click was handeled.
     */
    private boolean onMarkerClicked(int markerId, IGeoPoint makerPosition, Object markerData) {
        return false; // TODO
    }
}
