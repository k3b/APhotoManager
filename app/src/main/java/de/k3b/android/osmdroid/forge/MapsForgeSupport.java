package de.k3b.android.osmdroid.forge;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.osmdroid.mapsforge.MapsForgeTileProvider;
import org.osmdroid.mapsforge.MapsForgeTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.views.MapView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by k3b on 17.08.2016.
 */
public class MapsForgeSupport {
    public static void createInstance(Application application) {
        AndroidGraphicFactory.createInstance(application);
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
                    MapsForgeTileSource.createFromFiles(maps, theme, "rendertheme-v4"));

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
