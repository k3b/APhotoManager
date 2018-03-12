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
package de.k3b.android.osmdroid.forge;

import android.app.Activity;
import android.app.Application;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme;
import org.mapsforge.map.reader.ReadBuffer;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.osmdroid.mapsforge.MapsForgeTileProvider;
import org.osmdroid.mapsforge.MapsForgeTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.views.MapView;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by k3b on 17.08.2016.
 */
public class MapsForgeSupport {
    public static void createInstance(Application application) {
        AndroidGraphicFactory.createInstance(application);

        // see https://github.com/mapsforge/mapsforge/issues/868
        ReadBuffer.setMaximumBufferSize(6500000);
    }

    public static void load(Activity activity, MapView mMap, File mapsForgeDir) {
        //first let's up our map source, mapsforge needs you to explicitly specify which map files to load
        //this bit does some basic file system scanning
        File[] maps = scan(mapsForgeDir);

        if ((maps != null) && (maps.length > 0)) {
            //this creates the forge provider and tile sources
            //that's it!

            //note this example does not using caching yet, so each tile is rendered on the fly, every time
            //the user browses to an area. This needs to be updated to support sqlite raster image caches

            //protip: when changing themes, you should also change the tile source name to prevent cached tiles

            //null is ok here, uses the default rendering theme if it's not set
            XmlRenderTheme theme = null;
            try {
                theme = new AssetsRenderTheme(activity.getApplicationContext(), "renderthemes/", "rendertheme-v4.xml");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            MapsForgeTileProvider forge = new MapsForgeTileProvider(
                    new SimpleRegisterReceiver(activity),
                    MapsForgeTileSource.createFromFiles(maps, theme, "rendertheme-v4"),
                    null);

            mMap.setTileProvider(forge);
            mMap.setUseDataConnection(false);
            mMap.setMultiTouchControls(true);
            mMap.setBuiltInZoomControls(true);
        }
    }

    /** simple function to scan all *.map files */
    private static File[] scan(File mapDir) {
        File[] files = (mapDir == null) ? null : mapDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().toLowerCase().endsWith(".map"))
                    return true;
                return false;
            }
        });
        return files;
    }
}
