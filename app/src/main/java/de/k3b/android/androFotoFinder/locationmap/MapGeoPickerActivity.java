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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import de.k3b.android.androFotoFinder.R;
import de.k3b.android.osmdroid.ZoomUtil;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.database.SelectedItems;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.io.GeoUri;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.GeoRectangle;

public class MapGeoPickerActivity extends Activity  {

    private LocationMapFragment mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = this.getIntent();

        GeoPointDto geoPointFromIntent = getGeoPointDtoFromIntent(intent);

        String extraTitle = intent.getStringExtra(Intent.EXTRA_TITLE);
        if (extraTitle == null && (geoPointFromIntent == null)) {
            extraTitle = getString(R.string.app_map_name);
        }
        if (extraTitle == null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            // must be called before this.setContentView(...) else crash
            this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        setContentView(R.layout.activity_map_geo_picker);

        if (extraTitle != null) {
            this.setTitle(extraTitle);
        } else {
            setNoTitle();
        }

        mMap = (LocationMapFragment) getFragmentManager().findFragmentById(R.id.fragment_map);
        mMap.STATE_LAST_VIEWPORT = "ignore"; // do not use last viewport in settings

        GalleryFilterParameter rootFilter = new GalleryFilterParameter();
        GeoRectangle rectangle = new GeoRectangle();
        SelectedItems selectedItems = null;

        int zoom = ZoomUtil.NO_ZOOM;
        if ((savedInstanceState == null) && (geoPointFromIntent != null)) {
            zoom = geoPointFromIntent.getZoomMin();
            rectangle.setLogituedMin(geoPointFromIntent.getLongitude()).setLatitudeMin(geoPointFromIntent.getLatitude());
            rectangle.setLogituedMax(geoPointFromIntent.getLongitude()).setLatitudeMax(geoPointFromIntent.getLatitude());
        } // else (savedInstanceState != null) restore after rotation. fragment takes care of restoring map pos
        mMap.defineNavigation(rootFilter, rectangle, zoom, selectedItems);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map_geo_picker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.cmd_about:
                AboutDialogPreference.createAboutDialog(this).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private GeoPointDto getGeoPointDtoFromIntent(Intent intent) {
        final Uri uri = (intent != null) ? intent.getData() : null;
        String uriAsString = (uri != null) ? uri.toString() : null;
        GeoPointDto pointFromIntent = null;
        if (uriAsString != null) {
            Toast.makeText(this, getString(R.string.app_name) + ": received  " + uriAsString, Toast.LENGTH_LONG).show();
            GeoUri parser = new GeoUri(GeoUri.OPT_PARSE_INFER_MISSING);
            pointFromIntent = (GeoPointDto) parser.fromUri(uriAsString, new GeoPointDto());
        }
        return pointFromIntent;
    }

    private void setNoTitle() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }


}
