package de.k3b.android.fotoviewer;

import java.util.ArrayList;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class GalleryActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static String TAG="CursorLoader";

    private GridView galleryView;
    private GalleryArrayAdapter galleryAdapter;
    // private GalleryCursorAdapter galleryAdapter;
    LoaderManager loadermanager;
    CursorLoader cursorLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_activity);

        galleryView = (GridView) findViewById(R.id.gridView);
        galleryAdapter = new GalleryArrayAdapter(this, R.layout.gallery_grid_item, getData());
        // galleryAdapter = new GalleryCursorAdapter(this, null);

        // loadermanager=getLoaderManager();
        // loadermanager=getLoaderManager();

        galleryView.setAdapter(galleryAdapter);

        galleryView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                ImageItem item = (ImageItem) parent.getItemAtPosition(position);

                //Create intent
                Intent intent = new Intent(GalleryActivity.this, ImageViewActivity.class);
                intent.putExtra("title", item.getTitle());
                intent.putExtra("image", item.getImage());

                //Start details activity
                startActivity(intent);
            }
        });
        
        /**
         * This initializes the loader and makes it active. If the loader
         * specified by the ID already exists, the last created loader is reused.
         * If the loader specified by the ID does not exist, initLoader() triggers
         * the LoaderManager.LoaderCallbacks method onCreateLoader().
         * This is where you implement the code to instantiate and return a new loader.
         * Use restartLoader() instead of this, to discard the old data and restart the Loader.
         * Hence, here the given LoaderManager.LoaderCallbacks implementation are associated with the loader.
         */
        // loadermanager.initLoader(1, null, this);
    }

    /**
     * This creates and return a new Loader (CursorLoader or custom Loader) for the given ID. 
     * This method returns the Loader that is created, but you don't need to capture a reference to it. 
     */
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {

    //    String[] projection = { DatabaseHandler.UserTable.id, DatabaseHandler.UserTable.name };

/**
 * This requires the URI of the Content Provider
 * projection is the list of columns of the database to return. Null will return all the columns
 * selection is the filter which declares which rows to return. Null will return all the rows for the given URI.
 * selectionArgs:  You may include ?s in the selection, which will be replaced
 * by the values from selectionArgs, in the order that they appear in the selection. 
 * The values will be bound as Strings.
 * sortOrder determines the order of rows. Passing null will use the default sort order, which may be unordered.
 * To back a ListView with a Cursor, the cursor must contain a column named _ID.
 */

        // cursorLoader = new CursorLoader(this, DatabaseAccessUtility.CONTENT_URI, projection, null, null, null);
        return cursorLoader;

    }

    /**
     * Called when a previously created loader has finished its load. This assigns the new Cursor but does not close the previous one. 
     * This allows the system to keep track of the Cursor and manage it for us, optimizing where appropriate. This method is guaranteed
     * to be called prior to the release of the last data that was supplied for this loader. At this point you should remove all use of 
     * the old data (since it will be released soon), but should not
     * do your own release of the data since its loader owns it and will take care of that.
     * The framework would take of closing of old cursor once we return.
     */

    public void onLoadFinished(Loader<Cursor> loader,Cursor cursor) {
        if(galleryAdapter!=null && cursor!=null)
           // galleryAdapter.swapCursor(cursor)
           ; //swap the new cursor in.
        else
            Log.v(TAG,"OnLoadFinished: galleryAdapter is null");
    }

    /**
     * This method is triggered when the loader is being reset and the loader  data is no longer available. 
     * This is called when the last Cursor provided to onLoadFinished() above
     * is about to be closed. We need to make sure we are no longer using it.
     */

    public void onLoaderReset(Loader<Cursor> arg0) {
        if(galleryAdapter!=null)
            // galleryAdapter.swapCursor(null)
            ;
        else
            Log.v(TAG, "OnLoadFinished: galleryAdapter is null");
    }

    /**
     * Prepare some dummy data for gridview
     */
    private ArrayList<ImageItem> getData() {
        final ArrayList<ImageItem> imageItems = new ArrayList<>();
        TypedArray imgs = getResources().obtainTypedArray(R.array.image_ids);
        for (int i = 0; i < imgs.length(); i++) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), imgs.getResourceId(i, -1));
            imageItems.add(new ImageItem(bitmap, "Image#" + i));
        }
        return imageItems;
    }
}
