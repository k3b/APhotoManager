package de.k3b.android.fotoviewer;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import de.k3b.android.fotoviewer.directory.DirectoryGui;
import de.k3b.android.fotoviewer.directory.DirectoryLoaderTask;
import de.k3b.android.fotoviewer.queries.QueryParameterParcelable;
import de.k3b.android.fotoviewer.directory.DirectoryPickerFragment;
import de.k3b.android.fotoviewer.queries.FotoSql;
import de.k3b.android.fotoviewer.queries.FotoViewerParameter;
import de.k3b.android.fotoviewer.queries.Queryable;
import de.k3b.io.Directory;

public class GalleryActivity extends Activity implements
        OnGalleryInteractionListener, DirectoryPickerFragment.OnDirectoryInteractionListener {
    private static final String DBG_PREFIX = "Gal-";

    public static final String EXTRA_QUERY = "gallery";
    private static final String DLG_NAVIGATOR = "navigator";

    private QueryParameterParcelable mGalleryContentQuery = null;
    private boolean mHasEmbeddedDirPicker = false;
    private Queryable mGalleryGui;
    private DirectoryGui mDirGui;

    /** one of the FotoSql.QUERY_TYPE_xxx values */
    private int mDirQueryID = 0;
    private Directory mDirectoryRoot = null;
    private String mCurrentPath = "/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery); // .gallery_activity);

        this.mGalleryContentQuery = getIntent().getParcelableExtra(EXTRA_QUERY);
        if (mGalleryContentQuery == null) mGalleryContentQuery = FotoViewerParameter.currentGalleryContentQuery;

        setTitle(mGalleryContentQuery.getID(), getIntent().getStringExtra(Intent.EXTRA_TITLE));

        mGalleryGui = (Queryable) getFragmentManager().findFragmentById(R.id.galleryCursor);

        if (mGalleryGui != null) {
            mGalleryGui.requery(this, mGalleryContentQuery);
        }

        // on tablet seperate dir navigator fragment
        mDirGui = (DirectoryGui) getFragmentManager().findFragmentById(R.id.directoryFragment);

        if (mDirGui == null) {
            // on small screen/cellphone DirectoryGui is part of gallery
            mDirGui = (DirectoryGui) getFragmentManager().findFragmentById(R.id.galleryCursor);
        }

        // load directoryRoot in background
        final QueryParameterParcelable currentDirContentQuery = FotoViewerParameter.currentDirContentQuery;
        this.mDirQueryID = (currentDirContentQuery != null) ? currentDirContentQuery.getID() : 0;
        DirectoryLoaderTask loader = new DirectoryLoaderTask(this, DBG_PREFIX) {
            protected void onPostExecute(Directory directoryRoot) {
                onDirectoryDataLoadComplete(directoryRoot);
            }
        };
        loader.execute(currentDirContentQuery);
    }

    private void onDirectoryDataLoadComplete(Directory directoryRoot) {
        mDirectoryRoot = directoryRoot;
        if (mDirGui != null) {
            mDirGui.defineDirectoryNavigation(directoryRoot, mDirQueryID, FotoViewerParameter.currentDirContentValue);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_gallery, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.cmd_navigator:
                openNavigator();
                return true;
            case R.id.cmd_filter:
                openFilter();
                return true;
            case R.id.cmd_sort:
                openSort();
                return true;
            case R.id.action_settings:
                openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void openNavigator() {
        final FragmentManager manager = getFragmentManager();
        DirectoryPickerFragment dir = new DirectoryPickerFragment(); // (DirectoryPickerFragment) manager.findFragmentByTag(DLG_NAVIGATOR);
        dir.defineDirectoryNavigation(mDirectoryRoot, mDirQueryID, mCurrentPath);

        dir.show(manager, DLG_NAVIGATOR);

    }

    private void openFilter() {
    }

    private void openSort() {
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);;
        startActivity(intent);
    }

    /** called by Fragment: a fragment Item was clicked */
    @Override
    public void onGalleryImageClick(Bitmap image, Uri imageUri, String description, QueryParameterParcelable parentQuery) {
        Intent intent;
        if ((parentQuery != null) && (parentQuery.getID() == FotoSql.QUERY_TYPE_GROUP_ALBUM) ) {
            //Create intent
            intent = new Intent(this, this.getClass());

            QueryParameterParcelable newQuery = new QueryParameterParcelable(FotoSql.queryDetail);
            newQuery.getWhereFrom(parentQuery, false).getOrderByFrom(parentQuery, false);

            intent.putExtra(EXTRA_QUERY, newQuery);
        } else {

            //Create intent
            intent = new Intent(this, ImageViewActivity.class);

            if (image != null)
                intent.putExtra(ImageViewActivity.EXTRA_IMAGE, image); // does not work for images > 1mb. there we need to use uri-s instead
            if (imageUri != null) intent.setData(imageUri);

        }
        intent.putExtra(Intent.EXTRA_TITLE, description);
        //Start details activity
        startActivity(intent);
    }

    /** GalleryFragment tells the Owning Activity that querying data has finisched */
    @Override
    public void setResultCount(int count) {
        String countText = "(" + count + ")";
        String title = this.getTitle().toString();
        if (!title.contains(countText)) {
            setTitle(title + " - " + countText);
        }
    }

    /**
     * called when user selects a new directory
     *
     * @param selectedAbsolutePath
     * @param queryTypeId
     */
    @Override
    public void onDirectoryPick(String selectedAbsolutePath, int queryTypeId) {
        if (!this.mHasEmbeddedDirPicker) {
            navigateTo(selectedAbsolutePath, queryTypeId);
        }
    }

    /**
     * called when user cancels selection of a new directory
     * @param queryTypeId
     */
    @Override
    public void onDirectoryCancel(int queryTypeId) {
        // do nothing
    }

    /** called after the selection in tree has changed */
    @Override
    public void onDirectorySelectionChanged(String selectedAbsolutePath, int queryTypeId) {
        if (this.mHasEmbeddedDirPicker) {
            navigateTo(selectedAbsolutePath, queryTypeId);
        }
    }

    private void navigateTo(String selectedAbsolutePath, int queryTypeId) {
        if (mCurrentPath.compareTo(selectedAbsolutePath) != 0) {
            mCurrentPath = selectedAbsolutePath;
            Log.d(Global.LOG_CONTEXT, "GalleryActivity.navigateTo " + selectedAbsolutePath + " from " + mCurrentPath);

            Toast.makeText(this, selectedAbsolutePath, Toast.LENGTH_LONG);

            QueryParameterParcelable newQuery = new QueryParameterParcelable(this.mGalleryContentQuery);
            FotoSql.addPathWhere(newQuery, selectedAbsolutePath, queryTypeId);

            this.mGalleryGui.requery(this, newQuery);
            if (mDirGui != null) {
                mDirGui.navigateTo(selectedAbsolutePath);
            }
        }
    }

    private void setTitle(int id, String description) {
        String title = getString(id);

        if (null != description) title += " - " + description;
        this.setTitle(title);
    }

}
